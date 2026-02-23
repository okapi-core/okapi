"""okapi-cp command line interface."""

import click

from okapi_cp.deploy.k8s import K8sDeploy, K8sDeployArgs
from okapi_cp.deploy.local import LocalDeploy, LocalDeployArgs


@click.group()
def cli() -> None:
  """okapi-cp CLI."""


@cli.group()
def deploy() -> None:
  """Deploy okapi."""


@deploy.command("local")
@click.option("--hmac-key", default=None, help="HMAC key for test secret")
@click.option("--api-key", default=None, help="API key for test secret")
def deploy_local(hmac_key: str | None, api_key: str | None) -> None:
  """Deploy okapi locally using docker compose."""
  args = LocalDeployArgs(raw_args={}, hmac_key=hmac_key, api_key=api_key)
  flow = LocalDeploy(args=args)
  flow.run()


@deploy.command("k8s")
@click.option("--namespace", default="okapi", help="Kubernetes namespace")
@click.option("--release-ingester", default="okapi-ingester", help="Helm release name for ingester")
@click.option("--release-web", default="okapi-web", help="Helm release name for web")
@click.option("--replica-ingester", type=int, default=None, help="Ingester replica count")
@click.option("--replica-web", type=int, default=None, help="Web replica count")
@click.option("--chart-repo", default=None, help="Helm chart repository URL")
@click.option("--chart-version", default=None, help="Helm chart version")
@click.option("--local-chart-ingester", default=None, help="Local chart path for ingester")
@click.option("--local-chart-web", default=None, help="Local chart path for web")
@click.option("--values-file", default=None, help="Values file to apply")
@click.option("--set", "set_values", multiple=True, help="Helm --set overrides")
@click.option("--ingester-service-type", default=None, help="Service type for ingester")
@click.option("--web-service-type", default=None, help="Service type for web")
@click.option("--clickhouse-host", default=None, help="ClickHouse host")
@click.option("--clickhouse-port", type=int, default=None, help="ClickHouse port")
@click.option("--clickhouse-username", default=None, help="ClickHouse username")
@click.option("--clickhouse-password", default=None, help="ClickHouse password")
@click.option(
    "--clickhouse-secure/--clickhouse-insecure",
    default=None,
    help="Enable or disable ClickHouse TLS",
)
@click.option("--aws-mode", type=click.Choice(["localstack", "aws"]), default=None)
@click.option("--aws-endpoint", default=None, help="AWS endpoint (e.g. LocalStack)")
@click.option("--aws-region", default=None, help="AWS region")
@click.option("--aws-profile", default=None, help="AWS profile")
def deploy_k8s(
    namespace: str | None,
    release_ingester: str | None,
    release_web: str | None,
    replica_ingester: int | None,
    replica_web: int | None,
    chart_repo: str | None,
    chart_version: str | None,
    local_chart_ingester: str | None,
    local_chart_web: str | None,
    values_file: str | None,
    set_values: tuple[str, ...],
    ingester_service_type: str | None,
    web_service_type: str | None,
    clickhouse_host: str | None,
    clickhouse_port: int | None,
    clickhouse_username: str | None,
    clickhouse_password: str | None,
    clickhouse_secure: bool | None,
    aws_mode: str | None,
    aws_endpoint: str | None,
    aws_region: str | None,
    aws_profile: str | None,
) -> None:
  """Deploy okapi to Kubernetes using Helm."""
  args = K8sDeployArgs(
      raw_args={},
      namespace=namespace,
      release_ingester=release_ingester,
      release_web=release_web,
      replica_ingester=replica_ingester,
      replica_web=replica_web,
      chart_repo=chart_repo,
      chart_version=chart_version,
      local_chart_ingester=local_chart_ingester,
      local_chart_web=local_chart_web,
      values_file=values_file,
      set_values=set_values,
      ingester_service_type=ingester_service_type,
      web_service_type=web_service_type,
      clickhouse_host=clickhouse_host,
      clickhouse_port=clickhouse_port,
      clickhouse_username=clickhouse_username,
      clickhouse_password=clickhouse_password,
      clickhouse_secure=clickhouse_secure,
      aws_mode=aws_mode,
      aws_endpoint=aws_endpoint,
      aws_region=aws_region,
      aws_profile=aws_profile,
  )
  flow = K8sDeploy(args=args)
  flow.run()


def main() -> None:
  cli()


if __name__ == "__main__":
  main()
