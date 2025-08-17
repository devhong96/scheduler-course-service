package com.scheduler.courseservice.outbox.domain;

import lombok.Getter;

@Getter
public class OutBoxEvent {

    private OutBox outBox;

    public static OutBoxEvent of(OutBox outBox) {
        OutBoxEvent outBoxEvent = new OutBoxEvent();
        outBoxEvent.outBox = outBox;
        return outBoxEvent;
    }
}
