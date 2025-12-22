package com.exchange.model;

import com.exchange.state.ParticipantState;
import com.exchange.state.ActiveState;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Represents a participant in the currency exchange simulation. Each participant
 * keeps track of a unique identifier, their name, an associated balance, and the
 * current state describing whether they are active or paused. The class also
 * enforces a limit on the number of transactions a participant can be involved
 * with concurrently.
 */
public class Participant {
    private static final AtomicInteger idGenerator = new AtomicInteger(0);

    private final int id;
    private final String name;
    private final Balance balance;
    private final AtomicReference<ParticipantState> state;
    private final AtomicInteger activeTransactions;
    private final int maxConcurrentTransactions;

    /**
     * Creates a new participant with the provided name and balance while setting
     * the maximum number of concurrent transactions the participant can take
     * part in.
     *
     * @param name the participant's display name
     * @param balance the balance associated with the participant
     * @param maxConcurrentTransactions the maximum number of simultaneous transactions allowed
     */
    public Participant(String name, Balance balance, int maxConcurrentTransactions) {
        this.id = idGenerator.incrementAndGet();
        this.name = name;
        this.balance = balance;
        this.state = new AtomicReference<>(new ActiveState());
        this.activeTransactions = new AtomicInteger(0);
        this.maxConcurrentTransactions = maxConcurrentTransactions;
    }

    /**
     * Returns the unique identifier for the participant.
     *
     * @return participant identifier
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the participant's name.
     *
     * @return participant name
     */
    public String getName() {
        return name;
    }

    /**
     * Provides access to the participant's balance.
     *
     * @return participant balance
     */
    public Balance getBalance() {
        return balance;
    }

    /**
     * Returns the current state of the participant.
     *
     * @return participant state
     */
    public ParticipantState getState() {
        return state.get();
    }

    /**
     * Updates the participant's state.
     *
     * @param newState new state to apply
     */
    public void setState(ParticipantState newState) {
        state.set(newState);
    }

    /**
     * Indicates whether the participant can begin another transaction based on
     * the configured concurrency limit.
     *
     * @return {@code true} if the participant can start a new transaction; otherwise {@code false}
     */
    public boolean canStartTransaction() {
        return activeTransactions.get() < maxConcurrentTransactions;
    }

    /**
     * Increments the number of active transactions for the participant.
     */
    public void incrementActiveTransactions() {
        activeTransactions.incrementAndGet();
    }

    /**
     * Decrements the number of active transactions for the participant.
     */
    public void decrementActiveTransactions() {
        activeTransactions.decrementAndGet();
    }

    /**
     * Returns the count of currently active transactions for the participant.
     *
     * @return current active transaction count
     */
    public int getActiveTransactions() {
        return activeTransactions.get();
    }

    /**
     * Returns the maximum number of concurrent transactions the participant can
     * engage in.
     *
     * @return maximum concurrent transaction count
     */
    public int getMaxConcurrentTransactions() {
        return maxConcurrentTransactions;
    }

    @Override
    public String toString() {
        return String.format("Participant{id=%d, name='%s', state=%s}",
                id, name, state.get().getStateName());
    }
}
