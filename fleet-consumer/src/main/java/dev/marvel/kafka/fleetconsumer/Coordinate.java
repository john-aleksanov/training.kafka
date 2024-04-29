package dev.marvel.kafka.fleetconsumer;

public record Coordinate(double latitude, double longitude) {

    private static final int EARTH_RADIUS_IN_KM = 6_371;

    public Coordinate {
        if (latitude < -90.0 || latitude > 90.0) {
            throw new IllegalArgumentException("Latitude should be between -90.0 and 90.0.");
        }
        if (longitude < -180.0 || longitude > 180.0) {
            throw new IllegalArgumentException("Longitude should be between 180.0 and 180.0.");
        }
    }
}
