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
        consumer = new KafkaConsumer<>(consumerProps);
        consumer.subscribe(Collections.singletonList(INPUT_TOPIC));

        var producerProps = new Properties();
        producerProps.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVER);
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, DoubleSerializer.class);
        producer = new KafkaProducer<>(producerProps);

        latestTruckData = new HashMap<>();
    }

    public static void main(String[] args) {
        var distanceCalculator = new DistanceCalculator();
        distanceCalculator.run();
    }

    public void run() {
        while (true) {
            var geoData = consumer.poll(Duration.ofSeconds(1));
            geoData.forEach(this::processRecord);
        }
    }

    private void processRecord(ConsumerRecord<String, GeoData> record) {
        var vehicleId = record.key();
        var geoDatum = record.value();
        latestTruckData.compute(vehicleId, (k, oldValue) -> oldValue == null
            ? new LatestTruckData(vehicleId, geoDatum.coordinate(), 0d)
            : oldValue.update(geoDatum));
        var distance = latestTruckData.get(vehicleId).distanceTraveled();
        producer.send(new ProducerRecord<>(OUTPUT_TOPIC, vehicleId, distance));
        logger.info("Sent truck distance {} for truck {}", distance, vehicleId);
    }
}
