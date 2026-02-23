package org.okapi.web.ai.provider;

import com.openai.client.OpenAIClient;
import com.openai.models.responses.*;
import java.util.ArrayList;
import java.util.Optional;
import lombok.Builder;
import lombok.Getter;

@Getter
public class OpenAiProvider implements AiProvider {
  OpenAIClient client;

  @Builder
  public OpenAiProvider(OpenAIClient aiClient) {
    this.client = aiClient;
  }

  @Override
  public ApiResponse getResponse(ApiRequest request) {
    var responseParams =
        ResponseCreateParams.builder()
            .input(request.getRoleAndPrompts().getPrompt())
            .model(request.getModelId())
            .build();
    var response = client.responses().create(responseParams);
    // sort out the reasoning from the responses
    var reasoning = new ArrayList<String>();
    var reasons =
        response.output().stream()
            .map(ResponseOutputItem::reasoning)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .flatMap(reason -> reason.summary().stream())
            .map(ResponseReasoningItem.Summary::text)
            .toList();
    var output =
        response.output().stream()
            .map(ResponseOutputItem::message)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .findFirst()
            .flatMap(msg -> msg.content().stream().findFirst())
            .flatMap(ResponseOutputMessage.Content::outputText)
            .map(ResponseOutputText::text);
    return ApiResponse.builder()
        .createdAt(doubleToLinux(response.createdAt()))
        .status(
            response
                .status()
                .map(st -> this.translateResponseStatus(st.asString()))
                .orElse(RESPONSE_STATUS.INCOMPLETE))
        .model(request.getModelId())
        .response(output.orElse(""))
        .build();
  }

  public long doubleToLinux(double tsSeconds) {
    return (long) (1000 * tsSeconds);
  }

  public RESPONSE_STATUS translateResponseStatus(String status) {
    return switch (status) {
      case "completed" -> RESPONSE_STATUS.COMPLETED;
      default -> RESPONSE_STATUS.INCOMPLETE;
    };
  }
}
