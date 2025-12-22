package com.exchange.model;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Thread-safe representation of monetary balances for each supported {@link Currency}.
 * <p>
 * Uses a {@link ReadWriteLock} to guard access to the underlying concurrent map while
 * allowing multiple readers to view balances simultaneously.
 */
public class Balance {
    private final Map<Currency, BigDecimal> amounts;
    private final ReadWriteLock lock;

    /**
     * Creates a balance initialized to zero for every supported currency.
     */
    public Balance() {
        this.amounts = new ConcurrentHashMap<>();
        this.lock = new ReentrantReadWriteLock();

        for (Currency currency : Currency.values()) {
            amounts.put(currency, BigDecimal.ZERO);
        }
    }

    /**
     * Creates a balance using the provided initial amounts and zero for any missing currencies.
     *
     * @param initialAmounts starting amounts for one or more currencies
     */
    public Balance(Map<Currency, BigDecimal> initialAmounts) {
        this();
        amounts.putAll(initialAmounts);
    }

    /**
     * Retrieves the balance for the given currency.
     *
     * @param currency currency to check
     * @return current amount for the currency
     */
    public BigDecimal getAmount(Currency currency) {
        lock.readLock().lock();
        try {
            return amounts.get(currency);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Sets the amount for the specified currency.
     *
     * @param currency currency to update
     * @param amount   new amount to store
     */
    public void setAmount(Currency currency, BigDecimal amount) {
        lock.writeLock().lock();
        try {
            amounts.put(currency, amount);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Determines whether the balance contains at least the requested amount for a currency.
     *
     * @param currency currency to evaluate
     * @param amount   amount to compare against the stored balance
     * @return {@code true} if the balance is greater than or equal to the provided amount
     */
    public boolean hasAmount(Currency currency, BigDecimal amount) {
        lock.readLock().lock();
        try {
            return amounts.get(currency).compareTo(amount) >= 0;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Increases the stored amount for a currency by the provided value.
     *
     * @param currency currency to update
     * @param amount   amount to add
     */
    public void add(Currency currency, BigDecimal amount) {
        lock.writeLock().lock();
        try {
            BigDecimal current = amounts.get(currency);
            amounts.put(currency, current.add(amount));
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Decreases the stored amount for a currency by the provided value.
     *
     * @param currency currency to update
     * @param amount   amount to subtract
     */
    public void subtract(Currency currency, BigDecimal amount) {
        lock.writeLock().lock();
        try {
            BigDecimal current = amounts.get(currency);
            amounts.put(currency, current.subtract(amount));
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Provides an immutable snapshot of the current balances.
     *
     * @return unmodifiable copy of currency amounts
     */
    public Map<Currency, BigDecimal> getSnapshot() {
        lock.readLock().lock();
        try {
            return Map.copyOf(amounts);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Returns a formatted string containing each currency and its balance with two decimal places.
     *
     * @return human-readable balance representation
     */
    @Override
    public String toString() {
        lock.readLock().lock();
        try {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<Currency, BigDecimal> entry : amounts.entrySet()) {
                sb.append(entry.getKey().name())
                        .append(": ")
                        .append(entry.getValue().setScale(2, BigDecimal.ROUND_HALF_UP))
                        .append(" ");
            }
            return sb.toString().trim();
        } finally {
            lock.readLock().unlock();
        }
    }
}