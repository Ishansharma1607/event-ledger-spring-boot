package com.mphasis.eventledger.gateway.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mphasis.eventledger.gateway.api.EventRequest;
import com.mphasis.eventledger.gateway.client.AccountClient;
import com.mphasis.eventledger.gateway.client.AccountTransactionRequest;
import com.mphasis.eventledger.gateway.domain.EventEntity;
import com.mphasis.eventledger.gateway.repository.EventRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final AccountClient accountClient;
    private final ObjectMapper objectMapper;

    public EventService(EventRepository eventRepository, AccountClient accountClient, ObjectMapper objectMapper) {
        this.eventRepository = eventRepository;
        this.accountClient = accountClient;
        this.objectMapper = objectMapper;
    }

    public SubmitEventResult submit(EventRequest request, String traceId) {
        return eventRepository.findById(request.eventId())
                .map(event -> new SubmitEventResult(false, event))
                .orElseGet(() -> createEvent(request, traceId));
    }

    public EventEntity getEvent(String eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));
    }

    public List<EventEntity> listAccountEvents(String accountId) {
        return eventRepository.findByAccountIdOrderByEventTimestampAscEventIdAsc(accountId);
    }

    private SubmitEventResult createEvent(EventRequest request, String traceId) {
        accountClient.applyTransaction(new AccountTransactionRequest(
                request.eventId(),
                request.accountId(),
                request.type(),
                request.amount(),
                request.currency(),
                request.eventTimestamp()
        ), traceId);

        var event = eventRepository.save(EventEntity.accepted(request, metadataJson(request), traceId));
        return new SubmitEventResult(true, event);
    }

    private String metadataJson(EventRequest request) {
        try {
            return objectMapper.writeValueAsString(request.metadata());
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("metadata must be serializable as JSON", ex);
        }
    }
}
