package com.exchange.service;

import com.exchange.model.Currency;
import com.exchange.model.Participant;
import com.exchange.model.Transaction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Central currency exchange service.
 * Uses explicit locks from java.util.concurrent.locks.
 * No synchronized and no concurrent collections are used.
 */
public final class Exchange {

    private static final Logger logger = LogManager.getLogger(Exchange.class);

    /* ===================== Singleton ===================== */

    private static class Holder {
        private static final Exchange INSTANCE = new Exchange();
    }

    public static Exchange getInstance() {
        return Holder.INSTANCE;
    }

    /* ===================== State ===================== */

    private final Map<String, BigDecimal> exchangeRates = new HashMap<>();
    private final ReadWriteLock ratesLock = new ReentrantReadWriteLock();

    private final List<Participant> participants = new ArrayList<>();
    private final ReadWriteLock participantsLock = new ReentrantReadWriteLock();

    private final List<Transaction> transactionHistory = new ArrayList<>();
    private final ReadWriteLock transactionsLock = new ReentrantReadWriteLock();

    /* ===================== Constructor ===================== */

    private Exchange() {
        initializeExchangeRates();
        logger.info("Exchange initialized");
    }

    private void initializeExchangeRates() {
        setExchangeRate(Currency.USD, Currency.MNT, new BigDecimal("3450.50"));
        setExchangeRate(Currency.EUR, Currency.MNT, new BigDecimal("3780.75"));
        setExchangeRate(Currency.USD, Currency.EUR, new BigDecimal("0.91"));
    }

    /* ===================== Participants ===================== */

    public void registerParticipant(Participant participant) {
        participantsLock.writeLock().lock();
        try {
            participants.add(participant);
            logger.info("Participant registered: {}", participant.getName());
        } finally {
            participantsLock.writeLock().unlock();
        }
    }

    public List<Participant> getParticipants() {
        participantsLock.readLock().lock();
        try {
            return List.copyOf(participants);
        } finally {
            participantsLock.readLock().unlock();
        }
    }

    /* ===================== Rates ===================== */

    public BigDecimal getExchangeRate(Currency from, Currency to) {
        if (from == to) {
            return BigDecimal.ONE;
        }

        String key = rateKey(from, to);

        ratesLock.readLock().lock();
        try {
            BigDecimal rate = exchangeRates.get(key);
            if (rate != null) {
                return rate;
            }

            String inverseKey = rateKey(to, from);
            BigDecimal inverse = exchangeRates.get(inverseKey);
            if (inverse != null) {
                return BigDecimal.ONE.divide(inverse, 6, RoundingMode.HALF_UP);
            }

            return BigDecimal.ZERO;
        } finally {
            ratesLock.readLock().unlock();
        }
    }

    public void setExchangeRate(Currency from, Currency to, BigDecimal rate) {
        ratesLock.writeLock().lock();
        try {
            exchangeRates.put(rateKey(from, to), rate);
            logger.debug("Rate set: {} -> {} = {}", from, to, rate);
        } finally {
            ratesLock.writeLock().unlock();
        }
    }

    private String rateKey(Currency from, Currency to) {
        return from.name() + "_" + to.name();
    }

    /* ===================== Transactions ===================== */

    public boolean executeTransaction(Transaction transaction) {
        Participant buyer = transaction.getBuyer();
        Participant seller = transaction.getSeller();

        if (buyer == null) {
            return executeExchangeTransaction(
                    seller,
                    transaction.getFromCurrency(),
                    transaction.getToCurrency(),
                    transaction.getAmount()
            );
        }
        return executeP2PTransaction(transaction);
    }

    private boolean executeExchangeTransaction(Participant participant,
                                               Currency from,
                                               Currency to,
                                               BigDecimal amount) {

        BigDecimal rate = getExchangeRate(from, to);
        BigDecimal converted = amount.multiply(rate);

        if (!participant.getBalance().hasAmount(from, amount)) {
            recordTransaction(null, participant, from, to, amount, rate,
                    Transaction.TransactionStatus.REJECTED);
            return false;
        }

        participant.getBalance().subtract(from, amount);
        participant.getBalance().add(to, converted);

        updateExchangeRate(from, to, amount);

        recordTransaction(null, participant, from, to, amount, rate,
                Transaction.TransactionStatus.COMPLETED);

        return true;
    }

    private boolean executeP2PTransaction(Transaction t) {
        Participant buyer = t.getBuyer();
        Participant seller = t.getSeller();

        Currency from = t.getFromCurrency();
        Currency to = t.getToCurrency();
        BigDecimal amount = t.getAmount();
        BigDecimal rate = t.getRate();

        BigDecimal converted = amount.multiply(rate);

        if (!buyer.getBalance().hasAmount(to, converted) ||
                !seller.getBalance().hasAmount(from, amount)) {

            recordTransaction(buyer, seller, from, to, amount, rate,
                    Transaction.TransactionStatus.REJECTED);
            return false;
        }

        seller.getBalance().subtract(from, amount);
        seller.getBalance().add(to, converted);

        buyer.getBalance().add(from, amount);
        buyer.getBalance().subtract(to, converted);

        updateExchangeRate(from, to, amount);

        recordTransaction(buyer, seller, from, to, amount, rate,
                Transaction.TransactionStatus.COMPLETED);

        return true;
    }

    private void recordTransaction(Participant buyer,
                                   Participant seller,
                                   Currency from,
                                   Currency to,
                                   BigDecimal amount,
                                   BigDecimal rate,
                                   Transaction.TransactionStatus status) {

        Transaction tx = new Transaction.Builder()
                .buyer(buyer)
                .seller(seller)
                .fromCurrency(from)
                .toCurrency(to)
                .amount(amount)
                .rate(rate)
                .status(status)
                .build();

        transactionsLock.writeLock().lock();
        try {
            transactionHistory.add(tx);
        } finally {
            transactionsLock.writeLock().unlock();
        }
    }

    private void updateExchangeRate(Currency from, Currency to, BigDecimal amount) {
        ratesLock.writeLock().lock();
        try {
            BigDecimal current = getExchangeRate(from, to);
            BigDecimal change = amount
                    .multiply(new BigDecimal("0.00001"))
                    .divide(new BigDecimal("10000"), 6, RoundingMode.HALF_UP);

            BigDecimal updated = current.add(change);
            if (updated.compareTo(BigDecimal.ZERO) > 0) {
                exchangeRates.put(rateKey(from, to), updated);
            }
        } finally {
            ratesLock.writeLock().unlock();
        }
    }

    public List<Transaction> getTransactionHistory() {
        transactionsLock.readLock().lock();
        try {
            return List.copyOf(transactionHistory);
        } finally {
            transactionsLock.readLock().unlock();
        }
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
