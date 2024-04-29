package dev.marvel.kafka.distancecalculator;

public record LatestTruckData(String vehicleId, Coordinate coordinate, double distanceTraveled) {

    public LatestTruckData update(GeoData data) {
        var newCoordinate = data.coordinate();
        return new LatestTruckData(vehicleId, newCoordinate, distanceTraveled + coordinate.calculateDistance(newCoordinate));
    }
}
