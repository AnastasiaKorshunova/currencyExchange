package com.exchange.state;

import com.exchange.model.Participant;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * State indicating that a participant has completed all of their transactions.
 * <p>
 * In this state, the participant cannot initiate further trades. Any
 * invocation of {@link #handle(Participant)} simply records completion.
 */
public class CompletedState implements ParticipantState {
    private static final Logger logger = LogManager.getLogger(CompletedState.class);

    /**
     * Logs that the participant has finished all transactions.
     *
     * @param participant the participant whose workflow is complete
     */
    @Override
    public void handle(Participant participant) {
        logger.info("Participant {} has completed all transactions", participant.getName());
    }

    /**
     * Provides the identifier for the completed state.
     *
     * @return the string {@code "COMPLETED"}
     */
    @Override
    public String getStateName() {
        return "COMPLETED";
    }

    /**
     * Indicates whether the participant can initiate trades while completed.
     *
     * @return {@code false} because no trades are allowed once completed
     */
    @Override
    public boolean canTrade() {
        return false;
    }

    /**
     * Returns the display name of this state.
     *
     * @return the completed state name
     */
    @Override
    public String toString() {
        return getStateName();
    }
}