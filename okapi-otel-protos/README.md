Okapi OTEL Protos
==================

Purpose
-------

- Generate Java classes for OpenTelemetry protobuf definitions (messages only, no gRPC stubs).
- Provide these definitions to other modules (e.g., `okapi-metrics`) via the Maven module `org.okapi:okapi-otel-protos`.

Layout
------

- This module uses a git submodule of the upstream OpenTelemetry proto repository:
    - Path: `okapi-otel-protos/opentelemetry-proto`
    - Repo: https://github.com/open-telemetry/opentelemetry-proto
- The Maven build compiles only message protos for:
    - `opentelemetry/proto/common/**`
    - `opentelemetry/proto/resource/**`
    - `opentelemetry/proto/metrics/**`

Setup (first time)
------------------

1) Add and initialize the git submodule pointing to upstream:

    - Using HTTPS
      git submodule add -b main https://github.com/open-telemetry/opentelemetry-proto.git
      okapi-otel-protos/opentelemetry-proto

    - Or using SSH
      git submodule add -b main git@github.com:open-telemetry/opentelemetry-proto.git
      okapi-otel-protos/opentelemetry-proto

    - Initialize and fetch
      git submodule update --init --recursive okapi-otel-protos/opentelemetry-proto

2) (Optional) Pin to a known-good commit for reproducible builds:

   cd okapi-otel-protos/opentelemetry-proto
   git checkout <commit-sha>
   cd -
   git add okapi-otel-protos/opentelemetry-proto
   git commit -m "Pin opentelemetry-proto submodule to <commit-sha>"

Generate Code
-------------

- From the repo root:

    - Generate only for this module
      ./mvnw -q -pl okapi-otel-protos -am clean generate-sources

    - Or full build
      ./mvnw -q clean package

- Generated sources are placed under
  `okapi-otel-protos/target/generated-sources/protobuf/java`

Use in okapi-metrics
--------------------

- Add a dependency in `okapi-metrics/pom.xml`:

  <dependency>
    <groupId>org.okapi</groupId>
    <artifactId>okapi-otel-protos</artifactId>
    <version>${project.version}</version>
  </dependency>

- Import types like:
  `io.opentelemetry.proto.metrics.v1.Metric` and
  `io.opentelemetry.proto.common.v1.AnyValue`.

Notes
-----

- This module does NOT generate gRPC service stubs; only message classes are compiled.
- If additional OTel areas (e.g., traces, logs) are needed later, extend the plugin `includes` in this module’s
  `pom.xml`.

