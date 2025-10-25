# Local Kubernetes E2E (Minikube) – okapi-logs

This guide shows how to:

- Build and run okapi-logs on Minikube
- Use Localstack for S3 in a single‑pod (test profile) check
- Run a 3‑replica setup with a LoadBalancer and verify SWIM membership (k8s profile)
- Exercise the HTTP ingestion and query APIs using the blackbox integration tests

Prerequisites

- Docker (or `podman` with Docker shim)
- Java 21 + Maven
- kubectl
- Minikube (v1.32+ recommended)

Conventions

- Namespace: `okapi`
- App label key: `okapi_service=okapi-logs` (used by SWIM k8s discovery)
- Service port: `8080`

## 1) Start Minikube + MetalLB

```
minikube start --driver=docker
```

## 2) Build the okapi-logs image, load the docker image and deploy

```
# Build artifacts
make okapi-logs-local
```

## 3) Get a local IP to test the cluster against

This variant uses the `k8s` profile to enable Kubernetes‑aware SWIM discovery and configures S3 to point at an
in‑cluster Localstack Service using a new endpoint property.

First open a url to connect to NGINX.

```
minikube service nginx -n okapi --url
```

NOTE - this call will block and its required to leave the terminal open.
The output will look something like this:

```bash
$ minikube service nginx -n okapi --url
http://127.0.0.1:62136
```

Wait for rollout and obtain the LoadBalancer URL:

```
kubectl -n okapi rollout status deploy/okapi-logs --timeout=120s
export OKAPI_BASE_URL=$(minikube service -n okapi nginx --url)
echo ${OKAPI_BASE_URL}
```

### Verify SWIM membership (k8s profile)

- Pods are labeled `okapi_service=okapi-logs`. The k8s watcher and seed registry will discover peers and add/remove
  members on pod add/delete.
- Verify unique node IDs across replicas by hitting `/fleet/meta` multiple times (the LoadBalancer will spread
  requests):

```
for i in $(seq 1 6); do curl -s ${OKAPI_BASE_URL}/fleet/meta; echo; done
```

You should see varying `nodeId` values over several calls.

Optionally, delete a pod and watch membership react:

```
kubectl -n okapi get pods -l app=okapi-logs
kubectl -n okapi delete pod <one-pod-name>
```

### Run blackbox ingestion tests against the LB

Collect the minikube URL from step 3) above.
Note - we'll need to prefix base url with `/okapi` since NGINX LB is configured to strip this prefix away.

```
export OKAPI_BASE_URL=http://127.0.0.1:62136/okapi
mvn -q -pl okapi-integ-test verify -DskipITs=false -Dokapi.integ.baseUrl=${OKAPI_BASE_URL}
```

This exercises:

- POST `/v1/logs` (OTLP/HTTP) with headers `X-Okapi-Tenant-Id` and `X-Okapi-Log-Stream`
- POST `/logs/query` with JSON `QueryRequest` (TRACE/REGEX/LEVEL filters)

If the cluster forwards correctly and SWIM resolves the block owners, the expected counts in the test (e.g., 5 for a
fixed trace; 2 matches for regex `failed`; 2 WARN) should pass consistently.

### Optional: Inspect S3 uploads in Localstack

If you have AWS CLI available, you can port‑forward Localstack and list objects to verify each replica writes to its own
node prefix:

```
kubectl -n okapi port-forward svc/localstack 4566:4566 &
export AWS_ACCESS_KEY_ID=test
export AWS_SECRET_ACCESS_KEY=test
export AWS_DEFAULT_REGION=us-east-1

aws --endpoint-url=http://localhost:4566 s3 ls s3://okapi-e2e/logs/ --recursive
```

You should see keys like:

```
logs/<tenant>/<stream>/<hour>/<nodeId>/logfile.idx
logs/<tenant>/<stream>/<hour>/<nodeId>/logfile.bin
```

## 5) Cleanup

```
kubectl delete deployment nginx -n okapi
kubectl delete deployment okapi-logs -n okapi
```

## Notes & Troubleshooting

- LoadBalancer on Minikube requires MetalLB. If `minikube service ... --url` returns multiple URLs, use any; they all
  point to the same `NodePort/LoadBalancer` bindings.
- Localstack sidecar is only needed for the `test` profile single‑pod smoke (section 3). The multi‑pod cluster in
  section 4 intentionally avoids the `test` profile to allow k8s SWIM membership.
- To verify ingestion manually without the test suite, you can post OTLP bytes to `${OKAPI_BASE_URL}/v1/logs` and then
  query `${OKAPI_BASE_URL}/logs/query` with a JSON body. The `okapi-integ-test` module automates this flow.
