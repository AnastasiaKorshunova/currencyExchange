package com.exchange.state;

import com.exchange.model.Participant;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Represents a participant that is currently executing transactions.
 * This state does not manage concurrency or transaction counts.
 */
public class TradingState implements ParticipantState {

    private static final Logger logger = LogManager.getLogger(TradingState.class);

    @Override
    public void handle(Participant participant) {
        logger.debug("Participant {} is in TRADING state", participant.getName());
    }

    @Override
    public boolean canTrade() {
        return false;
    }

    @Override
    public String getStateName() {
        return "TRADING";
    }

    @Override
    public String toString() {
        return getStateName();
    }
}
