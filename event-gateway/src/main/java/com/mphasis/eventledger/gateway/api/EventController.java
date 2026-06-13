package com.mphasis.eventledger.gateway.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mphasis.eventledger.gateway.service.EventService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.mphasis.eventledger.gateway.error.TraceSupport.responseTraceId;

@RestController
public class EventController {

    private final EventService eventService;
    private final ObjectMapper objectMapper;

    public EventController(EventService eventService, ObjectMapper objectMapper) {
        this.eventService = eventService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/events")
    public ResponseEntity<EventResponse> submit(
            @Valid @RequestBody EventRequest request,
            @RequestHeader(value = "X-Trace-ID", required = false) String traceIdHeader
    ) {
        var traceId = responseTraceId(traceIdHeader);
        var result = eventService.submit(request, traceId);
        var status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status)
                .header("X-Trace-ID", traceId)
                .body(EventResponse.from(result.event(), objectMapper));
    }

    @GetMapping("/events/{id}")
    public ResponseEntity<EventResponse> getEvent(
            @PathVariable("id") String eventId,
            @RequestHeader(value = "X-Trace-ID", required = false) String traceIdHeader
    ) {
        var traceId = responseTraceId(traceIdHeader);
        return ResponseEntity.ok()
                .header("X-Trace-ID", traceId)
                .body(EventResponse.from(eventService.getEvent(eventId), objectMapper));
    }

    @GetMapping("/events")
    public ResponseEntity<List<EventResponse>> listAccountEvents(
            @RequestParam("account") String accountId,
            @RequestHeader(value = "X-Trace-ID", required = false) String traceIdHeader
    ) {
        var traceId = responseTraceId(traceIdHeader);
        var events = eventService.listAccountEvents(accountId)
                .stream()
                .map(event -> EventResponse.from(event, objectMapper))
                .toList();
        return ResponseEntity.ok()
                .header("X-Trace-ID", traceId)
                .body(events);
    }
}
