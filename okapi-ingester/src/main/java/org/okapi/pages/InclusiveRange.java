/*
 * Copyright The OkapiCore Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.okapi.pages;

/** Inclusive range of timestamps in milliseconds. */
public record InclusiveRange(long startInclusive, long endInclusive) {}
