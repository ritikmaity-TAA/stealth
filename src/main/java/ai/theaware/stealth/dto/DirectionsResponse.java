package ai.theaware.stealth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DirectionsResponse {

    public List<Route> routes;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Route {
        public OverviewPolyline overviewPolyline;
        public List<Leg> legs;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OverviewPolyline {
        public String encodedPath;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Leg {
        public LatLng startLocation;
        public LatLng endLocation;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LatLng {
        public double lat;
        public double lng;
    }


    public boolean isEmpty() {
        return routes == null || routes.isEmpty();
    }

    public LatLng getStartLocation() {
        return routes.get(0).legs.get(0).startLocation;
    }

    public LatLng getEndLocation() {
        List<Leg> legs = routes.get(0).legs;
        return legs.get(legs.size() - 1).endLocation;
    }

    public String getEncodedPath(int routeIndex) {
        return routes.get(routeIndex).overviewPolyline.encodedPath;
    }
}