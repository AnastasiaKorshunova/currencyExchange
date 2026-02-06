package com.exchange.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Thread-safe representation of participant balances.
 * Uses ReadWriteLock as required by the assignment.
 */
public class Balance {

    private final Map<Currency, BigDecimal> amounts;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Initializes balance with zero amount for each currency.
     */
    public Balance() {
        this.amounts = new EnumMap<>(Currency.class);
        for (Currency currency : Currency.values()) {
            amounts.put(currency, BigDecimal.ZERO);
        }
    }

    /**
     * Initializes balance with provided amounts.
     * Missing currencies are initialized to zero.
     */
    public Balance(Map<Currency, BigDecimal> initialAmounts) {
        this();
        lock.writeLock().lock();
        try {
            initialAmounts.forEach(amounts::put);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public BigDecimal getAmount(Currency currency) {
        lock.readLock().lock();
        try {
            return amounts.get(currency);
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean hasAmount(Currency currency, BigDecimal amount) {
        lock.readLock().lock();
        try {
            return amounts.get(currency).compareTo(amount) >= 0;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void add(Currency currency, BigDecimal amount) {
        lock.writeLock().lock();
        try {
            amounts.put(currency, amounts.get(currency).add(amount));
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void subtract(Currency currency, BigDecimal amount) {
        lock.writeLock().lock();
        try {
            amounts.put(currency, amounts.get(currency).subtract(amount));
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Map<Currency, BigDecimal> getSnapshot() {
        lock.readLock().lock();
        try {
            return Map.copyOf(amounts);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public String toString() {
        lock.readLock().lock();
        try {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<Currency, BigDecimal> entry : amounts.entrySet()) {
                sb.append(entry.getKey().getSymbol())
                        .append(entry.getKey().name())
                        .append(": ")
                        .append(entry.getValue().setScale(2, RoundingMode.HALF_UP))
                        .append(" ");
            }
            return sb.toString().trim();
        } finally {
            lock.readLock().unlock();
        }
    }
}
