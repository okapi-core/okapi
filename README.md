# Okapi - an observability stack built for fast queries and compatibility with existing systems

[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](./LICENSE)
[![Community](https://img.shields.io/badge/community-discussions-green)](https://github.com/okapi-core/okapi/discussions)

Okapi is an observability stack.

It stores metrics and traces collected in OpenTelemetry format and allows users to create dashboards, explore metrics,
query spans in a UI.
Okapi also exposes a PromQl querying system meaning metrics ingested in Okapi can be visualized on
Grafana or Perses.

Okapi is built to be easy to self-host and manage. Getting started is easy with the `okapi-cp` CLI.
Please refer to the Quickstart for options on how deploy Okapi.

---

## Why and when to use Okapi ?

Existing observability are usually bursting at the seams, there's either too much data or too many dollars involved.
Okapi is built to augment existing observability deployments with additional capacity.
This is why "play-nice" is an explicit goal so teams can start using Okapi with minimal disruption and onboarding.

## Feature list

- **OpenTelemetry native** : Okapi is compatible with OTLP meaning any collector that emits metrics as OTel or submits
  spans via OTel can leverage Okapi as a storage and analysis layer.
- **PromQl** : Okapi has its own implementation of PromQl. Metrics stored in Okapi can be
  visualized using existing solutions such as Grafana and Perses.
- **Dashboard designer** : Okapi has its own dashboard designer should you choose to just use our own UI.
- **Dashboards as code and templates** : Dashboards can be expressed as intuitive YAML. Refer to templates for examples
  on
  how to monitor Clickhouse, Postgres, Kafka.
- **Spans Browser and Visualizer** : Okapi comes with its own browser for Spans. Users can search spans, visualize span attributes, view attributes of a span.
- **Autocomplete almost everywhere** : Nearly every form field in the Okapi UI has autocomplete minimizing the
  need for copy-paste.
- **Support for arbitrary span fields** : Traces forwarded to Okapi can contain spans with arbitrary attributes.

## Quickstart

### Deploy on local machine (for testing)

Install the CLI:

```sh
pip install okapi-cp
```

Local deployment (testing):

```sh
okapi-cp deploy local --hmac-key 5e1a04d3 --api-key a2991d99
```

Okapi will be available at `http://localhost:9001` once the healthcheck succeeds.

### Deploy on Kubernetes (sample production deployment)

Full deployment with LocalStack and a new ClickHouse install:

```sh
helm repo add clickhouse https://charts.clickhouse.com
helm install clickhouse clickhouse/clickhouse --namespace okapi --create-namespace
okapi-cp deploy k8s \
  --chart-repo https://REPLACE_WITH_OKAPI_HELM_REPO \
  --namespace okapi \
  --aws-mode localstack \
  --aws-endpoint http://localstack.okapi.svc.cluster.local:4566 \
  --clickhouse-host clickhouse.okapi.svc.cluster.local \
  --clickhouse-port 8123 \
  --ingester-service-type ClusterIP \
  --web-service-type LoadBalancer
```

Using an existing ClickHouse deployment:

```sh
okapi-cp deploy k8s \
  --chart-repo https://REPLACE_WITH_OKAPI_HELM_REPO \
  --namespace okapi \
  --aws-mode localstack \
  --aws-endpoint http://localstack.okapi.svc.cluster.local:4566 \
  --clickhouse-host <CLICKHOUSE_HOST> \
  --clickhouse-port 8123
```

Note: replace `https://REPLACE_WITH_OKAPI_HELM_REPO` with the published Helm repo for okapi.

## OTLP/HTTP Ingest (OpenTelemetry)

### Example: Setting up OTel collector with okapi export

Assuming a local deployment, here's how to forward host metrics via OTel collector to Okapi.
Collector config (`otel-collector.yaml`):

```yaml
receivers:
  hostmetrics:
    collection_interval: 10s
    scrapers:
      cpu: { }
      memory: { }
      disk: { }
      filesystem: { }
      load: { }
      network: { }

processors:
  batch: { }

exporters:
  otlphttp/okapi:
    # Base endpoint; exporter appends /v1/metrics for metrics
    endpoint: http://localhost:9009
    compression: none
    headers:
      X-Okapi-Tenant: demo

service:
  pipelines:
    metrics:
      receivers: [ hostmetrics ]
      processors: [ batch ]
      exporters: [ otlphttp/okapi ]
```

Run the collector:

```bash
otelcol --config otel-collector.yaml
```

You can then start making dashboards in the Okapi UI.
`http://localhost:9001`

## License

Licensed under the [Apache License, Version 2.0](./LICENSE).