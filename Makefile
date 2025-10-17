package:
	mvn package -DskipTests=true

docker-metrics: package
	docker build -t ghcr.io/okapi-core/okapi:latest -f okapi-metrics/Dockerfile okapi-metrics

DOCKER_CMD := docker run -d --name
DOCKER_RM := docker rm
DOCKER_STOP := docker stop

run-localstack:
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

test-infra: run-localstack

stop-test-infra:
	localstack stop || true
	docker stop zookeeper || true
	docker rm zookeeper || true
