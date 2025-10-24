package:
	mvn package -DskipTests=true

docker-metrics: package
	docker build -t ghcr.io/okapi-core/okapi:latest -f okapi-metrics/Dockerfile okapi-metrics

DOCKER_CMD := docker run -d --name
DOCKER_RM := docker rm
DOCKER_STOP := docker stop

localstack:
	localstack stop || true
	localstack start -d
	java -cp okapi-data-ddb/target/okapi-data-ddb-0.0.1-SNAPSHOT.jar  org.okapi.data.CreateDynamoDBTables test
	java -cp okapi-data-ddb/target/okapi-data-ddb-0.0.1-SNAPSHOT.jar  org.okapi.data.CreateS3Bucket test

tables:
	java -cp okapi-data-ddb/target/okapi-data-ddb-0.0.1-SNAPSHOT.jar  org.okapi.data.CreateDynamoDBTables $(ENV:ENV=test)
	java -cp okapi-data-ddb/target/okapi-data-ddb-0.0.1-SNAPSHOT.jar  org.okapi.data.CreateS3Bucket  $(ENV:ENV=test)

run-parmetrics:
	$(DOCKER_CMD) parmetrics  --network testnetwork -p 9000:9000 okapi-parquet-metrics --spring.profiles.active=test --zk.connectionString=zookeeper:2181

run-metrics-proxy:
	$(DOCKER_CMD) metrics-proxy  --network testnetwork -p 9001:9001 okapi-metrics-proxy --spring.profiles.active=test --zk.connectionString=zookeeper:2181

stop-test-infra:
	localstack stop || true
	docker stop zookeeper || true
	docker rm zookeeper || true

# Build and package okapi-traces as a Docker image
okapi-traces-app:
	mvn -pl okapi-traces -am package
	docker build -t okapi-traces:local -f okapi-traces/Dockerfile okapi-traces


localstack-k8s:
	kubectl apply -n okapi -f okapi-logs/local-test/localstack.yml

okapi-logs-local:
	mvn -pl okapi-logs -DskipTests=true -am package
	docker build -t okapi-logs:local -f okapi-logs/Dockerfile okapi-logs
	kubectl delete -n okapi deployment okapi-logs --ignore-not-found
	minikube ssh -- docker rmi --force okapi-logs:local
	minikube image load okapi-logs:local
	kubectl apply -n okapi -f okapi-logs/local-test/okapi-logs-multiple.yml