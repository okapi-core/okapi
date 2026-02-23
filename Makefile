ch_dir = ${HOME}/.okapi-data

FE_SETUP ?= fe-setup.json
REPO = ghcr.io/okapi-core
OKAPI_TEST_NET = okapi-test-network

fe-dist:
	@python3 build-scripts/fe_dist_copy.py

package:
	mvn package -T 4 -DskipTests=true

TAG ?= latest

docker-okapi-ingester: package
	docker build -t $(REPO)/okapi-ingester:$(TAG) -f okapi-ingester/Dockerfile okapi-ingester

docker-okapi-web: fe-dist package
	docker build -t $(REPO)/okapi-web:$(TAG) -f okapi-web/Dockerfile okapi-web

docker-okapi-ops: package
	docker build -t $(REPO)/okapi-ops:$(TAG) -f okapi-ops/Dockerfile okapi-ops

docker-all: docker-okapi-ingester docker-okapi-web docker-okapi-ops

DOCKER_CMD := docker run -d --name
DOCKER_RM := sh stop_and_remove_container.sh
DOCKER_STOP := docker stop

localstack:
	localstack stop || true
	$(DOCKER_RM) localstack-main
	docker run --name localstack-main \
	--network $(OKAPI_TEST_NET) \
	-p 4566:4566 -d \
	localstack/localstack:latest

tables:
	java -cp okapi-data-ddb/target/okapi-data-ddb-0.0.1-SNAPSHOT.jar  org.okapi.data.CreateDynamoDBTables $(ENV:ENV=test)
	java -cp okapi-data-ddb/target/okapi-data-ddb-0.0.1-SNAPSHOT.jar  org.okapi.data.CreateS3Bucket  $(ENV:ENV=test)

stop-test-infra:
	localstack stop || true
	$(DOCKER_RM) okapi-clickhouse

localstack-k8s:
	kubectl apply -n okapi -f okapi-ingester/local-stack-yamls/localstack.yml

testnetwork:
	sh test-network.sh $(OKAPI_TEST_NET)

run-zk:
	$(DOCKER_RM) zookeeper
	$(DOCKER_CMD) zookeeper --network $(OKAPI_TEST_NET) -p 2181:2181 zookeeper:latest

ch:
	$(DOCKER_RM) okapi-clickhouse
	$(DOCKER_CMD) \
	okapi-clickhouse \
	--network $(OKAPI_TEST_NET) \
	-p 8123:8123 \
	-p 9000:9000 \
	-e CLICKHOUSE_PASSWORD=okapi_testing_password \
	--ulimit nofile=262144:262144 \
	-v "$(ch_dir)/ch_data:/var/lib/clickhouse/" \
	-v "$(ch_dir)/ch_logs:/var/log/clickhouse-server/" \
	clickhouse/clickhouse-server

test-secret:
	aws secretsmanager create-secret \
	--name '/okapi/secrets' \
	--description "Okapi test secrets" \
	--secret-string '{"hmacKey":"5e1a04d3","apiKey":"a2991d99"}' \
	--region us-west-2 \
	--endpoint-url http://localhost:4566


migrate:
	java -jar okapi-ops/target/okapi-ops-0.0.1-SNAPSHOT.jar ddb-migrate --env local --region us-west-2
	java -jar okapi-ops/target/okapi-ops-0.0.1-SNAPSHOT.jar ch-migrate --host localhost --port 8123 --user default --password okapi_testing_password

test-data:
	java -jar okapi-datagen/target/okapi-datagen-0.0.1-SNAPSHOT.jar astro-spans-gen --host http://localhost --port 9009
	java -jar okapi-datagen/target/okapi-datagen-0.0.1-SNAPSHOT.jar users-gen --host http://localhost --port 9001

test-infra: testnetwork localstack test-secret ch migrate

run-ingester:
	java -jar okapi-ingester/target/okapi-ingester-0.0.1-SNAPSHOT.jar &

build: run-ingester
	mvn package -T 4

all: package test-infra build docker-all

test-run-ingester:
	$(DOCKER_RM) okapi-ingester
	docker run -p 9009:9009 -d \
	--network $(OKAPI_TEST_NET) \
	--name okapi-ingester \
	$(REPO)/okapi-ingester:$(TAG) \
	--okapi.clickhouse.host=okapi-clickhouse \
	--okapi.chMetricsWal=/wal/metrics \
	--okapi.chLogsWal=/wal/metrics \
	--okapi.chTracesWal=/wal/metrics


test-run-web:
	$(DOCKER_RM) okapi-web
	docker run -p 9001:9001 -d \
	--network $(OKAPI_TEST_NET) \
	-e AWS_ACCESS_KEY_ID=$(AWS_ACCESS_KEY_ID) \
	-e AWS_SECRET_ACCESS_KEY=$(AWS_SECRET_ACCESS_KEY) \
	-e AWS_REGION=$(AWS_REGION) \
	--name okapi-web \
	$(REPO)/okapi-web:$(TAG) \
	--clusterEndpoint=http://okapi-ingester:9009 \
	--okapi.aws.endpoint=http://localstack-main:4566

test-run: test-run-ingester test-run-web
publish:
	docker push $(REPO)/okapi-web:$(TAG)
	docker push $(REPO)/okapi-ingester:$(TAG)
	docker push $(REPO)/okapi-ops:$(TAG)
