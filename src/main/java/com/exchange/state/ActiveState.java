package com.exchange.state;

import com.exchange.model.Participant;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ActiveState implements ParticipantState {
    private static final Logger logger = LogManager.getLogger(ActiveState.class);

    @Override
    public void handle(Participant participant) {
        if (participant.canStartTransaction()) {
            participant.setState(new TradingState());
            logger.debug("Participant {} transitioned to TRADING state", participant.getName());
        } else {
            participant.setState(new WaitingState());
            logger.debug("Participant {} transitioned to WAITING state", participant.getName());
        }
    }

    @Override
    public String getStateName() {
        return "ACTIVE";
    }

    @Override
    public boolean canTrade() {
        return true;
    }

    @Override
    public String toString() {
        return getStateName();
    }
}
