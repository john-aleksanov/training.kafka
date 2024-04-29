package dev.marvel.kafka.fleetconsumer;

import java.time.LocalDateTime;
import java.util.Objects;

public record GeoData(LocalDateTime timestamp, String vehicleId, Coordinate coordinate) {
    public GeoData {
        Objects.requireNonNull(timestamp, "GeoData timestamp cannot be null");
        Objects.requireNonNull(vehicleId, "GeoData vehicle ID cannot be null");
        Objects.requireNonNull(coordinate, "GeoData coordinate cannot be null");
    }
}
