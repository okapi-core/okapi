package org.okapi.auth;

import java.util.Optional;

public class RoleTemplates {

  public enum ORG_ROLE {
    ADMIN,
    MEMBER
  };

  public enum API_TOKEN_PERMISSION {
    READER,
    WRITER
  }

  public record ParsedOrgRole(String orgId, ORG_ROLE orgRole) {}

  public record ParsedTokenRole(
      String orgId, String teamId, API_TOKEN_PERMISSION apiTokenPermission) {}

  public static String getOrgAdminRole(String orgId) {
    return "org:" + orgId + ":admin";
  }

  public static String getClusterAdminRole(String clusterId) {
    return "cluster:" + clusterId + ":admin";
  }

  public static Optional<ORG_ROLE> parseOrgRoleEnum(String role) {
    if (role.equals("admin")) {
      return Optional.of(ORG_ROLE.ADMIN);
    }
    if (role.equals("member")) {
      return Optional.of(ORG_ROLE.MEMBER);
    } else return Optional.empty();
  }

  public static Optional<API_TOKEN_PERMISSION> parsePermission(String permission) {
    if (permission.equals("reader")) return Optional.of(API_TOKEN_PERMISSION.READER);
    if (permission.equals("writer")) return Optional.of(API_TOKEN_PERMISSION.WRITER);
    else return Optional.empty();
  }

  public static Optional<ParsedOrgRole> parseAsOrgRole(String role) {
    var split = role.split(":");
    if (split.length != 3) {
      return Optional.empty();
    }
    var resource = split[0];
    var orgId = split[1];
    var designation = split[2];
    var parsedRole = parseOrgRoleEnum(designation);
    if (!resource.equals("org") || parsedRole.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(new ParsedOrgRole(orgId, parsedRole.get()));
  }

  public static Optional<ParsedTokenRole> parseAsTokenRole(String role) {
    var parts = role.split(":");
    if (parts.length != 5 || !parts[0].equals("org") || !parts[2].equals("team"))
      return Optional.empty();
    var orgId = parts[1];
    var teamId = parts[3];
    var parsedPermission = parsePermission(parts[4]);
    return parsedPermission.map(
        apiTokenPermission -> new ParsedTokenRole(orgId, teamId, apiTokenPermission));
  }

  public static Optional<String> isTeamAdmin(String role) {
    return checkTeamDesignation(role, "admin");
  }

  public static Optional<String> isTeamReader(String role) {
    return checkTeamDesignation(role, "reader");
  }

  public static Optional<String> isTeamWriter(String role) {
    return checkTeamDesignation(role, "writer");
  }

  private static Optional<String> checkTeamDesignation(String role, String expectedDesignation) {
    var split = role.split(":");
    if (split.length != 5) {
      return Optional.empty();
    }
    var orgPrefix = split[0];
    var teamPrefix = split[2];
    var teamId = split[3];
    var designation = split[4];
    if (!orgPrefix.equals("org")
        || !teamPrefix.equals("team")
        || !designation.equals(expectedDesignation)) {
      return Optional.empty();
    }

    return Optional.of(teamId);
  }

  public static String getOrgMemberRole(String orgId) {
    return "org:" + orgId + ":member";
  }

  public static String getTeamAdminRole(String orgId, String teamId) {
    return "org:" + orgId + ":team:" + teamId + ":admin";
  }

  public static String getTeamReaderRole(String orgId, String teamId) {
    return "org:" + orgId + ":team:" + teamId + ":reader";
  }

  public static String getTeamWriterRole(String orgId, String teamId) {
    return "org:" + orgId + ":team:" + teamId + ":writer";
  }
}
