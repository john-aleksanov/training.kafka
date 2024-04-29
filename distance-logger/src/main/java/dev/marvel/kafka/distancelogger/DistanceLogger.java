package dev.marvel.kafka.distancelogger;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.DoubleDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class DistanceLogger {

    private static final Logger logger = LoggerFactory.getLogger(DistanceLogger.class);

    private static final String BOOTSTRAP_SERVER = "localhost:9092";
    private static final String INPUT_TOPIC = "output";

    private final Consumer<String, Double> consumer;

    public DistanceLogger() {
        var consumerProps = new Properties();
        consumerProps.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVER);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, DoubleDeserializer.class);
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "distance-calculator");
        consumer = new KafkaConsumer<>(consumerProps);
        consumer.subscribe(Collections.singletonList(INPUT_TOPIC));
    }

    public static void main(String[] args) {
        var distanceLogger = new DistanceLogger();
        distanceLogger.run();
    }

    public void run() {
        while (true) {
            var records = consumer.poll(Duration.ofSeconds(1));
            records.forEach(this::processRecord);
        }
    }

    private void processRecord(ConsumerRecord<String, Double> record) {
        var vehicleId = record.key();
        var distance = record.value();
        logger.info("Vehicle {} has traveled {} so far", vehicleId, distance);
    }
}
