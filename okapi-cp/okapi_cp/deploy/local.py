"""Local deployment via docker compose."""

from dataclasses import dataclass
import os
import subprocess
from typing import Mapping, Any

from okapi_cp.deploy import DeployFlow
from okapi_cp.utils import do_healthcheck


@dataclass
class LocalDeployArgs:
  raw_args: Mapping[str, Any]
  hmac_key: str | None = None
  api_key: str | None = None


@dataclass
class LocalDeploy(DeployFlow):
  args: LocalDeployArgs

  def run(self) -> None:
    for name in ["localstack-main", "okapi-clickhouse", "okapi-ingester", "okapi-web"]:
      kill_container_with_name(name)
    ensure_network()
    start_localstack()
    start_clickhouse()
    run_ops_migrations()
    create_test_secret(self.args)
    start_ingester()
    start_web()
    do_healthcheck("http://localhost:9001/internal/healthcheck")
    print("Okapi is now ready ! You can access it at http://localhost:9001")


def kill_container_with_name(name: str) -> None:
  subprocess.run(
      ["docker", "rm", "-f", name],
      check=False,
      stdout=subprocess.DEVNULL,
      stderr=subprocess.DEVNULL,
  )


def ensure_network() -> None:
  if subprocess.run(["docker", "network", "inspect", NETWORK_NAME], check=False).returncode != 0:
    subprocess.run(["docker", "network", "create", NETWORK_NAME], check=True)


def start_localstack() -> None:
  subprocess.run(
      [
          "docker",
          "run",
          "--name",
          "localstack-main",
          "--network",
          NETWORK_NAME,
          "-p",
          "4566:4566",
          "-d",
          "localstack/localstack:latest",
      ],
      check=True,
  )


def start_clickhouse() -> None:
  ch_dir = os.path.expanduser("~/.okapi-data")
  os.makedirs(os.path.join(ch_dir, "ch_data"), exist_ok=True)
  os.makedirs(os.path.join(ch_dir, "ch_logs"), exist_ok=True)
  subprocess.run(
      [
          "docker",
          "run",
          "-d",
          "--name",
          "okapi-clickhouse",
          "--network",
          NETWORK_NAME,
          "-p",
          "8123:8123",
          "-p",
          "9000:9000",
          "-e",
          f"CLICKHOUSE_PASSWORD={CLICKHOUSE_PASSWORD}",
          "--ulimit",
          "nofile=262144:262144",
          "-v",
          f"{ch_dir}/ch_data:/var/lib/clickhouse/",
          "-v",
          f"{ch_dir}/ch_logs:/var/log/clickhouse-server/",
          "clickhouse/clickhouse-server",
      ],
      check=True,
  )


def run_ops_migrations() -> None:
  image = f"ghcr.io/okapi-core/okapi-ops:{os.getenv('OKAPI_TAG', 'latest')}"
  aws_env_defaults = {
      "AWS_ACCESS_KEY_ID": "test-id",
      "AWS_SECRET_ACCESS_KEY": "test-secret",
      "AWS_REGION": "us-west-2",
  }
  aws_env_args = []
  for key, default in aws_env_defaults.items():
    value = os.environ.get(key) or default
    aws_env_args.extend(["-e", f"{key}={value}"])
  subprocess.run(
      [
          "docker",
          "run",
          "--rm",
          "--network",
          NETWORK_NAME,
          *aws_env_args,
          image,
          "ch-migrate",
          "--host",
          "okapi-clickhouse",
          "--port",
          "8123",
          "--user",
          "default",
          "--password",
          CLICKHOUSE_PASSWORD,
      ],
      check=True,
  )
  subprocess.run(
      [
          "docker",
          "run",
          "--rm",
          "--network",
          NETWORK_NAME,
          *aws_env_args,
          image,
          "ddb-migrate",
          "--env",
          "local",
          "--endpoint",
          "http://localstack-main:4566",
          "--region",
          "us-west-2",
      ],
      check=True,
  )


def create_test_secret(args: LocalDeployArgs) -> None:
  hmac_key = args.hmac_key or DEFAULT_HMAC_KEY
  api_key = args.api_key or DEFAULT_API_KEY
  secret_payload = f'{{"hmacKey":"{hmac_key}","apiKey":"{api_key}"}}'
  subprocess.run(
      [
          "aws",
          "secretsmanager",
          "create-secret",
          "--name",
          "/okapi/secrets",
          "--description",
          "Okapi test secrets",
          "--secret-string",
          secret_payload,
          "--region",
          "us-west-2",
          "--endpoint-url",
          "http://localhost:4566",
      ],
      check=False,
  )


def start_ingester() -> None:
  image = f"ghcr.io/okapi-core/okapi-ingester:{os.getenv('OKAPI_TAG', 'latest')}"
  subprocess.run(
      [
          "docker",
          "run",
          "-p",
          "9009:9009",
          "-d",
          "--network",
          NETWORK_NAME,
          "--name",
          "okapi-ingester",
          image,
          "--okapi.clickhouse.host=okapi-clickhouse",
          "--okapi.chMetricsWal=/wal/metrics",
          "--okapi.chLogsWal=/wal/metrics",
          "--okapi.chTracesWal=/wal/metrics",
      ],
      check=True,
  )


def start_web() -> None:
  image = f"ghcr.io/okapi-core/okapi-web:{os.getenv('OKAPI_TAG', 'latest')}"
  cmd = [
      "docker",
      "run",
      "-p",
      "9001:9001",
      "-d",
      "--network",
      NETWORK_NAME,
      "--name",
      "okapi-web",
  ]
  aws_env_defaults = {
      "AWS_ACCESS_KEY_ID": "test-id",
      "AWS_SECRET_ACCESS_KEY": "test-secret",
      "AWS_REGION": "us-west-2",
  }
  for key, default in aws_env_defaults.items():
    value = os.environ.get(key) or default
    cmd.extend(["-e", f"{key}={value}"])
  cmd.extend(
      [
          image,
          "--clusterEndpoint=http://okapi-ingester:9009",
          "--okapi.aws.endpoint=http://localstack-main:4566",
      ]
  )
  subprocess.run(cmd, check=True)


NETWORK_NAME = "okapi-test-network"
CLICKHOUSE_PASSWORD = "okapi_testing_password"
DEFAULT_HMAC_KEY = "5e1a04d3"
DEFAULT_API_KEY = "a2991d99"
