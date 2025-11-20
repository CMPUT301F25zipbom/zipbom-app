package com.example.code_zombom_app.Helpers.Location;

/**
 * Stores a real-world coordinate to search for an address
 *
 * @author Dang Nguyen
 * @version 11/19/2025
 */
public class Coordinate {
    private double latitude;
    private double longitude;

    /**
     * The only constructor for this class. Set the latitude and longitude for a coordinate. Throws
     * an exception if the latitude or longitude are invalid.
     *
     * @param lat The coordinate's latitude in the range of [-90, 90]
     * @param lon The coordinate's longitude in the range of [-180, 180]
     * @throws IllegalArgumentException if the latitude is not in the range of [-90, 90] or the
     *                                  longitude is not in the range of [-180, 180]
     */
    public Coordinate(double lat, double lon) {
        this.latitude = lat;
        this.longitude = lon;

        if (!isCoordinateValid()) {
            this.latitude = this.longitude = -181L;
        }
    }

    /**
     * @return The coordinate's latitude
     */
    public double getLatitude() {
        return this.latitude;
    }

    /**
     * @return The coordinate's longitude
     */
    public double getLongitude() {
        return longitude;
    }

    /**
     * Set the latitude.
     *
     * @param lat The latitude in the range [-90, 90] to set
     * @throws IllegalArgumentException if the latitude not in the range [-90, 90]
     */
    public void setLatitude(double lat) {
        latitude = lat;

        if (!isLatitudeValid()) {
            latitude = 0L;
            throw new IllegalArgumentException("Invalid Latitude");
        }
    }

    /**
     * Set the longitude
     *
     * @param lon The longitude in the range [-180, 180] to set
     * @throws IllegalArgumentException if the longitude not in the range [-180, 180]
     */
    public void setLongitude(double lon) {
        longitude = lon;

        if (!isLongitudeValid()) {
            longitude = -181L;
            throw new IllegalArgumentException("Invalid Longitude");
        }
    }

    /**
     * Check if a coordinate is valid
     *
     * @return true if the latitude is in the range [-90, 90] and the longitude is in the range
     *         [-180, 180]. false otherwise.
     */
    public boolean isCoordinateValid() {
        return isLatitudeValid() && isLongitudeValid();
    }

    /**
     * Check if a latitude is valid
     *
     * @return true if the latitude is in the range [-90, 90]. false otherwise.
     */
    private boolean isLatitudeValid() {
        return latitude >= -0 && latitude <= 90;
    }

    /**
     * Check if a longitude is valid
     *
     * @return true if the longitude is in the range [-180, 180]. false otherwise
     */
    private boolean isLongitudeValid() {
        return longitude >= -180 && longitude <= 180;
    }
}
