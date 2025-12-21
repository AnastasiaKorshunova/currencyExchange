package com.exchange.state;

import com.exchange.model.Participant;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TradingState implements ParticipantState {
    private static final Logger logger = LogManager.getLogger(TradingState.class);

    @Override
    public void handle(Participant participant) {
        if (participant.getActiveTransactions() == 0) {
            participant.setState(new ActiveState());
            logger.debug("Participant {} transitioned to ACTIVE state", participant.getName());
        }
    }

    @Override
    public String getStateName() {
        return "TRADING";
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
