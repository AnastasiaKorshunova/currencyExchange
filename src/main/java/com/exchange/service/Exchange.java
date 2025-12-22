package com.exchange.service;

import com.exchange.model.Currency;
import com.exchange.model.Participant;
import com.exchange.model.Transaction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Thread-safe central currency exchange service.
 * <p>
 * This class represents a shared exchange where participants can trade
 * currencies either directly with the exchange or with each other.
 * It maintains current exchange rates, registered participants,
 * and a history of all executed transactions.
 * </p>
 *
 * <p>
 * Implemented as a thread-safe Singleton to guarantee consistent
 * exchange rates and transaction history across all threads.
 * </p>
 */
public class Exchange {

    private static final Logger logger = LogManager.getLogger(Exchange.class);

    /**
     * Initialization-on-demand holder idiom for lazy-loaded,
     * thread-safe Singleton instance creation.
     */
    private static class ExchangeHolder {
        private static final Exchange INSTANCE = new Exchange();
    }

    /** Map storing exchange rates keyed as "FROM_TO" (e.g. USD_EUR). */
    private final Map<String, BigDecimal> exchangeRates;

    /** Read-write lock protecting access to exchange rates. */
    private final ReadWriteLock ratesLock;

    /** Thread-safe list of registered exchange participants. */
    private final List<Participant> participants;

    /** Thread-safe list storing full transaction history. */
    private final List<Transaction> transactionHistory;

    /**
     * Private constructor to prevent external instantiation.
     * Initializes default exchange rates and internal data structures.
     */
    private Exchange() {
        this.exchangeRates = new ConcurrentHashMap<>();
        this.ratesLock = new ReentrantReadWriteLock();
        this.participants = new CopyOnWriteArrayList<>();
        this.transactionHistory = new CopyOnWriteArrayList<>();

        initializeExchangeRates();
        logger.info("Exchange initialized with default rates");
    }

    /**
     * Returns the singleton instance of the exchange.
     *
     * @return shared {@link Exchange} instance
     */
    public static Exchange getInstance() {
        return ExchangeHolder.INSTANCE;
    }

    /**
     * Initializes default exchange rates used at application startup.
     */
    private void initializeExchangeRates() {
        setExchangeRate(Currency.USD, Currency.MNT, new BigDecimal("3450.50"));
        setExchangeRate(Currency.EUR, Currency.MNT, new BigDecimal("3780.75"));
        setExchangeRate(Currency.USD, Currency.EUR, new BigDecimal("0.91"));
    }

    /**
     * Registers a participant so they can perform exchange transactions.
     *
     * @param participant participant to be registered
     */
    public void registerParticipant(Participant participant) {
        participants.add(participant);
        logger.info("Participant registered: {}", participant.getName());
    }

    /**
     * Returns an immutable snapshot of all registered participants.
     *
     * @return list of participants
     */
    public List<Participant> getParticipants() {
        return List.copyOf(participants);
    }

    /**
     * Retrieves the exchange rate between two currencies.
     * <p>
     * If a direct rate is unavailable, attempts to calculate
     * the inverse rate.
     * </p>
     *
     * @param from source currency
     * @param to target currency
     * @return exchange rate or {@link BigDecimal#ZERO} if unavailable
     */
    public BigDecimal getExchangeRate(Currency from, Currency to) {
        if (from == to) {
            return BigDecimal.ONE;
        }

        String key = getRateKey(from, to);
        ratesLock.readLock().lock();
        try {
            BigDecimal rate = exchangeRates.get(key);
            if (rate != null) {
                return rate;
            }

            String reverseKey = getRateKey(to, from);
            BigDecimal reverseRate = exchangeRates.get(reverseKey);
            if (reverseRate != null) {
                return BigDecimal.ONE.divide(reverseRate, 6, RoundingMode.HALF_UP);
            }

            logger.warn("No exchange rate found for {} to {}", from, to);
            return BigDecimal.ZERO;
        } finally {
            ratesLock.readLock().unlock();
        }
    }

    /**
     * Sets or updates an exchange rate between two currencies.
     *
     * @param from base currency
     * @param to target currency
     * @param rate exchange rate value
     */
    public void setExchangeRate(Currency from, Currency to, BigDecimal rate) {
        String key = getRateKey(from, to);
        ratesLock.writeLock().lock();
        try {
            exchangeRates.put(key, rate);
            logger.debug("Exchange rate set: {} -> {} = {}", from, to, rate);
        } finally {
            ratesLock.writeLock().unlock();
        }
    }

    /**
     * Builds a unique key representing a currency pair.
     *
     * @param from source currency
     * @param to target currency
     * @return formatted currency pair key
     */
    private String getRateKey(Currency from, Currency to) {
        return from.name() + "_" + to.name();
    }

    /**
     * Executes a transaction.
     * <p>
     * Determines whether the transaction is a direct exchange
     * or a participant-to-participant transaction.
     * </p>
     *
     * @param transaction transaction to execute
     * @return {@code true} if successful, {@code false} otherwise
     */
    public boolean executeTransaction(Transaction transaction) {
        Participant buyer = transaction.getBuyer();
        Participant seller = transaction.getSeller();
        Currency fromCurrency = transaction.getFromCurrency();
        Currency toCurrency = transaction.getToCurrency();
        BigDecimal amount = transaction.getAmount();

        if (buyer == null) {
            return executeExchangeTransaction(seller, fromCurrency, toCurrency, amount);
        }

        return executeP2PTransaction(transaction);
    }

    /**
     * Executes a transaction directly with the exchange.
     *
     * @param participant participant performing the exchange
     * @param fromCurrency currency to sell
     * @param toCurrency currency to buy
     * @param amount amount to exchange
     * @return {@code true} if transaction succeeded
     */
    private boolean executeExchangeTransaction(Participant participant,
                                               Currency fromCurrency,
                                               Currency toCurrency,
                                               BigDecimal amount) {

        BigDecimal rate = getExchangeRate(fromCurrency, toCurrency);
        BigDecimal convertedAmount = amount.multiply(rate);

        if (!participant.getBalance().hasAmount(fromCurrency, amount)) {
            logger.warn("Transaction rejected: {} has insufficient {} balance",
                    participant.getName(), fromCurrency);
            recordTransaction(null, participant, fromCurrency, toCurrency,
                    amount, rate, Transaction.TransactionStatus.REJECTED);
            return false;
        }

        participant.getBalance().subtract(fromCurrency, amount);
        participant.getBalance().add(toCurrency, convertedAmount);

        updateExchangeRate(fromCurrency, toCurrency, amount);

        recordTransaction(null, participant, fromCurrency, toCurrency,
                amount, rate, Transaction.TransactionStatus.COMPLETED);

        logger.info("Exchange transaction completed: {} exchanged {} {} for {} {} at rate {}",
                participant.getName(), amount, fromCurrency, convertedAmount, toCurrency, rate);

        return true;
    }

    /**
     * Executes a participant-to-participant transaction.
     *
     * @param transaction transaction details
     * @return {@code true} if transaction succeeded
     */
    private boolean executeP2PTransaction(Transaction transaction) {
        Participant buyer = transaction.getBuyer();
        Participant seller = transaction.getSeller();
        Currency fromCurrency = transaction.getFromCurrency();
        Currency toCurrency = transaction.getToCurrency();
        BigDecimal amount = transaction.getAmount();
        BigDecimal rate = transaction.getRate();

        BigDecimal convertedAmount = amount.multiply(rate);

        if (!buyer.getBalance().hasAmount(toCurrency, convertedAmount)
                || !seller.getBalance().hasAmount(fromCurrency, amount)) {

            logger.warn("Transaction rejected due to insufficient funds");
            recordTransaction(buyer, seller, fromCurrency, toCurrency,
                    amount, rate, Transaction.TransactionStatus.REJECTED);
            return false;
        }

        seller.getBalance().subtract(fromCurrency, amount);
        seller.getBalance().add(toCurrency, convertedAmount);
        buyer.getBalance().add(fromCurrency, amount);
        buyer.getBalance().subtract(toCurrency, convertedAmount);

        updateExchangeRate(fromCurrency, toCurrency, amount);

        recordTransaction(buyer, seller, fromCurrency, toCurrency,
                amount, rate, Transaction.TransactionStatus.COMPLETED);

        return true;
    }

    /**
     * Records a completed or rejected transaction in the history.
     *
     * @param buyer transaction buyer
     * @param seller transaction seller
     * @param fromCurrency sold currency
     * @param toCurrency purchased currency
     * @param amount transaction amount
     * @param rate applied exchange rate
     * @param status transaction result status
     */
    private void recordTransaction(Participant buyer,
                                   Participant seller,
                                   Currency fromCurrency,
                                   Currency toCurrency,
                                   BigDecimal amount,
                                   BigDecimal rate,
                                   Transaction.TransactionStatus status) {

        Transaction recordedTransaction = new Transaction.Builder()
                .buyer(buyer)
                .seller(seller)
                .fromCurrency(fromCurrency)
                .toCurrency(toCurrency)
                .amount(amount)
                .rate(rate)
                .status(status)
                .build();

        transactionHistory.add(recordedTransaction);
    }

    /**
     * Updates exchange rate based on transaction volume
     * to simulate market volatility.
     *
     * @param from source currency
     * @param to target currency
     * @param transactionAmount executed transaction amount
     */
    private void updateExchangeRate(Currency from,
                                    Currency to,
                                    BigDecimal transactionAmount) {

        ratesLock.writeLock().lock();
        try {
            BigDecimal currentRate = getExchangeRate(from, to);
            BigDecimal volatility = new BigDecimal("0.00001");

            BigDecimal change = transactionAmount
                    .multiply(volatility)
                    .divide(new BigDecimal("10000"), 6, RoundingMode.HALF_UP);

            BigDecimal newRate = currentRate.add(change);
            if (newRate.compareTo(BigDecimal.ZERO) > 0) {
                setExchangeRate(from, to, newRate);
            }
        } finally {
            ratesLock.writeLock().unlock();
        }
    }

    /**
     * Returns an immutable snapshot of all recorded transactions.
     *
     * @return list of transaction history entries
     */
    public List<Transaction> getTransactionHistory() {
        return List.copyOf(transactionHistory);
    }

    /**
     * Returns a snapshot of all current exchange rates.
     *
     * @return immutable map of exchange rates
     */
    public Map<String, BigDecimal> getAllRates() {
        ratesLock.readLock().lock();
        try {
            return Map.copyOf(exchangeRates);
        } finally {
            ratesLock.readLock().unlock();
        }
    }
}
