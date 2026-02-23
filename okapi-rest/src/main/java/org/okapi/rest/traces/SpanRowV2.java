/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.rest.traces;

import java.util.Map;
import lombok.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class SpanRowV2 {
  long tsStartNs;
  long tsEndNs;
  String traceId;
  String spanId;
  String parentSpanId;
  String kind;
  String kindString;
  String serviceName;
  String servicePeerName;
  String httpMethod;
  Integer httpStatusCode;
  Integer httpRequestSize;
  Integer httpResponseSize;
  String httpOrigin;
  String httpHost;
  Integer serverAddress;
  Integer serverPort;
  String clientAddress;
  Integer clientPort;
  String sourceAddress;
  Integer sourcePort;
  String networkProtocolType;
  String networkProtocolVersion;
  String dbSystemName;
  String dbCollectionName;
  String dbNamespace;
  String dbOperationName;
  Integer dbResponseStatusCode;
  String dbQueryText;
  String dbQuerySummary;
  String dbStoredProcedureName;
  Integer dbResponseReturnedRows;
  String rpcMethod;
  String rpcMethodOriginal;
  Integer rpcResponseStatusCode;
  Map<String, String> customStringAttributes;
  Map<String, Double> customNumberAttributes;
}
