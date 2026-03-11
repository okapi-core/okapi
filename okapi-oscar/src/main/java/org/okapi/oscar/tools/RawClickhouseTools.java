package org.okapi.oscar.tools;

import com.clickhouse.client.api.Client;

public class RawClickhouseTools {
  // todo: this tool will start a raw clickhouse session and return records as formatted strings
  // todo: this tool will be used to query clickhouse directly, each session will have a unique
  // client with specific rate limits to avoid DDOS-ing Clickhouse.
  Client client;
}
