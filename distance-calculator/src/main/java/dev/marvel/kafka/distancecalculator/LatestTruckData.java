package dev.marvel.kafka.distancecalculator;

import java.time.LocalDateTime;

public record LatestTruckData(LocalDateTime timestamp, String vehicleId, Coordinate coordinate, double distanceTraveled) {

    public LatestTruckData update(GeoData data) {
        var newCoordinate = data.coordinate();
        return new LatestTruckData(data.timestamp(), vehicleId, newCoordinate, distanceTraveled + coordinate.calculateDistance(newCoordinate));
    }
}
