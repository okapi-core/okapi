package org.okapi.data.dto;


public class TableAttributes {
    // common
    public static final String CREATED_TIME = "created_time";
    public static final String UPDATED_TIME = "updated_time";
    public static final String EXPIRY_TIME = "expiry_time";
    public static final String VERSION = "dto_version";

    // org
    public static final String ORG_ID = "org_id";
    public static final String ORG_NAME = "org_name";
    public static final String ORG_CREATOR = "org_creator";

    // user
    public static final String USER_ID = "user_id";
    public static final String USER_ROLE = "user_role";
    public static final String USER_STATUS = "user_status";
    public static final String USER_ROLE_STATUS = "user_role_status";
    public static final String FIRST_NAME = "first_name";
    public static final String LAST_NAME = "last_name";
    public static final String HASHED_PW = "hashed_pw";
    public static final String EMAIL = "email";

    // team
    public static final String TEAM_ID = "team_id";
    public static final String TEAM_NAME = "team_name";
    public static final String TEAM_DESCRIPTION = "team_description";

    // authorization related
    public static final String AUTHORIZATION_TOKEN_ID = "authorization_token_id";
    public static final String AUTHORIZATION_TOKEN_STATUS = "authorization_token_status";
    public static final String AUTHORIZATION_TOKEN_ISSUER = "authorization_token_issuer";
    public static final String AUTHORIZATION_TOKEN_ROLES = "authorization_token_roles";
    
    // dashboards related
    public static final String DASHBOARD_ID = "dashboard_id";
    public static final String DASHBOARD_STATUS = "dashboard_status";
    public static final String DASHBOARD_CREATOR = "dashboard_creator";
    public static final String DASHBOARD_LAST_EDITOR = "dashboard_last_editor";
    public static final String DASHBOARD_NOTE = "dashboard_note";
    public static final String DASHBOARD_TITLE = "dashboard_title";
    public static final String DASHBOARD_DEF_BUCKET = "dashboard_definition_bucket";
    public static final String DASHBOARD_DFE_PREFIX = "dashboard_definition_prefix";

    // graph permissions related
    public static final String ENTITY_ID = "graph_entity_id";
    public static final String RELATED_ENTITY = "related_entity_id";
    public static final String RELATION_TYPE = "relation_type";
}
