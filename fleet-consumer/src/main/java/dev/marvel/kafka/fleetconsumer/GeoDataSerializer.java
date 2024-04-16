package dev.marvel.kafka.fleetconsumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Serializer;

import java.util.Map;

public class GeoDataSerializer implements Serializer<GeoData> {

    private ObjectMapper mapper;

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        Serializer.super.configure(configs, isKey);
        mapper = new ObjectMapper();
    }

    @Override
    public byte[] serialize(String topic, GeoData geoData) {
        try {
            return mapper.writeValueAsBytes(geoData);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize data", e);
        }
    }

    @Override
    public byte[] serialize(String topic, Headers headers, GeoData data) {
        return Serializer.super.serialize(topic, headers, data);
    }

    @Override
    public void close() {
        Serializer.super.close();
    }
}
