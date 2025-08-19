package com.okapi.rest.auth;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class AuthRequest {
    String firstName;
    String lastName;
    String email;
    String password;
}
