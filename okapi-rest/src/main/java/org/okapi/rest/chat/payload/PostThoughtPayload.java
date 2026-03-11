package org.okapi.rest.chat.payload;

import org.okapi.rest.annotations.TsResponseType;

@TsResponseType
public record PostThoughtPayload(String thought) {}
