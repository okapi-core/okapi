package org.okapi.web.ai.provider;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Builder
@Getter
public class ModelOutput {
  String type;
  String id;
  String status;
  String role;
  List<ChatResponse> responses;
}
