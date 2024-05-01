## Kafka Showcase Project

This project sets up a Kafka cluster using Docker and Docker Compose, alongside a multi-module Gradle project designed to interact with the
Kafka environment for handling and processing geo data. See [task.md](./task.md) for a description of the task.

## Prerequisites

- Docker
- Docker Compose
- Java 17 (although this would also compile with JDK 16 as no sealed types were used in development)
- Gradle 7.3 or higher (see Gradle / Java compatibility matrix [here](https://docs.gradle.org/current/userguide/compatibility.html))

## Project Structure

The Docker compose file in the root directory configures three Kafka brokers that comprise the Kafka cluster. Additionally, there are three
main modules / services in the Gradle project:

- fleet-consumer
- distance-logger
- distance-calculator

Each service is designed to perform specific tasks within the data processing pipeline.

## Running the Kafka Cluster

To start the Kafka cluster, navigate to the root directory of this project and run the following command:

```shell
docker-compose up
```

This command builds and starts all services defined in the docker-compose.yml file, creating a networked cluster of Kafka brokers. After the
cluster has been run, create two kafka topics - `input` and `output`:

```shell
docker exec -it kafka3 kafka-topics.sh --create --bootstrap-server kafka3:19092 --topic input --partitions 3 --replication-factor 2
```

```shell
docker exec -it kafka3 kafka-topics.sh --create --bootstrap-server kafka3:19092 --topic output --partitions 3 --replication-factor 2
```

## Services

### Fleet-consumer

This service acts as a REST service to receive geo data via HTTP POST requests and pushes it to a Kafka topic named `input`.
After ensuring the Kafka cluster is running, start the service by navigating to the [fleet-consumer](./fleet-consumer) directory and
running:

```shell
./gradlew bootRun
```

or just run it from within your favorite IDE.

You can submit geo data to the service using:

```shell
curl -X POST http://localhost:8081/fleet-consumer/geo -H 'Content-Type: application/json' -d '{"vehicleId": "abc123", "timestamp": "2024-01-05T13:04:06", "coordinate": {"latitude": 40.712776, "longitude": -74.005974}}'
```

### Distance-calculator

Calculates the distance traveled by each vehicle based on incoming geo data from the `input` topic, and publishes the results to the
`output` topic.

After ensuring the Kafka cluster is running, start the service by navigating to the [distance-calculator](./distance-calculator) directory
and running:

```shell
./gradlew run
```

or just run it from within your favorite IDE. The service is expected to work in a cluster of three instances, so run three instances.
You will see in the logs that each instance subscribes to its own partition of the `input` topic. Here are example logs from one of the
instances showing it subscribed to partition `input-2`:

```text
2024-04-29 13:36:59 [main] INFO  org.apache.kafka.clients.consumer.internals.ConsumerRebalanceListenerInvoker - [Consumer clientId=consumer-distance-calculator-1, groupId=distance-calculator] Adding newly assigned partitions: input-2
2024-04-29 13:36:59 [main] INFO  org.apache.kafka.clients.consumer.internals.ConsumerUtils - Setting offset for partition input-2 to the committed offset FetchPosition{offset=3, offsetEpoch=Optional[0], currentLeader=LeaderAndEpoch{leader=Optional[localhost:9092 (id: 2 rack: null)], epoch=0}}
```

### Distance-logger

Subscribes to the `output` topic and logs the calculated distances to the console.

After ensuring the Kafka cluster is running, start the service by navigating to the [distance-logger](./distance-logger) directory
and running:

```shell
./gradlew run
```

or just run it from within your favorite IDE.

## Processing Guarantees

### Sequential processing of vehicle data

In our pipeline, we ensure that data for each vehicle is processed sequentially. The key to achieving sequential processing lies in how we
submit records to the Kafka `input` topic. We use the vehicle ID as the key for each message. Kafka guarantees that all messages sharing the
same key (in this case, the vehicle ID) are routed to the same partition. This is a feature of Kafka's default partitioner, which ensures
that the same key always maps to the same partition.

We have configured three partitions for the `input` topic and have three consumers, each belonging to the same consumer group. This setup
means that each consumer is typically assigned to one specific partition. Since all messages from a single vehicle go to the same partition,
they are processed by the same consumer. This arrangement ensures that there's no overlap in processing vehicle data, and each vehicle's
data is handled sequentially by a single consumer.

### Transactional processing

In Kafka, the default approach to acknowledging messages is through automated commits. Typically, consumer offsets are committed
automatically at each poll, with a default commit interval (auto-commit interval) of 5000 milliseconds. This behavior is designed for
convenience and efficiency.

We modify this default behavior by manually committing offsets synchronously using `consumer.commitSync()`. This method is called only after
all messages from the current poll are successfully processed and produced to the output topic. By manually committing after the messages
are produced to the output topic, we ensure that the entire process — from reading from the input topic to pushing to the output — as a
single transaction. If any part of this chain fails, the offset is not committed, which means the consumer will reprocess the same message
upon the next poll. This guarantees that no data is lost or incorrectly processed due to partial failures.

To make our approach fully work, we also need to make our consumer idempotent, that is, be capable of handling duplicate messages correctly.
For that, we analyze the timestamp of each geo datum that we receive from `fleet-consumer` and compare it to the timestamp that we have on
hand for the given `vehicleId`. If the timestamp in the Kafka record is not later than the one we have as the latest timestamp, this means
we've already processed this record, so we skip it.

## Notes & Limitations

This project serves as a basic example to demonstrate usage of Kafka in processing pipelines. Due to its simplicity, certain shortcuts have
been made and certain things hardcoded. In a production setting, the following improvements would be made:

1. Testing: Currently, no tests are available. A TDD approach would be adopted.
2. Observability: Extensive logging and performance metrics would be added.
3. Architecture: Domain-Driven Design / ports - adapters architecture would be used for better decoupling.
4. Error Handling: Robust error handling mechanisms would be in place.
5. API first: An OpenAPI description of the services' APIs would be provided.
6. Gradle project structure: Common parts of Gradle build descriptors would be extracted to a parent `build.gradle` file.
7. Deployment: Cloud-native approach to deployment would be implemented and Kubernetes / Helm would be used to run the application.
8. Framework: Depending on the ecosystem and company's platform, we might refrain from using Spring / Spring Boot in favor of other
   approaches, e.g. code using the Servlet API directly.
9. Persistence: Latest vehicle data would need to be persisted to survive service restarts and ensure correct distributed processing. 
10. Security: Production-grade security would have been implemented at all levels including:
- secured Kafka cluster (TLS for encryption in transit, SASL for client authentication, ACL / RBAC for authorization if required, data
  encryption at rest);
- secured API (API keys or JWT);
- network-level security (routing tables, VPCs, firewalls);
- etc., etc.