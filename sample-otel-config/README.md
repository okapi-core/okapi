Sample OpenTelemetry Collector Configs
=====================================

Configs to export metrics to an okapi-metrics backend running on `localhost:9000`.

Usage
-----
- Install OpenTelemetry Collector (or `otelcol-contrib`).
- Run with one of the configs here. For example:

  otelcol --config sample-otel-config/hostmetrics-to-okapi.yaml

Notes
-----
- The exporter includes header `X-Okapi-Tenant: demo`. Change as needed.
- Compression is disabled (`compression: none`). Enable only if the server supports request decompression.

