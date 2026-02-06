package com.exchange.model;

import com.exchange.state.ActiveState;
import com.exchange.state.ParticipantState;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Represents a participant in the currency exchange simulation.
 * <p>
 * Each participant has:
 * - a unique identifier
 * - a name
 * - a thread-safe balance
 * - a current state (State pattern)
 * - a limit on concurrent transactions enforced atomically
 */
public class Participant {

    private static final AtomicInteger ID_GENERATOR = new AtomicInteger(0);

    private final int id;
    private final String name;
    private final Balance balance;

    private final AtomicReference<ParticipantState> state;
    private final AtomicInteger activeTransactions;
    private final int maxConcurrentTransactions;

    /**
     * Creates a new participant.
     *
     * @param name participant name
     * @param balance initial balance
     * @param maxConcurrentTransactions maximum number of simultaneous transactions
     */
    public Participant(String name, Balance balance, int maxConcurrentTransactions) {
        this.id = ID_GENERATOR.incrementAndGet();
        this.name = name;
        this.balance = balance;
        this.state = new AtomicReference<>(new ActiveState());
        this.activeTransactions = new AtomicInteger(0);
        this.maxConcurrentTransactions = maxConcurrentTransactions;
    }

    /* ===================== Getters ===================== */

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Balance getBalance() {
        return balance;
    }

    public ParticipantState getState() {
        return state.get();
    }

    public int getActiveTransactions() {
        return activeTransactions.get();
    }

    public int getMaxConcurrentTransactions() {
        return maxConcurrentTransactions;
    }

    /* ===================== State handling ===================== */

    public void setState(ParticipantState newState) {
        state.set(newState);
    }

    /* ===================== Transaction slot control ===================== */

    /**
     * Attempts to atomically acquire a transaction slot.
     *
     * @return {@code true} if a slot was acquired, {@code false} otherwise
     */
    public boolean tryStartTransaction() {
        while (true) {
            int current = activeTransactions.get();
            if (current >= maxConcurrentTransactions) {
                return false;
            }
            if (activeTransactions.compareAndSet(current, current + 1)) {
                return true;
            }
        }
    }

    /**
     * Releases a previously acquired transaction slot.
     */
    public void finishTransaction() {
        activeTransactions.decrementAndGet();
    }

    /* ===================== Object ===================== */

    @Override
    public String toString() {
        return String.format(
                "Participant{id=%d, name='%s', state=%s, activeTx=%d/%d}",
                id,
                name,
                state.get().getStateName(),
                activeTransactions.get(),
                maxConcurrentTransactions
        );
    }
}

