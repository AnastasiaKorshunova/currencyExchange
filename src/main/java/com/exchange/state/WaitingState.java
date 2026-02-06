package com.exchange.state;

import com.exchange.model.Participant;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Represents a participant that is waiting for a free transaction slot.
 * This state does not perform concurrency checks.
 */
public class WaitingState implements ParticipantState {

    private static final Logger logger = LogManager.getLogger(WaitingState.class);

    @Override
    public void handle(Participant participant) {
        // Transition back to ACTIVE is controlled externally (ParticipantThread)
        logger.debug("Participant {} is in WAITING state", participant.getName());
    }

    @Override
    public boolean canTrade() {
        return false;
    }

    @Override
    public String getStateName() {
        return "WAITING";
    }

    @Override
    public String toString() {
        return getStateName();
    }
}

