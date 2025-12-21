package com.exchange.state;

import com.exchange.model.Participant;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class WaitingState implements ParticipantState {
    private static final Logger logger = LogManager.getLogger(WaitingState.class);

    @Override
    public void handle(Participant participant) {
        if (participant.canStartTransaction()) {
            participant.setState(new ActiveState());
            logger.debug("Participant {} transitioned to ACTIVE state from WAITING", participant.getName());
        }
    }

    @Override
    public String getStateName() {
        return "WAITING";
    }

    @Override
    public boolean canTrade() {
        return false;
    }

    @Override
    public String toString() {
        return getStateName();
    }
}
