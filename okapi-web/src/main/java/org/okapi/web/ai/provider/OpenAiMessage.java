package org.okapi.web.ai.provider;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Getter
@Builder
public class OpenAiMessage {
    String model;
    String input;
}
