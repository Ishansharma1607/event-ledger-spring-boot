package com.mphasis.eventledger.gateway.service;

public class EventNotFoundException extends RuntimeException {

    public EventNotFoundException(String eventId) {
        super("Event '%s' was not found".formatted(eventId));
    }
}
