package com.exchange.state;

import com.exchange.model.Participant;

public interface ParticipantState {
    void handle(Participant participant);
    String getStateName();
    boolean canTrade();
}
