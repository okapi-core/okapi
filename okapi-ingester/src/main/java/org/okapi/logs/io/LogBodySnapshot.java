/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.logs.io;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.okapi.primitives.BinaryLogRecordV1;

@AllArgsConstructor
@Getter
public class LogBodySnapshot {
  List<BinaryLogRecordV1> logDocs;
}
