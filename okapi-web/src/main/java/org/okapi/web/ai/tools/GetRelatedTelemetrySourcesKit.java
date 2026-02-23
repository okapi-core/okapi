/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.web.ai.tools;

import java.util.List;

public interface GetRelatedTelemetrySourcesKit extends AiToolkit {
  // todo: this should mine a set of related metrics by applying some heuristics

  /**
   * For services: straightforward heuristic A: list all metrics, logs sources using a service
   * consumer filter heuristic B: list all related dbs with db.type/system or db.type/name metrics
   * -> SQL dependency heuristic C: messaging.producer = "X", messaging.consumer = "Y" -> spans
   * heuristic D: config.path = "path", consumer = "Y" OR config.path =
   *
   * @param serviceName
   * @return
   */

  /**
   * target categories: application_bug, traffic_spike
   *
   * <p>Shorten the tool list from here to serve these two systems get_logs_count_by_level(string
   * service_name, int level) get_request_count(string service, long start, long end) -> returns
   * rate level discontinuity. get_failures_by_host(string service, host = xyz)
   * get_failures_by_path_template(service, host, path_template) get_response_distribution(service,
   * host, code, path_template, method) -> inc here = failure get_host_level_metrics(string service,
   * host = xyz) -> cpu_usage, cpu_percent -> find specific metric path.
   * get_postgres_metrics(cluster, db) get_mysql_metrics_list(cluster, db)
   * get_metrics_summary(metrics) -> Gauge : changepoint get_error_logs(service, start, end)
   * get_error_logs_stats(service, start, end) get_error_spans_stats(service, start, end)
   *
   * <p>Step 1: make some assumptions and solve the problem (working prototype)
   *
   * <p>Agent long term memory: some long form pattern must go into memory Alarm: always threshold
   * based on failure rate APM: list failed requests by 5xx, list failed requests by 4xx
   *
   * <p>heuristic A: error_count = Total count of error logs => 90% failures explained by
   * application bug heuristic B: dependency_failure = Only specializes in request failures in a
   * microservices architecture Failure categories -> defines : list of tools required ->
   * categories: network_partition get_expected_failures: (service_name, long start, long end)
   * traffic spike: service = "api", request_count -> check for a spike. dependency failure: list
   * all dependencies -> get failure rate of service host issues: service = "api", host = "*"
   * metrics, get any changepoints.
   *
   * <p>Step 2: spec complies with a specific schema (integration) Step 3: spec complies with a
   * declared schema (customer-specific-integration) Step 4: release common schema that translates
   * to specific metrics-stores.
   */
  List<ResourcePath> getRelatedMetrics(String serviceName);

  List<ResourcePath> getRelatedLogsStreams(String serviceName);
}
