package com.exchange.model;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Balance {
    private final Map<Currency, BigDecimal> amounts;
    private final ReadWriteLock lock;

    public Balance() {
        this.amounts = new ConcurrentHashMap<>();
        this.lock = new ReentrantReadWriteLock();

        for (Currency currency : Currency.values()) {
            amounts.put(currency, BigDecimal.ZERO);
        }
    }

    public Balance(Map<Currency, BigDecimal> initialAmounts) {
        this();
        amounts.putAll(initialAmounts);
    }

    public BigDecimal getAmount(Currency currency) {
        lock.readLock().lock();
        try {
            return amounts.get(currency);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setAmount(Currency currency, BigDecimal amount) {
        lock.writeLock().lock();
        try {
            amounts.put(currency, amount);
        } finally {
            lock.writeLock().unlock();
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
            BigDecimal current = amounts.get(currency);
            amounts.put(currency, current.add(amount));
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void subtract(Currency currency, BigDecimal amount) {
        lock.writeLock().lock();
        try {
            BigDecimal current = amounts.get(currency);
            amounts.put(currency, current.subtract(amount));
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
