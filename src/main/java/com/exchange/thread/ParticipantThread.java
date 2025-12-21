package com.exchange.thread;

import com.exchange.model.Currency;
import com.exchange.model.Participant;
import com.exchange.model.Transaction;
import com.exchange.service.Exchange;
import com.exchange.state.CompletedState;
import com.exchange.state.TradingState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class ParticipantThread implements Callable<String> {
    private static final Logger logger = LogManager.getLogger(ParticipantThread.class);

    private final Participant participant;
    private final Exchange exchange;
    private final int numberOfTransactions;
    private final Random random;

    public ParticipantThread(Participant participant, int numberOfTransactions) {
        this.participant = participant;
        this.exchange = Exchange.getInstance();
        this.numberOfTransactions = numberOfTransactions;
        this.random = new Random();
    }

    @Override
    public String call() throws Exception {
        logger.info("Participant {} started trading", participant.getName());

        int completedTransactions = 0;
        int rejectedTransactions = 0;

        for (int i = 0; i < numberOfTransactions; i++) {
            if (!participant.getState().canTrade()) {
                logger.warn("Participant {} cannot trade in current state: {}",
                    participant.getName(), participant.getState().getStateName());
                TimeUnit.MILLISECONDS.sleep(500);
                continue;
            }

            if (participant.canStartTransaction()) {
                participant.getState().handle(participant);
                participant.setState(new TradingState());
                participant.incrementActiveTransactions();

                try {
                    boolean success = executeRandomTransaction();
                    if (success) {
                        completedTransactions++;
                    } else {
                        rejectedTransactions++;
                    }

                    TimeUnit.MILLISECONDS.sleep(random.nextInt(1000) + 500);
                } finally {
                    participant.decrementActiveTransactions();
                    participant.getState().handle(participant);
                }
            } else {
                logger.debug("Participant {} waiting for available transaction slot",
                    participant.getName());
                TimeUnit.MILLISECONDS.sleep(300);
            }
        }

        participant.setState(new CompletedState());
        String result = String.format("Participant %s completed: %d successful, %d rejected",
            participant.getName(), completedTransactions, rejectedTransactions);

        logger.info(result);
        return result;
    }

    private boolean executeRandomTransaction() {
        Currency[] currencies = Currency.values();
        Currency fromCurrency = currencies[random.nextInt(currencies.length)];
        Currency toCurrency;

        do {
            toCurrency = currencies[random.nextInt(currencies.length)];
        } while (toCurrency == fromCurrency);

        BigDecimal maxAmount = participant.getBalance().getAmount(fromCurrency);
        if (maxAmount.compareTo(BigDecimal.ZERO) <= 0) {
            logger.debug("Participant {} has no {} to exchange",
                participant.getName(), fromCurrency);
            return false;
        }

        BigDecimal amount = maxAmount
            .multiply(BigDecimal.valueOf(random.nextDouble() * 0.3 + 0.1))
            .setScale(2, BigDecimal.ROUND_HALF_UP);

        if (amount.compareTo(BigDecimal.valueOf(0.01)) < 0) {
            amount = BigDecimal.valueOf(0.01);
        }

        BigDecimal rate = exchange.getExchangeRate(fromCurrency, toCurrency);

        Transaction transaction = new Transaction.Builder()
            .seller(participant)
            .fromCurrency(fromCurrency)
            .toCurrency(toCurrency)
            .amount(amount)
            .rate(rate)
            .status(Transaction.TransactionStatus.PENDING)
            .build();

        logger.debug("Participant {} attempting to exchange {} {} to {}",
            participant.getName(), amount, fromCurrency, toCurrency);

        return exchange.executeTransaction(transaction);
    }
}
