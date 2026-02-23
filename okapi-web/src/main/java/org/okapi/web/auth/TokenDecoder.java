package org.okapi.web.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.okapi.exceptions.UnAuthorizedException;
import org.springframework.stereotype.Service;

@Service
public class TokenDecoder {
  public DecodedJWT decode(String token, Algorithm algorithm) throws UnAuthorizedException {
    try {
      var verifier = JWT.require(algorithm).build();
      var decoded = verifier.verify(token);
      if (decoded == null) {
        throw new UnAuthorizedException("Token is invalid or expired");
      }
      return decoded;
    } catch (JWTDecodeException e) {
      throw new UnAuthorizedException("Invalid token format");
    }
  }

  public String checkClaimOrThrow(String token, String claimName, Algorithm algorithm) throws UnAuthorizedException {
    try {
      var decoded = decode(token, algorithm);
      var claim = decoded.getClaim(claimName);
      if (claim == null || claim.asString() == null) {
        throw new UnAuthorizedException();
      }
      return claim.asString();
    } catch (JWTDecodeException jwt) {
      throw new UnAuthorizedException();
    }
  }
}
