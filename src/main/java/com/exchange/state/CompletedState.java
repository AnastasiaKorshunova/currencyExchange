package com.exchange.state;

import com.exchange.model.Participant;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CompletedState implements ParticipantState {
    private static final Logger logger = LogManager.getLogger(CompletedState.class);

    @Override
    public void handle(Participant participant) {
        logger.info("Participant {} has completed all transactions", participant.getName());
    }

    @Override
    public String getStateName() {
        return "COMPLETED";
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
