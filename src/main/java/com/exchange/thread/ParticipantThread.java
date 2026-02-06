package com.exchange.thread;

import com.exchange.model.Currency;
import com.exchange.model.Participant;
import com.exchange.model.Transaction;
import com.exchange.service.Exchange;
import com.exchange.state.ActiveState;
import com.exchange.state.CompletedState;
import com.exchange.state.TradingState;
import com.exchange.state.WaitingState;
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

/**
 * Represents a trading task executed by a single participant.
 * <p>
 * Each participant executes a fixed number of transactions using an internal
 * thread pool. Concurrency is limited atomically via the participant API.
 * </p>
 *
 * <p>
 * This class is responsible for:
 * <ul>
 *     <li>controlling transaction execution flow</li>
 *     <li>enforcing concurrent transaction limits</li>
 *     <li>managing participant state transitions</li>
 * </ul>
 * </p>
 */
public class ParticipantThread implements Callable<String> {

    private static final Logger logger = LogManager.getLogger(ParticipantThread.class);

    private final Participant participant;
    private final Exchange exchange;
    private final int numberOfTransactions;

    /**
     * Creates a trading task for a participant.
     *
     * @param participant participant executing trades
     * @param numberOfTransactions number of transactions to attempt
     */
    public ParticipantThread(Participant participant, int numberOfTransactions) {
        this.participant = participant;
        this.exchange = Exchange.getInstance();
        this.numberOfTransactions = numberOfTransactions;
    }

    /**
     * Executes all participant transactions and waits for their completion.
     *
     * @return execution summary
     * @throws Exception if interrupted
     */
    @Override
    public String call() throws Exception {
        logger.info("Participant {} started trading", participant.getName());

        ExecutorService transactionPool = Executors.newFixedThreadPool(
                Math.max(1, participant.getMaxConcurrentTransactions()),
                r -> {
                    Thread t = new Thread(r);
                    t.setName(participant.getName() + "-txn-" + t.getId());
                    t.setDaemon(true);
                    return t;
                }
        );

        CompletableFuture<Boolean>[] futures = createTransactions(transactionPool);

        int successful = 0;
        int rejected = 0;

        try {
            for (CompletableFuture<Boolean> future : futures) {
                if (future.join()) {
                    successful++;
                } else {
                    rejected++;
                }
            }
        } finally {
            transactionPool.shutdown();
            transactionPool.awaitTermination(5, TimeUnit.SECONDS);
        }

        participant.setState(new CompletedState());

        String result = String.format(
                "Participant %s completed: %d successful, %d rejected",
                participant.getName(), successful, rejected
        );

        logger.info(result);
        return result;
    }

    private CompletableFuture<Boolean>[] createTransactions(ExecutorService pool) {
        @SuppressWarnings("unchecked")
        CompletableFuture<Boolean>[] futures = new CompletableFuture[numberOfTransactions];

        for (int i = 0; i < numberOfTransactions; i++) {
            futures[i] = CompletableFuture.supplyAsync(this::executeWithConcurrencyControl, pool);
        }
        return futures;
    }

    /**
     * Executes a single transaction while respecting concurrency limits.
     *
     * @return {@code true} if transaction succeeded, {@code false} otherwise
     */
    private boolean executeWithConcurrencyControl() {

        while (!participant.tryStartTransaction()) {
            participant.setState(new WaitingState());
            sleepSilently(100);
        }

        participant.setState(new TradingState());

        try {
            return executeRandomTransaction();
        } finally {
            participant.finishTransaction();
            participant.setState(new ActiveState());
        }
    }

    /**
     * Executes a randomly generated exchange transaction.
     *
     * @return {@code true} if transaction completed successfully
     */
    private boolean executeRandomTransaction() {

        Currency[] currencies = Currency.values();
        Currency from = currencies[ThreadLocalRandom.current().nextInt(currencies.length)];
        Currency to;

        do {
            to = currencies[ThreadLocalRandom.current().nextInt(currencies.length)];
        } while (to == from);

        BigDecimal maxAmount = participant.getBalance().getAmount(from);
        if (maxAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }

        BigDecimal amount = maxAmount
                .multiply(BigDecimal.valueOf(ThreadLocalRandom.current().nextDouble(0.1, 0.4)))
                .setScale(2, RoundingMode.HALF_UP);

        if (amount.compareTo(BigDecimal.valueOf(0.01)) < 0) {
            amount = BigDecimal.valueOf(0.01);
        }

        BigDecimal rate = exchange.getExchangeRate(from, to);

        Transaction transaction = new Transaction.Builder()
                .seller(participant)
                .fromCurrency(from)
                .toCurrency(to)
                .amount(amount)
                .rate(rate)
                .status(Transaction.TransactionStatus.PENDING)
                .build();

        logger.debug(
                "Participant {} exchanging {} {} → {}",
                participant.getName(), amount, from, to
        );

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
