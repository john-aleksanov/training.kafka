package dev.marvel.kafka.fleetconsumer;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/geo")
public class GeoDataController {

    private static final Logger log = LoggerFactory.getLogger(GeoDataController.class);

    private final String topic;
    private final KafkaProducer<String, GeoData> producer;

    public GeoDataController(KafkaProducer<String, GeoData> producer) {
        this.producer = producer;
        this.topic = "input";
    }

    @PostMapping
    public void submit(@RequestBody GeoData geoData) {
        log.info("Received geo data {}", geoData);
        producer.send(new ProducerRecord<>(topic, geoData.vehicleId(), geoData));
    }
}
