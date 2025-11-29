package com.example.code_zombom_app.Helpers.Location;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.maps.android.heatmaps.HeatmapTileProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
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

    private static final String GOOGLE_API = "AIzaSyDEZ31HqSOzmjV0acyJk22MJjHjZKg2pXs";

    /**
     * No-arg constructor to allow deserializable by Firebase
     */
    public Location() {}

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
        return ((name != null && !name.trim().isEmpty()) ? (name + ", ") : "") + street + ", " +
                city + ", " + province + ", " + country +
                ", " + postalCode + ", (" + String.valueOf(coordinate.getLatitude()) + ", " +
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
     * Set the name of the location
     *
     * @param name The name of the location
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     *
     * @return The location's street
     */
    public String getStreet() {
        return street;
    }

    /**
     * Set the street
     *
     * @param street The street name
     */
    public void setStreet(String street) {
        this.street = street;
    }

    /**
     *
     * @return The location's city
     */
    public String getCity() {
        return city;
    }

    /**
     * Set the city
     *
     * @param city The city's name
     */
    public void setCity(String city) {
        this.city = city;
    }

    /**
     *
     * @return The location's province
     */
    public String getProvince() {
        return province;
    }

    /**
     * Set the province
     *
     * @param province The province name
     */
    public void setProvince(String province) {
        this.province = province;
    }

    /**
     *
     * @return The location's country
     */
    public String getCountry() {
        return country;
    }

    /**
     * Set the country
     *
     * @param country The country's name
     */
    public void setCountry(String country) {
        this.country = country;
    }

    /**
     *
     * @return The location's postal code
     */
    public String getPostalCode() {
        return postalCode;
    }

    /**
     * Set the postal code
     *
     * @param postalCode The postal code to set
     */
    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    /**
     *
     * @return The location's coordinate
     */
    public Coordinate getCoordinate() {
        return coordinate;
    }

    /**
     * Set the coordinate
     *
     * @param coordinate The coordinate to set
     */
    public void setCoordinate(Coordinate coordinate) {
        this.coordinate = coordinate;
    }

    /**
     * @return The Google API key
     */
    public static String getGoogleApi() {
        return GOOGLE_API;
    }

    /**
     * Convert a coordinate to a real-life address.
     *
     * @param coordinate The coordinate to convert to a real-life location
     * @return A location if success, null otherwise
     * @throws RuntimeException If the connection to the real-life address services is corrupted
     */
    public static Location fromCoordinates(Coordinate coordinate) {
        try {
            String urlStr = "https://maps.googleapis.com/maps/api/geocode/json?latlng="
                    + coordinate.getLatitude() + "," + coordinate.getLongitude()
                    + "&key=" + GOOGLE_API;

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            StringBuilder json = new StringBuilder();

            try (Scanner scanner = new Scanner(conn.getInputStream())) {
                while (scanner.hasNext())
                    json.append(scanner.nextLine());
            }

            JSONObject root = new JSONObject(json.toString());
            JSONArray results = root.getJSONArray("results");
            if (results.length() == 0)
                return null;

            JSONObject first = results.getJSONObject(0);

            String street = getComponent(first, "route");
            String houseNumber = getComponent(first, "street_number");
            String fullStreet = houseNumber.isEmpty() ? street : houseNumber + " " + street;
            String city = getComponent(first, "locality");
            String province = getComponent(first, "administrative_area_level_1");
            String country = getComponent(first, "country");
            String postalCode = getComponent(first, "postal_code");

            return new Location("", fullStreet, city, province, country,
                    postalCode, coordinate);
        } catch (IOException | JSONException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Generate a heatmap that pins all the registered locations on a Google Map.
     *
     * @param map       The GoogleMap object where the heatmap will be drawn
     * @param locations The list of locations to be put on the heatmap
     * @return The TileOverlay created for the heatmap, or null if nothing was drawn
     */
    public static TileOverlay generateHeatMap(GoogleMap map, List<Location> locations) {
        if (map == null || locations == null || locations.isEmpty()) {
            return null;
        }

        List<LatLng> latLngList = new ArrayList<>();
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        boolean hasAnyPoint = false;

        for (Location loc : locations) {
            if (loc == null || loc.getCoordinate() == null) continue;

            double lat = loc.getCoordinate().getLatitude();
            double lng = loc.getCoordinate().getLongitude();

            LatLng latLng = new LatLng(lat, lng);
            latLngList.add(latLng);
            boundsBuilder.include(latLng);
            hasAnyPoint = true;
        }

        if (!hasAnyPoint) {
            return null;
        }

        // Create the heatmap tile provider
        HeatmapTileProvider provider = new HeatmapTileProvider.Builder()
                .data(latLngList)
                .radius(40) // Size of blobs
                .opacity(0.7) // transparency (0 = transparent, 1 = opaque)
                .build();

        TileOverlay overlay = map.addTileOverlay(
                new TileOverlayOptions().tileProvider(provider)
        );

        // Move/zoom camera
        if (latLngList.size() == 1) {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLngList.get(0), 15f));
        } else {
            LatLngBounds bounds = boundsBuilder.build();
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
        }

        return overlay;
    }

    /**
     * Extracts the value of a specific component from a Google Geocoding API
     * {@code address_components} array.
     * <p>
     * The Google Geocoding API returns an array of {@code address_components} for each result,
     * where each component has a {@code long_name}, {@code short_name}, and a set of {@code types}.
     * This helper searches through all components and returns the {@code long_name} of the first
     * component that matches the specified type.
     * </p>
     *
     * <p>Example types include:</p>
     * <ul>
     *     <li>{@code street_number}</li>
     *     <li>{@code route}</li>
     *     <li>{@code locality} (city)</li>
     *     <li>{@code administrative_area_level_1} (state/province)</li>
     *     <li>{@code country}</li>
     *     <li>{@code postal_code}</li>
     * </ul>
     *
     * @param result A JSONObject representing a single Google Geocoding API result
     *               (typically an element from the "results" array).
     * @param type   The type of address component to extract (e.g., "locality", "country").
     * @return The {@code long_name} of the first matching component, or an empty string if
     *         no component of the specified type is found.
     * @throws RuntimeException If there is nay unexpected errors in querying the JSON file
     * @see JSONObject
     * @see JSONArray
     */
    private static String getComponent(JSONObject result, String type) {
        try {
            JSONArray components = result.getJSONArray("address_components");
            for (int i = 0; i < components.length(); i++) {
                JSONObject comp = components.getJSONObject(i);
                JSONArray types = comp.getJSONArray("types");
                for (int j = 0; j < types.length(); j++) {
                    if (types.getString(j).equals(type)) {
                        return comp.getString("long_name");
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return "";
    }
}
