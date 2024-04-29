package dev.marvel.kafka.distancecalculator;

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

    public double calculateDistance(Coordinate other) {
        double thisLatInRadians = Math.toRadians(this.latitude);
        double thisLonInRadians = Math.toRadians(this.longitude);
        double otherLatInRadians = Math.toRadians(other.latitude);
        double otherLonInRadians = Math.toRadians(other.longitude);

        double dLat = thisLatInRadians - otherLatInRadians;
        double dLon = thisLonInRadians - otherLonInRadians;

        double haversine = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(thisLatInRadians) * Math.cos(otherLatInRadians) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double angularDistance = 2 * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine));

        return EARTH_RADIUS_IN_KM * angularDistance;
    }
}
