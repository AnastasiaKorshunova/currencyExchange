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

public class Exchange {
    private static final Logger logger = LogManager.getLogger(Exchange.class);
    private static class ExchangeHolder {
        private static final Exchange INSTANCE = new Exchange();
    }

    private final Map<String, BigDecimal> exchangeRates;
    private final ReadWriteLock ratesLock;
    private final List<Participant> participants;
    private final List<Transaction> transactionHistory;

    private Exchange() {
        this.exchangeRates = new ConcurrentHashMap<>();
        this.ratesLock = new ReentrantReadWriteLock();
        this.participants = new CopyOnWriteArrayList<>();
        this.transactionHistory = new CopyOnWriteArrayList<>();

        initializeExchangeRates();
        logger.info("Exchange initialized with default rates");
    }

    public static Exchange getInstance() {
        return ExchangeHolder.INSTANCE;;
    }

    private void initializeExchangeRates() {
        setExchangeRate(Currency.USD, Currency.MNT, new BigDecimal("3450.50"));
        setExchangeRate(Currency.EUR, Currency.MNT, new BigDecimal("3780.75"));
        setExchangeRate(Currency.USD, Currency.EUR, new BigDecimal("0.91"));
    }

    public void registerParticipant(Participant participant) {
        participants.add(participant);
        logger.info("Participant registered: {}", participant.getName());
    }

    public List<Participant> getParticipants() {
        return List.copyOf(participants);
    }

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

    private String getRateKey(Currency from, Currency to) {
        return from.name() + "_" + to.name();
    }

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

    private boolean executeExchangeTransaction(Participant participant, Currency fromCurrency,
                                               Currency toCurrency, BigDecimal amount) {
        BigDecimal rate = getExchangeRate(fromCurrency, toCurrency);
        BigDecimal convertedAmount = amount.multiply(rate);

        if (!participant.getBalance().hasAmount(fromCurrency, amount)) {
            logger.warn("Transaction rejected: {} has insufficient {} balance",
                participant.getName(), fromCurrency);
            recordTransaction(null, participant, fromCurrency, toCurrency, amount, rate,
                Transaction.TransactionStatus.REJECTED);
            return false;
        }

        participant.getBalance().subtract(fromCurrency, amount);
        participant.getBalance().add(toCurrency, convertedAmount);

        updateExchangeRate(fromCurrency, toCurrency, amount);

        recordTransaction(null, participant, fromCurrency, toCurrency, amount, rate,
            Transaction.TransactionStatus.COMPLETED);


        logger.info("Exchange transaction completed: {} exchanged {} {} for {} {} at rate {}",
            participant.getName(), amount, fromCurrency, convertedAmount, toCurrency, rate);

        return true;
    }

    private boolean executeP2PTransaction(Transaction transaction) {
        Participant buyer = transaction.getBuyer();
        Participant seller = transaction.getSeller();
        Currency fromCurrency = transaction.getFromCurrency();
        Currency toCurrency = transaction.getToCurrency();
        BigDecimal amount = transaction.getAmount();
        BigDecimal rate = transaction.getRate();

        BigDecimal convertedAmount = amount.multiply(rate);

        if (!buyer.getBalance().hasAmount(toCurrency, convertedAmount)) {
            logger.warn("Transaction rejected: {} has insufficient {} balance",
                buyer.getName(), toCurrency);
            recordTransaction(buyer, seller, fromCurrency, toCurrency, amount, rate,
                Transaction.TransactionStatus.REJECTED);
            return false;
        }

        if (!seller.getBalance().hasAmount(fromCurrency, amount)) {
            logger.warn("Transaction rejected: {} has insufficient {} balance",
                seller.getName(), fromCurrency);
            recordTransaction(buyer, seller, fromCurrency, toCurrency, amount, rate,
                Transaction.TransactionStatus.REJECTED);
            return false;
        }

        seller.getBalance().subtract(fromCurrency, amount);
        seller.getBalance().add(toCurrency, convertedAmount);
        buyer.getBalance().add(fromCurrency, amount);
        buyer.getBalance().subtract(toCurrency, convertedAmount);

        updateExchangeRate(fromCurrency, toCurrency, amount);

        recordTransaction(buyer, seller, fromCurrency, toCurrency, amount, rate,
            Transaction.TransactionStatus.COMPLETED);

        logger.info("P2P transaction completed: {} <-> {}, {} {} at rate {}",
            buyer.getName(), seller.getName(), amount, fromCurrency, rate);

        return true;
    }

    private void recordTransaction(Participant buyer, Participant seller,
                                   Currency fromCurrency, Currency toCurrency,
                                   BigDecimal amount, BigDecimal rate,
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

    private void updateExchangeRate(Currency from, Currency to, BigDecimal transactionAmount) {
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
                logger.debug("Exchange rate updated: {} -> {} = {} (was {})",
                    from, to, newRate, currentRate);
            }
        } finally {
            ratesLock.writeLock().unlock();
        }
    }

    public List<Transaction> getTransactionHistory() {
        return List.copyOf(transactionHistory);
    }

    public Map<String, BigDecimal> getAllRates() {
        ratesLock.readLock().lock();
        try {
            return Map.copyOf(exchangeRates);
        } finally {
            ratesLock.readLock().unlock();
        }
    }
}
