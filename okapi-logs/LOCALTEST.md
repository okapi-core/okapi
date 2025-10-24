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

# Enable MetalLB so LoadBalancer works locally
minikube addons enable metallb

# Configure an address pool for MetalLB (pick an unused range in your local minikube CIDR)
# Hint: `minikube ip` typically shows e.g. 192.168.49.2; use 192.168.49.100-192.168.49.120
cat <<EOF | kubectl apply -f -
apiVersion: v1
kind: Namespace
metadata:
  name: okapi
---
apiVersion: metallb.io/v1beta1
kind: IPAddressPool
metadata:
  name: okapi-lb-pool
  namespace: metallb-system
spec:
  addresses:
    - 192.168.49.100-192.168.49.120
---
apiVersion: metallb.io/v1beta1
kind: L2Advertisement
metadata:
  name: okapi-lb-adv
  namespace: metallb-system
spec:
  ipAddressPools:
    - okapi-lb-pool
EOF
```

## 2) Build the okapi-logs image and load to Minikube

```
# Build artifacts
mvn -q -DskipTests -pl okapi-logs -am package

# Build image using the module Dockerfile
docker build -t okapi-logs:local okapi-logs

# Load into Minikube node(s)
minikube image load okapi-logs:local
```

## 3) Single‑pod S3 smoke test (Localstack sidecar, profile=test)

This deploys okapi-logs with `SPRING_PROFILES_ACTIVE=test` and a Localstack sidecar in the same Pod so the app’s S3
client (which is hardcoded to `http://localhost:4566` in test profile) can reach it.

```
cat <<EOF | kubectl apply -n okapi -f okapi-logs/local-test/local-test-file.yaml
```

Get the service URL and run a quick curl check:

```
kubectl -n okapi wait --for=condition=available deploy/okapi-logs-localstack --timeout=120s

minikube service -n okapi okapi-logs-localstack-svc --url
# export this URL for later
export OKAPI_BASE_URL=$(minikube service -n okapi okapi-logs-localstack-svc --url)

curl -sS ${OKAPI_BASE_URL}/okapi/swim/meta
```

Optionally, run the blackbox integration test suite against this endpoint:

```
mvn -q -pl okapi-integ-test verify -DskipITs=false -Dokapi.integ.baseUrl=${OKAPI_BASE_URL}
```

## 4) Multi‑pod cluster (3 replicas, profile=k8s) + LoadBalancer + Localstack S3

This variant uses the `k8s` profile to enable Kubernetes‑aware SWIM discovery and configures S3 to point at an
in‑cluster Localstack Service using a new endpoint property.

First, deploy Localstack (ClusterIP) in the `okapi` namespace:

```
cat <<EOF | kubectl apply -n okapi -f -
apiVersion: apps/v1
kind: Deployment
metadata:
  name: localstack
  labels:
    app: localstack
spec:
  replicas: 1
  selector:
    matchLabels:
      app: localstack
  template:
    metadata:
      labels:
        app: localstack
    spec:
      containers:
        - name: localstack
          image: localstack/localstack:2.3
          ports:
            - containerPort: 4566
          env:
            - name: SERVICES
              value: s3
            - name: DEBUG
              value: "1"
---
apiVersion: v1
kind: Service
metadata:
  name: localstack
  labels:
    app: localstack
spec:
  selector:
    app: localstack
  ports:
    - name: s3
      port: 4566
      targetPort: 4566
      protocol: TCP
  type: ClusterIP
EOF
```

Now deploy okapi‑logs with 3 replicas (profile `k8s`) and S3 pointing to Localstack via `okapi.logs.s3.endpoint`:

```
cat <<EOF | kubectl apply -n okapi -f -
apiVersion: apps/v1
kind: Deployment
metadata:
  name: okapi-logs
  labels:
    app: okapi-logs
spec:
  replicas: 3
  selector:
    matchLabels:
      app: okapi-logs
  template:
    metadata:
      labels:
        app: okapi-logs
        okapi_service: okapi-logs
    spec:
      containers:
        - name: okapi-logs
          image: okapi-logs:local
          imagePullPolicy: IfNotPresent
          ports:
            - containerPort: 8080
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: k8s
            - name: SERVER_PORT
              value: "8080"
            # Enable S3 against in‑cluster Localstack
            - name: OKAPI_LOGS_S3_BUCKET
              value: okapi-e2e
            - name: OKAPI_LOGS_S3_BASEPREFIX
              value: logs
            - name: OKAPI_LOGS_S3_REGION
              value: us-east-1
            - name: OKAPI_LOGS_S3_ENDPOINT
              value: http://localstack.okapi:4566
            # Localstack accepts any credentials but AWS SDK requires them
            - name: AWS_ACCESS_KEY_ID
              value: test
            - name: AWS_SECRET_ACCESS_KEY
              value: test
            # SWIM k8s discovery: select pods via this label value
            - name: OKAPI_SWIM_K8S_OKAPI-SERVICE-LABEL-VALUE
              value: okapi-logs
          volumeMounts:
            - name: data
              mountPath: /data
      volumes:
        - name: data
          emptyDir: {}
---
apiVersion: v1
kind: Service
metadata:
  name: okapi-logs-svc
  labels:
    app: okapi-logs
spec:
  selector:
    app: okapi-logs
  type: LoadBalancer
  ports:
    - name: http
      port: 8080
      targetPort: 8080
EOF
```

Wait for rollout and obtain the LoadBalancer URL:

```
kubectl -n okapi rollout status deploy/okapi-logs --timeout=120s
export OKAPI_BASE_URL=$(minikube service -n okapi okapi-logs-svc --url)
echo ${OKAPI_BASE_URL}
```

### Verify SWIM membership (k8s profile)

- Pods are labeled `okapi_service=okapi-logs`. The k8s watcher and seed registry will discover peers and add/remove
  members on pod add/delete.
- Verify unique node IDs across replicas by hitting `/okapi/swim/meta` multiple times (the LoadBalancer will spread
  requests):

```
for i in $(seq 1 6); do curl -s ${OKAPI_BASE_URL}/okapi/swim/meta; echo; done
```

You should see varying `nodeId` values over several calls.

Optionally, delete a pod and watch membership react:

```
kubectl -n okapi get pods -l app=okapi-logs
kubectl -n okapi delete pod <one-pod-name>
```

### Run blackbox ingestion tests against the LB

```
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
kubectl delete ns okapi
minikube delete
```

## Notes & Troubleshooting

- LoadBalancer on Minikube requires MetalLB. If `minikube service ... --url` returns multiple URLs, use any; they all
  point to the same `NodePort/LoadBalancer` bindings.
- Localstack sidecar is only needed for the `test` profile single‑pod smoke (section 3). The multi‑pod cluster in
  section 4 intentionally avoids the `test` profile to allow k8s SWIM membership.
- To verify ingestion manually without the test suite, you can post OTLP bytes to `${OKAPI_BASE_URL}/v1/logs` and then
  query `${OKAPI_BASE_URL}/logs/query` with a JSON body. The `okapi-integ-test` module automates this flow.
