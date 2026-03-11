package org.okapi.rest.chat.payload;

import org.okapi.rest.annotations.TsResponseType;

@TsResponseType
public record GetTraceFollowUpPayload(String traceId, long from, long to) {}
