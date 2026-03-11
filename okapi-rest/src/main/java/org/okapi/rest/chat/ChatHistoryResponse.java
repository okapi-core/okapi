package org.okapi.rest.chat;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@ToString
public class ChatHistoryResponse {
  @NotNull List<ChatMessageResponse> responses;
}
