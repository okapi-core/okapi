package org.okapi.web.ai.provider;

import java.io.StringWriter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.okapi.web.investigation.ctx.PrintableContext;

@AllArgsConstructor
@Builder
@Getter
public class ApiRequest {
  String modelId;
  RoleAndPrompt roleAndPrompts;

  public static ApiRequest of(String modelId, PrintableContext context) {
    var stringWriter = new StringWriter();
    var printWriter = new java.io.PrintWriter(stringWriter);
    context.print(printWriter);
    printWriter.flush();
    return ApiRequest.builder()
        .modelId(modelId)
        .roleAndPrompts(RoleAndPrompt.withPrompt(stringWriter.toString()))
        .build();
  }
}
