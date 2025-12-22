package com.exchange.state;

import com.exchange.model.Participant;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Trading state of a {@link Participant}.
 * <p>
 * This state represents a participant that is actively executing
 * one or more exchange transactions.
 * </p>
 *
 * <p>
 * While in the {@code TRADING} state, the participant is allowed
 * to initiate new transactions as long as concurrency limits
 * are not exceeded.
 * </p>
 *
 * <p>
 * When all active transactions are completed (active transaction
 * count reaches zero), the participant automatically transitions
 * back to the {@link ActiveState}.
 * </p>
 */
public class TradingState implements ParticipantState {

    private static final Logger logger = LogManager.getLogger(TradingState.class);

    /**
     * Handles state transition logic for the participant.
     * <p>
     * If the participant has no active transactions remaining,
     * the state is changed to {@link ActiveState}.
     * </p>
     *
     * @param participant participant whose state is being handled
     */
    @Override
    public void handle(Participant participant) {
        if (participant.getActiveTransactions() == 0) {
            participant.setState(new ActiveState());
            logger.debug("Participant {} transitioned to ACTIVE state", participant.getName());
        }
    }

    /**
     * Returns the human-readable name of this state.
     *
     * @return state name {@code "TRADING"}
     */
    @Override
    public String getStateName() {
        return "TRADING";
    }

    /**
     * Indicates whether the participant is allowed to initiate
     * new transactions while in this state.
     *
     * @return {@code true}, trading is allowed
     */
    @Override
    public boolean canTrade() {
        return true;
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
