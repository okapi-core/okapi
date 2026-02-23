# Copyright The OkapiCore Authors
# SPDX-License-Identifier: Apache-2.0

"""Kubernetes deployment via Helm."""

from dataclasses import dataclass
import subprocess
from typing import Mapping, Any, Iterable

from okapi_cp.deploy import DeployFlow


@dataclass
class K8sDeployArgs:
  raw_args: Mapping[str, Any]
  namespace: str | None = None
  release_ingester: str | None = None
  release_web: str | None = None
  release_localstack: str | None = None
  replica_ingester: int | None = None
  replica_web: int | None = None
  chart_repo: str | None = None
  chart_version: str | None = None
  local_chart_ingester: str | None = None
  local_chart_web: str | None = None
  localstack_chart: str | None = None
  localstack_values_file: str | None = None
  values_file: str | None = None
  set_values: Iterable[str] | None = None
  ingester_service_type: str | None = None
  web_service_type: str | None = None
  clickhouse_host: str | None = None
  clickhouse_port: int | None = None
  clickhouse_username: str | None = None
  clickhouse_password: str | None = None
  clickhouse_secure: bool | None = None
  aws_mode: str | None = None
  aws_endpoint: str | None = None
  aws_region: str | None = None
  aws_profile: str | None = None


@dataclass
class K8sDeploy(DeployFlow):
  args: K8sDeployArgs

  def run(self) -> None:
    steps = [
        LocalStackDeployStep(),
        IngesterDeployStep(),
        WebDeployStep(),
    ]
    for step in steps:
      step.run(self.args)


class LocalStackDeployStep:
  def run(self, args: K8sDeployArgs) -> None:
    if args.aws_mode != "localstack":
      return
    if args.aws_endpoint is None:
      raise ValueError("aws_endpoint is required when aws_mode=localstack")
    release = args.release_localstack or "localstack"
    namespace = args.namespace or "okapi"
    cmd = [
        "helm",
        "upgrade",
        "--install",
        release,
        "localstack/localstack",
        "--namespace",
        namespace,
        "--create-namespace",
    ]
    if args.localstack_values_file:
      cmd.extend(["-f", args.localstack_values_file])
    _run(["helm", "repo", "add", "localstack", "https://localstack.github.io/helm-charts"])
    _run(["helm", "repo", "update"])
    _run(cmd)


class IngesterDeployStep:
  def run(self, args: K8sDeployArgs) -> None:
    namespace = args.namespace or "okapi"
    release = args.release_ingester or "okapi-ingester"
    chart, version = _resolve_chart(
        local_chart=args.local_chart_ingester,
        chart_repo=args.chart_repo,
        chart_name="okapi-ingester",
        chart_version=args.chart_version,
    )
    cmd = [
        "helm",
        "upgrade",
        "--install",
        release,
        chart,
        "--namespace",
        namespace,
        "--create-namespace",
    ]
    if version:
      cmd.extend(["--version", version])
    cmd.extend(_helm_values_args(args))
    cmd.extend(
        _helm_set_pairs(
            {
                "replicaCount": args.replica_ingester,
                "service.type": args.ingester_service_type,
                "springOverrides.okapi.clickhouse.host": args.clickhouse_host,
                "springOverrides.okapi.clickhouse.port": args.clickhouse_port,
                "springOverrides.okapi.clickhouse.username": args.clickhouse_username,
                "springOverrides.okapi.clickhouse.password": args.clickhouse_password,
                "springOverrides.okapi.clickhouse.secure": args.clickhouse_secure,
                "springOverrides.okapi.aws.endpoint": args.aws_endpoint,
                "springOverrides.okapi.aws.region": args.aws_region,
                "springOverrides.okapi.aws.creds": "env",
            }
        )
    )
    _run(cmd)


class WebDeployStep:
  def run(self, args: K8sDeployArgs) -> None:
    namespace = args.namespace or "okapi"
    release = args.release_web or "okapi-web"
    chart, version = _resolve_chart(
        local_chart=args.local_chart_web,
        chart_repo=args.chart_repo,
        chart_name="okapi-web",
        chart_version=args.chart_version,
    )
    ingester_release = args.release_ingester or "okapi-ingester"
    ingester_service = f"{ingester_release}-okapi-ingester"
    cluster_endpoint = (
        f"http://{ingester_service}.{namespace}.svc.cluster.local:9009"
    )
    cmd = [
        "helm",
        "upgrade",
        "--install",
        release,
        chart,
        "--namespace",
        namespace,
        "--create-namespace",
    ]
    if version:
      cmd.extend(["--version", version])
    cmd.extend(_helm_values_args(args))
    cmd.extend(
        _helm_set_pairs(
            {
                "replicaCount": args.replica_web,
                "service.type": args.web_service_type,
                "springOverrides.clusterEndpoint": cluster_endpoint,
                "springOverrides.okapi.aws.endpoint": args.aws_endpoint,
                "springOverrides.okapi.aws.region": args.aws_region,
                "springOverrides.okapi.aws.creds": "env",
            }
        )
    )
    _run(cmd)


def _resolve_chart(
    local_chart: str | None,
    chart_repo: str | None,
    chart_name: str,
    chart_version: str | None,
) -> tuple[str, str | None]:
  if local_chart:
    return local_chart, None
  if chart_repo is None:
    raise ValueError("chart_repo is required when local_chart is not provided")
  if chart_repo.startswith("oci://"):
    chart = f"{chart_repo.rstrip('/')}/{chart_name}"
    return chart, chart_version
  _run(["helm", "repo", "add", "okapi", chart_repo])
  _run(["helm", "repo", "update"])
  chart = f"okapi/{chart_name}"
  return chart, chart_version


def _helm_values_args(args: K8sDeployArgs) -> list[str]:
  out: list[str] = []
  if args.values_file:
    out.extend(["-f", args.values_file])
  if args.set_values:
    for item in args.set_values:
      out.extend(["--set", item])
  return out


def _helm_set_pairs(values: Mapping[str, Any]) -> list[str]:
  out: list[str] = []
  for key, value in values.items():
    if value is None:
      continue
    out.extend(["--set", f"{key}={value}"])
  return out


def _run(cmd: list[str]) -> None:
  subprocess.run(cmd, check=True)
