package dev.marvel.kafka.distancecalculator;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.DoubleSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class DistanceCalculator {

    private static final Logger logger = LoggerFactory.getLogger(DistanceCalculator.class);

    private static final String BOOTSTRAP_SERVER = "localhost:9092";
    private static final String INPUT_TOPIC = "input";
    private static final String OUTPUT_TOPIC = "output";

    private final KafkaConsumer<String, GeoData> consumer;
    private final KafkaProducer<String, Double> producer;
    private final Map<String, LatestTruckData> latestTruckData;

    public DistanceCalculator() {
        var consumerProps = new Properties();
        consumerProps.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVER);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, GeoDataDeserializer.class);
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "distance-calculator");
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        consumer = new KafkaConsumer<>(consumerProps);
        consumer.subscribe(Collections.singletonList(INPUT_TOPIC));

        var producerProps = new Properties();
        producerProps.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVER);
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, DoubleSerializer.class);
        producerProps.put(ProducerConfig.ACKS_CONFIG, "all");
        producerProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
        producer = new KafkaProducer<>(producerProps);

        latestTruckData = new HashMap<>();
    }

    public static void main(String[] args) {
        var distanceCalculator = new DistanceCalculator();
        distanceCalculator.run();
    }

    public void run() {
        while (true) {
            var records = consumer.poll(Duration.ofSeconds(1));
            records.forEach(this::processRecord);
            consumer.commitSync();
        }
    }

    private void processRecord(ConsumerRecord<String, GeoData> record) {
        var vehicleId = record.key();
        var geoDatum = record.value();

        var existingTruckData = latestTruckData.get(vehicleId);
        if (existingTruckData == null) {
            latestTruckData.put(vehicleId, new LatestTruckData(geoDatum.timestamp(), vehicleId, geoDatum.coordinate(), 0d));
            producer.send(new ProducerRecord<>(OUTPUT_TOPIC, vehicleId, 0d));
            logger.info("Received first GPS coordinates for truck {}", vehicleId);
            return;
        }

        var existingTimestamp = existingTruckData.timestamp();
        if (!geoDatum.timestamp().isAfter(existingTimestamp)) {
            logger.info("Got a message that we've already processed, skipping");
            return;
        }

        var distance = latestTruckData.get(vehicleId).distanceTraveled();
        latestTruckData.put(vehicleId, existingTruckData.update(geoDatum));
        producer.send(new ProducerRecord<>(OUTPUT_TOPIC, vehicleId, distance));
        logger.info("Sent truck distance {} for truck {}", distance, vehicleId);
    }
}
