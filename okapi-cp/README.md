# okapi-cp

okapi-cp is a command line tool for deploying Okapi and its dependencies for local testing or Kubernetes deployments.

## Installation

```sh
pip install okapi-cp
```

## Usage

Local deployment for testing:

```sh
export HMAC_KEY=$(openssl rand -hex 4)
export API_KEY=$(openssl rand -hex 4)
okapi-cp deploy local --hmac-key ${HMAC_KEY} --api-key ${API_KEY}
```

Kubernetes deployment:

```sh
okapi-cp deploy k8s --chart-repo oci://ghcr.io/okapi-core
```
