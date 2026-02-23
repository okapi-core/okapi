package org.okapi.web.ai.provider;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Getter
@Builder
public class RoleAndPrompt {
    String role;
    String prompt;

    public static RoleAndPrompt withPrompt(String newPrompt) {
        return new RoleAndPrompt("user", newPrompt);
    }
}
