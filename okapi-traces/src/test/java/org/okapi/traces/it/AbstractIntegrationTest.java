package org.okapi.traces.it;

import com.datastax.oss.driver.api.core.CqlSession;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;

public abstract class AbstractIntegrationTest {
  protected static final String CONTACT_HOST = "127.0.0.1";
  protected static final int CONTACT_PORT = 9042;
  protected static final String LOCAL_DC = "datacenter1";
  protected static final String KEYSPACE = "okapi_traces_it";

  @BeforeAll
  static void setupSchema() {
    // Create keyspace if needed
    try (CqlSession admin =
        CqlSession.builder()
            .addContactPoint(new InetSocketAddress(CONTACT_HOST, CONTACT_PORT))
            .withLocalDatacenter(LOCAL_DC)
            .build()) {
      admin.execute(
          "CREATE KEYSPACE IF NOT EXISTS "
              + KEYSPACE
              + " WITH replication = {'class':'SimpleStrategy','replication_factor':1}");
    }
    // Create tables in keyspace
    try (CqlSession session =
        CqlSession.builder()
            .addContactPoint(new InetSocketAddress(CONTACT_HOST, CONTACT_PORT))
            .withLocalDatacenter(LOCAL_DC)
            .withKeyspace(KEYSPACE)
            .build()) {
      InputStream in =
          AbstractIntegrationTest.class
              .getClassLoader()
              .getResourceAsStream("schema.cql");
      if (in == null) return;
      String cql =
          new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))
              .lines()
              .collect(Collectors.joining("\n"));
      for (String stmt : cql.split(";")) {
        String s = stmt.trim();
        if (s.isEmpty() || s.startsWith("//")) continue;
        session.execute(s + ";");
      }
    }
  }

  @AfterEach
  void truncateTables() {
    try (CqlSession session =
        CqlSession.builder()
            .addContactPoint(new InetSocketAddress(CONTACT_HOST, CONTACT_PORT))
            .withLocalDatacenter(LOCAL_DC)
            .withKeyspace(KEYSPACE)
            .build()) {
      session.execute("TRUNCATE okapi_spans");
      session.execute("TRUNCATE okapi_spans_by_time");
      session.execute("TRUNCATE okapi_spans_by_duration");
      session.execute("TRUNCATE okapi_traces_by_time");
      session.execute("TRUNCATE okapi_spans_by_id");
      // histogram table may not be used in DAO version; skip if absent
      try {
        session.execute("TRUNCATE okapi_span_count_by_minute");
      } catch (Exception ignored) {
      }
    }
  }
}

