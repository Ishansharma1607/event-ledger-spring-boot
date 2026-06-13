package com.mphasis.eventledger.gateway.service;

import com.mphasis.eventledger.gateway.domain.EventEntity;

public record SubmitEventResult(
        boolean created,
        EventEntity event
) {
}
