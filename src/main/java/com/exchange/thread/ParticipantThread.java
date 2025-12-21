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
import java.math.RoundingMode;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class ParticipantThread implements Callable<String> {
    private static final Logger logger = LogManager.getLogger(ParticipantThread.class);

    private final Participant participant;
    private final Exchange exchange;
    private final int numberOfTransactions;

    public ParticipantThread(Participant participant, int numberOfTransactions) {
        this.participant = participant;
        this.exchange = Exchange.getInstance();
        this.numberOfTransactions = numberOfTransactions;
    }

    @Override
    public String call() throws Exception {
        logger.info("Participant {} started trading", participant.getName());

        ExecutorService transactionPool = Executors.newFixedThreadPool(
            Math.max(1, participant.getMaxConcurrentTransactions()),
            r -> {
                Thread thread = new Thread(r);
                thread.setName(participant.getName() + "-txn-" + thread.getId());
                thread.setDaemon(true);
                return thread;
            }
        );

        CompletableFuture<Boolean>[] transactionFutures = createTransactions(transactionPool);

        int completedTransactions = 0;
        int rejectedTransactions = 0;
       
        try {
            for (CompletableFuture<Boolean> future : transactionFutures) {
                boolean success = future.join();
                if (success) {
                    completedTransactions++;
                } else {
                    rejectedTransactions++;
                }
  }
        } finally {
            transactionPool.shutdown();
            if (!transactionPool.awaitTermination(5, TimeUnit.SECONDS)) {
                transactionPool.shutdownNow();
            }
        }

        participant.setState(new CompletedState());
        String result = String.format("Participant %s completed: %d successful, %d rejected",
            participant.getName(), completedTransactions, rejectedTransactions);

        logger.info(result);
        return result;
    }

    private CompletableFuture<Boolean>[] createTransactions(ExecutorService transactionPool) {
        @SuppressWarnings("unchecked")
        CompletableFuture<Boolean>[] futures = new CompletableFuture[numberOfTransactions];

        for (int i = 0; i < numberOfTransactions; i++) {
            futures[i] = CompletableFuture.supplyAsync(this::executeWithConcurrencyControl,
                transactionPool);
        }
        return futures;
    }

    private boolean executeWithConcurrencyControl() {
        if (!participant.getState().canTrade()) {
            logger.warn("Participant {} cannot trade in current state: {}",
                participant.getName(), participant.getState().getStateName());
            sleepSilently(500);
            return false;
        }

        while (!participant.canStartTransaction()) {
            logger.debug("Participant {} waiting for available transaction slot",
                participant.getName());
            sleepSilently(300);
        }

        participant.getState().handle(participant);
        participant.setState(new TradingState());
        participant.incrementActiveTransactions();

        try {
            boolean success = executeRandomTransaction();
            sleepSilently(ThreadLocalRandom.current().nextInt(500, 1501));
            return success;
        } finally {
            participant.decrementActiveTransactions();
            participant.getState().handle(participant);
        }
    }


    private boolean executeRandomTransaction() {
        Currency[] currencies = Currency.values();
        Currency fromCurrency = currencies[ThreadLocalRandom.current().nextInt(currencies.length)];
        Currency toCurrency;

        do {
            toCurrency = currencies[ThreadLocalRandom.current().nextInt(currencies.length)];
        } while (toCurrency == fromCurrency);

        BigDecimal maxAmount = participant.getBalance().getAmount(fromCurrency);
        if (maxAmount.compareTo(BigDecimal.ZERO) <= 0) {
            logger.debug("Participant {} has no {} to exchange",
                participant.getName(), fromCurrency);
            return false;
        }

        BigDecimal amount = maxAmount
            .multiply(BigDecimal.valueOf(ThreadLocalRandom.current().nextDouble() * 0.3 + 0.1))
            .setScale(2, RoundingMode.HALF_UP);

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

    private void sleepSilently(long millis) {
        try {
            TimeUnit.MILLISECONDS.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

