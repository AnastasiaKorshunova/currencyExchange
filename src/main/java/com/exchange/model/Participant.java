package com.exchange.model;

import com.exchange.state.ParticipantState;
import com.exchange.state.ActiveState;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class Participant {
    private static final AtomicInteger idGenerator = new AtomicInteger(0);

    private final int id;
    private final String name;
    private final Balance balance;
    private final AtomicReference<ParticipantState> state;
    private final AtomicInteger activeTransactions;
    private final int maxConcurrentTransactions;

    public Participant(String name, Balance balance, int maxConcurrentTransactions) {
        this.id = idGenerator.incrementAndGet();
        this.name = name;
        this.balance = balance;
        this.state = new AtomicReference<>(new ActiveState());
        this.activeTransactions = new AtomicInteger(0);
        this.maxConcurrentTransactions = maxConcurrentTransactions;
    }

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

    public void setState(ParticipantState newState) {
        state.set(newState);
    }

    public boolean canStartTransaction() {
        return activeTransactions.get() < maxConcurrentTransactions;
    }

    public void incrementActiveTransactions() {
        activeTransactions.incrementAndGet();
    }

    public void decrementActiveTransactions() {
        activeTransactions.decrementAndGet();
    }

    public int getActiveTransactions() {
        return activeTransactions.get();
    }

    public int getMaxConcurrentTransactions() {
        return maxConcurrentTransactions;
    }

    @Override
    public String toString() {
        return String.format("Participant{id=%d, name='%s', state=%s}",
            id, name, state.get().getStateName());
    }
}
