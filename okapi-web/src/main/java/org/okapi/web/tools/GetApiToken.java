package org.okapi.web.tools;

public class GetApiToken {

  public static void main(String[] args) {
    var endpoint = args.length > 0 ? args[0] : GetTempToken.TEST_ENDPOINT;
    var tokenCliSupport = new TokenCliSupport(endpoint);
    try {
      var context =
          tokenCliSupport.fetchTempToken(
              GetTempToken.RESERVED_TEST_USER, GetTempToken.RESERVED_TEST_PASSWORD);
      var apiToken = tokenCliSupport.createApiToken(context.tempToken());
      System.out.println(apiToken.getToken());
    } catch (Exception e) {
      System.err.println("Failed to fetch API token: " + e.getMessage());
      System.exit(1);
    }
  }
}
