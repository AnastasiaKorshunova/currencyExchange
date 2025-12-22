package com.exchange.state;

import com.exchange.model.Participant;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Represents a participant that is ready to initiate trading activity.
 * <p>
 * From this state, the participant either transitions to {@link TradingState}
 * when they can start a transaction or moves to {@link WaitingState} while
 * they wait for the next opportunity to trade.
 * </p>
 */
public class ActiveState implements ParticipantState {
    private static final Logger logger = LogManager.getLogger(ActiveState.class);

    /**
     * Determines the next state for the participant based on whether they can
     * begin a transaction.
     *
     * @param participant the participant whose state is being evaluated
     */
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

    /**
     * Provides the display name for this state.
     *
     * @return the string literal "ACTIVE"
     */
    @Override
    public String getStateName() {
        return "ACTIVE";
    }

    /**
     * Indicates that a participant in the active state is allowed to trade.
     *
     * @return always {@code true}
     */
    @Override
    public boolean canTrade() {
        return true;
    }

    @Override
    public String toString() {
        return getStateName();
    }
}