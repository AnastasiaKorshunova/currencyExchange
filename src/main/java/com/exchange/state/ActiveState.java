package com.exchange.state;

import com.exchange.model.Participant;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Represents a participant that is ready to trade.
 * This state does not make concurrency decisions.
 */
public class ActiveState implements ParticipantState {

    private static final Logger logger = LogManager.getLogger(ActiveState.class);

    @Override
    public void handle(Participant participant) {
        logger.debug("Participant {} is ACTIVE", participant.getName());
    }

    @Override
    public boolean canTrade() {
        return true;
    }

    @Override
    public String getStateName() {
        return "ACTIVE";
    }
}
