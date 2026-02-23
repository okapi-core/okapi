/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.data.dto;

public class TableAttributes {
  // common
  public static final String CREATED_TIME = "created_time";
  public static final String UPDATED_TIME = "updated_time";
  public static final String ASSIGNED_TIME = "assigned_time";
  public static final String VERSION = "dto_version";

  // org
  public static final String ORG_ID = "org_id";
  public static final String ORG_NAME = "org_name";
  public static final String ORG_CREATOR = "org_creator";

  // user
  public static final String USER_ID = "user_id";
  public static final String USER_STATUS = "user_status";
  public static final String FIRST_NAME = "first_name";
  public static final String LAST_NAME = "last_name";
  public static final String HASHED_PW = "hashed_pw";
  public static final String EMAIL = "email";

  // dashboards related
  public static final String DASHBOARD_ID = "dashboard_id";
  public static final String DASHBOARD_CREATOR = "dashboard_creator";
  public static final String DASHBOARD_LAST_EDITOR = "dashboard_last_editor";
  public static final String DASHBOARD_TAGS = "dashboard_tags";
  public static final String ROW_ORDER = "dash_row_order";
  public static final String DASH_VARS = "dash_variables";
  public static final String DASHBOARD_ACTIVE_VERSION = "dashboard_active_version";
  public static final String DASHBOARD_VERSION_ID = "dashboard_version_id";
  public static final String DASHBOARD_VERSION = "dashboard_version";
  public static final String DASHBOARD_VERSION_STATUS = "dashboard_version_status";
  public static final String DASHBOARD_VERSION_SPEC_HASH = "dashboard_version_spec_hash";

  // asset related
  public static final String ASSET_NOTE = "asset_note";
  public static final String ASSET_TITLE = "asset_title";
  // graph permissions related
  public static final String EDGE_ID = "graph_entity_id";
  public static final String RELATED_ENTITY = "related_entity_id";
  public static final String RELATED_ENTITY_TYPE = "related_entity_type";
  public static final String RELATION_TYPE = "relation_type";

  // graph source related
  public static final String EDGE_ATTRIBUTES = "edge_attributes";

  // source name
  public static final String SOURCE_ID = "source_name";
  public static final String SOURCE_TYPE = "source_type";
  public static final String REGISTRATION_TOKEN = "registration_token";
  public static final String ORG_SOURCE_STATUS_KEY = "org_source_status_key";

  // row
  public static final String ROW_ID = "dash_row_id";
  public static final String PANEL_ID = "dash_panel_id";
  public static final String ORG_DASH_KEY = "org_dash_key";
  public static final String ORG_ROW_HASH_KEY = "org_row_hash_key";
  public static final String ORG_PANEL_HASH_KEY = "org_panel_hash_key";
  public static final String PANEL_ORDER = "row_panel_order";

  // panel
  public static final String PANEL_QUERY_CONFIG = "panel_query_config";

  // infra entity graph related
  public static final String INFRA_ENTITY_ID = "infra_entity_id";
  public static final String INFRA_ENTITY_ATTRIBUTES = "infra_entity_attributes";
  public static final String INFRA_NODE_OUTGOING_EDGES = "infra_node_outgoing_edges";

  // pending jobs
  public static final String JOB_ID = "job_id";
  public static final String RESULT_LOCATION = "result_location";
  public static final String ERROR_LOCATION = "error_location";
  public static final String JOB_STATUS = "job_status";
  public static final String ATTEMPT_COUNT = "attempt_count";
  public static final String DATA_SOURCE_QUERY = "data_source_query";

  // token metadata
  public static final String TOKEN_ID = "token_id";
  public static final String TOKEN_STATUS = "token_status";
  public static final String CREATOR_ID = "creator_id";

  // dashboard variables
  public static final String ORG_BOARD_KEY = "org_board_hash_key";
  public static final String DASH_VAR_NAME = "dash_variable_name";
  public static final String DASH_VAR_TAG = "dash_variable_tag";
  public static final String DASH_VAR_TYPE = "dash_variable_type";
}
