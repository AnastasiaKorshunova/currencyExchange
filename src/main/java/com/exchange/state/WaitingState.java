package com.exchange.state;

import com.exchange.model.Participant;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Waiting state of a {@link Participant}.
 * <p>
 * This state represents a participant that is temporarily unable
 * to start new transactions because concurrency limits have been reached.
 * </p>
 *
 * <p>
 * While in the {@code WAITING} state, trading is not allowed.
 * The participant remains in this state until at least one
 * transaction slot becomes available.
 * </p>
 */
public class WaitingState implements ParticipantState {

    private static final Logger logger = LogManager.getLogger(WaitingState.class);

    /**
     * Handles state transition logic for the participant.
     * <p>
     * If the participant is allowed to start a new transaction,
     * the state is changed to {@link ActiveState}.
     * </p>
     *
     * @param participant participant whose state is being handled
     */
    @Override
    public void handle(Participant participant) {
        if (participant.canStartTransaction()) {
            participant.setState(new ActiveState());
            logger.debug("Participant {} transitioned to ACTIVE state from WAITING",
                    participant.getName());
        }
    }

    /**
     * Returns the human-readable name of this state.
     *
     * @return state name {@code "WAITING"}
     */
    @Override
    public String getStateName() {
        return "WAITING";
    }

    /**
     * Indicates whether the participant is allowed to trade
     * while in this state.
     *
     * @return {@code false}, trading is not allowed
     */
    @Override
    public boolean canTrade() {
        return false;
    }

    /**
     * Returns string representation of the state.
     *
     * @return state name
     */
    @Override
    public String toString() {
        return getStateName();
    }
}
