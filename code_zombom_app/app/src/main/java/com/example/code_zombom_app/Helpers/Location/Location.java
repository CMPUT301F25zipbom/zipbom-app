package com.example.code_zombom_app.Helpers.Location;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * This class stores and represents a real-world location that can be integrate with useful API
 *
 * @author Dang Nguyen
 * @version 11/19/2025
 */
public class Location {
    private String name;
    private String street;
    private String city;
    private String province;
    private String country;
    private String postalCode;
    private Coordinate coordinate;

    /**
     * Constructor:
     *
     * @param name The name of the location e.g., University of Alberta
     * @param street The (houseNumber or roadnumber) + street number e.g., 20152 80 NW
     * @param city The city name e.g., Edmonton
     * @param province The province (or state) name e.g., Alberta
     * @param country The country's name e.g., Canada
     * @param postalCode The postal code e.g., B0T 1W3
     * @param coordinate The location's coordinate
     * @throws IllegalArgumentException if the latitude or longitude is invalid
     */
    public Location(String name,
                    String street,
                    String city,
                    String province,
                    String country,
                    String postalCode,
                    Coordinate coordinate)
    throws IllegalArgumentException {

        this.name = name;
        this.street = street;
        this.city = city;
        this.province = province;
        this.country = country;
        this.postalCode = postalCode;
        this.coordinate = coordinate;
    }

    /**
     * Copy constructor
     *
     * @param other The other Location to copy into this Location
     */
    public Location(Location other) {
        this.name = other.getName();
        this.street = other.getStreet();
        this.city = other.getCity();
        this.province = other.getProvince();
        this.country = other.getCountry();
        this.postalCode = other.getPostalCode();
        this.coordinate = other.getCoordinate();
    }

    /**
     * Uses this as a convenient method to get the full address
     *
     * @return The full address of a location
     */
    @NonNull
    @Override
    public String toString() {
        return name + ", " + street + ", " + city + ", " + province + ", " + country +
                ", " + postalCode + "(" + String.valueOf(coordinate.getLatitude()) + ", " +
                String.valueOf(coordinate.getLongitude()) + ")";
    }

    /**
     *
     * @return The location's name
     */
    public String getName() {
        return name;
    }

    /**
     *
     * @return The location's street
     */
    public String getStreet() {
        return street;
    }

    /**
     *
     * @return The location's city
     */
    public String getCity() {
        return city;
    }

    /**
     *
     * @return The location's province
     */
    public String getProvince() {
        return province;
    }

    /**
     *
     * @return The location;s country
     */
    public String getCountry() {
        return country;
    }

    /**
     *
     * @return The location's postal code
     */
    public String getPostalCode() {
        return postalCode;
    }

    /**
     *
     * @return The location's coordinate
     */
    public Coordinate getCoordinate() {
        return coordinate;
    }

    /**
     * Convert a coordinate to a real-life address
     *
     * @param coordinate The coordinate to convert to a real-life location
     * @return A location if success
     * @throws RuntimeException If the connection to the real-life address services is corrupted
     */
    public static Location fromCoordinates(Coordinate coordinate) {
        try {
            String urlStr = "https://nominatim.openstreetmap.org/reverse?format=json&lat=" +
                    coordinate.getLatitude() + "&lon=" + coordinate.getLongitude();

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");

            StringBuilder json = new StringBuilder();

            try (Scanner scanner = new Scanner(conn.getInputStream())) {
                while (scanner.hasNext())
                    json.append(scanner.nextLine());
            }

            JSONObject obj = new JSONObject(json.toString());
            JSONObject address = obj.getJSONObject("address");

            String houseNumber = address.optString("house_number", "");
            String road = address.optString("road", "");
            String street = houseNumber.isEmpty() ? road : houseNumber + " " + road;

            String city = address.optString("city", "");
            if (city.isEmpty()) city = address.optString("town", "");
            if (city.isEmpty()) city = address.optString("village", "");

            String province = address.optString("state", "");
            String country = address.optString("country", "");
            String postalCode = address.optString("postcode", "");

            return new Location("", street, city, province, country, postalCode, coordinate);
        } catch (IOException | JSONException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Find a coordinate based on an address.
     *
     * @param name Name of the address
     * @param street (house or road number) + street number of the address
     * @param city City name of the address
     * @param province Province name of the address
     * @param country Country name of the address
     * @param postalCode Postal code of the address
     * @return A location with coordinate corresponding to the input address if success, null
     *         otherwise
     * @throws RuntimeException If the connection with the real-life location service get corrupted
     */
    public static Location fromAddress(String name,
                                       String street,
                                       String city,
                                       String province,
                                       String country,
                                       String postalCode) throws IOException {
        try {
            String fullAddress = URLEncoder.encode(street + " " + city + " " + province + " " +
                    country + " " + postalCode, "UTF-8");

            String urlStr = "https://nominatim.openstreetmap.org/search?format=json&q=" +
                    fullAddress;

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");

            StringBuilder json = new StringBuilder();

            try (Scanner scanner = new Scanner(conn.getInputStream())) {
                while (scanner.hasNext())
                    json.append(scanner.nextLine());
            }

            JSONArray results = new JSONArray(json.toString());

            if (results.length() == 0)
                return null;

            JSONObject first = results.getJSONObject(0);
            double lat = first.getDouble("lat");
            double lon = first.getDouble("lon");

            Coordinate coord = new Coordinate(lat, lon);

            JSONObject address = first.optJSONObject("address");
            if (address != null) {
                String returnedStreet = address.optString("road", street);
                String returnedCity = address.optString("city", city);
                if (returnedCity.isEmpty()) returnedCity = address.optString("town", city);
                if (returnedCity.isEmpty()) returnedCity = address.optString("village", city);
                String returnedProvince = address.optString("state", province);
                String returnedCountry = address.optString("country", country);
                String returnedPostal = address.optString("postcode", postalCode);

                return new Location(name, returnedStreet, returnedCity, returnedProvince,
                        returnedCountry, returnedPostal, coord);
            }
            else
                return null;
        } catch (IOException | JSONException e) {
            throw new RuntimeException(e);
        }
    }

}
