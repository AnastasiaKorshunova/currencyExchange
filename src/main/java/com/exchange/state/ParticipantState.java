package com.exchange.state;

import com.exchange.model.Participant;

/**
 * Represents a participant's current state in the trading workflow.
 * Implementations define how a participant should be handled and whether
 * they are eligible to perform trades while in this state.
 */
public interface ParticipantState {
    /**
     * Performs state-specific processing for the given participant.
     *
     * @param participant the participant whose state behavior should be executed
     */
    void handle(Participant participant);

    /**
     * Returns the displayable name of this state.
     *
     * @return human-readable state name
     */
    String getStateName();

    /**
     * Indicates whether a participant in this state can execute trades.
     *
     * @return {@code true} if trading is allowed; otherwise {@code false}
     */
    boolean canTrade();
}