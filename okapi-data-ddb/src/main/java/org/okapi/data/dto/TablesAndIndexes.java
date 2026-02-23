/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.data.dto;

public class TablesAndIndexes {
  // ORG
  public static final String ORGS_TABLE = "OrgsTable";

  // User
  public static final String USERS_TABLE = "UsersTable";
  public static final String USERS_BY_EMAIL_GSI = "UsersEmailGsi";

  // dashboards table
  public static final String DASHBOARDS_TABLE = "DashboardsTable";

  // dashboars variables table
  public static final String DASHBOARD_VAR_TABLE = "DashboardVariablesTable";

  // dashboard rows and panels
  public static final String DASHBOARD_ROWS_TABLE = "DashboardRowsTable";
  public static final String DASHBOARD_PANELS_TABLE = "DashboardPanelsTable";
  public static final String DASHBOARD_VERSIONS_TABLE = "DashboardVersionsTable";

  // graph permissions table
  public static final String RELATIONSHIP_GRAPH_TABLE = "RelationsGraphTable";

  //
  public static final String FEDERATED_SOURCES_TABLE = "FederatedSourcesTable";

  //
  public static final String USER_ENTITY_RELATIONS_TABLE = "UserEntityRelationsTable";

  // infra entity graph nodes
  public static final String INFRA_ENTITY_NODES_TABLE = "InfraEntityNodesTable";

  // pending jobs
  public static final String PENDING_JOBS_TABLE = "PendingJobsTable";
  public static final String PENDING_JOBS_BY_WORKER_GSI = "PendingJobsByWorkerGsi";
  public static final String PENDING_JOBS_BY_SOURCE_STATUS_GSI = "PendingJobsBySourceStatusGsi";

  // token metadata
  public static final String TOKEN_META_TABLE = "TokenMetaTable";
  public static final String TOKEN_META_BY_ORG_STATUS_GSI = "TokenMetaByOrgStatusGsi";
}
