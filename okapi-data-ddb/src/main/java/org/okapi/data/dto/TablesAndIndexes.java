package org.okapi.data.dto;

public class TablesAndIndexes {
    // ORG
    public static final String ORGS_TABLE = "OrgsTable";

    // User
    public static final String USERS_TABLE = "UsersTable";
    public static final String USER_ROLE_RELATIONS = "UserRoleRelationsTable";
    public static final String USERS_BY_EMAIL_GSI = "UsersEmailGsi";

    // role to user inversions
    public static final String ROLE_TO_USER_GSI = "RoleToUserGsi";

    // store the teams
    public static final String TEAMS_TABLE = "TeamsTable";
    public static final String TEAM_MEMBERS_TABLE = "TeamMembersTable";

    //org-to-team relation
    public static final String ORG_TO_TEAM_GSI = "OrgToTeamGsi";

    // authorization table
    public static final String AUTHORIZATION_TOKENS_TABLE = "AuthorizationTokensTable";
    public static final String TEAM_TO_AUTHORIZATION_TOKEN_GSI = "TeamToAuthorizationTokenGsi";

    // dashboards table
    public static final String DASHBOARDS_TABLE = "DashboardsTable";

    // graph permissions table
    public static final String RELATIONSHIP_GRAPH_TABLE = "RelationsGraphTable";
}
