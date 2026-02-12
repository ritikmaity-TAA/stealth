package ai.theaware.stealth.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.google.maps.DirectionsApi;
import com.google.maps.GeoApiContext;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;
import com.google.maps.model.LatLng;

import ai.theaware.stealth.dto.RouteResponseDTO;
import ai.theaware.stealth.entity.Route;
import ai.theaware.stealth.entity.Users;
import ai.theaware.stealth.repository.RouteRepository;
import ai.theaware.stealth.repository.UserRepository;
import tools.jackson.databind.ObjectMapper;

@Service
public class GoogleRoutingService {

    @Value("${google.maps.api.key}")
    private String apiKey;

    private final RouteRepository routeRepository;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final GeometryFactory geometryFactory;

    private static final double INTERVAL_METERS = 1000.0;
    private static final String AI_SERVICE_URL = "http://ai-service:8000/predict/aqi";

    public GoogleRoutingService(RouteRepository routeRepository, UserRepository userRepository) {
        this.routeRepository = routeRepository;
        this.userRepository = userRepository;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
    }

    public RouteResponseDTO getSmartRoute(Double sLat, Double sLon, Double dLat, Double dLon) {
        try {
            Optional<Route> cachedRoute = routeRepository.findCachedRoute(sLat, sLon, dLat, dLon);
            if (cachedRoute.isPresent()) {
                System.out.println("LOG: Global Cache Hit. Avoiding Google API call.");
                return objectMapper.readValue(cachedRoute.get().getCachedResponseJson(), RouteResponseDTO.class);
            }

            System.out.println("LOG: Cache Miss. Initiating Google Maps API & AI Handoff...");
            GeoApiContext context = new GeoApiContext.Builder().apiKey(apiKey).build();
            DirectionsResult result = DirectionsApi.newRequest(context)
                    .origin(new LatLng(sLat, sLon))
                    .destination(new LatLng(dLat, dLon))
                    .alternatives(true)
                    .await();

            List<RouteResponseDTO.RouteDetail> routeDetails = new ArrayList<>();

            for (DirectionsRoute route : result.routes) {
                List<RouteResponseDTO.Coordinate> rawCoords = route.overviewPolyline.decodePath()
                        .stream()
                        .map(p -> new RouteResponseDTO.Coordinate(p.lat, p.lng))
                        .collect(Collectors.toList());

        
                List<RouteResponseDTO.Coordinate> cleanedCoords = resamplePath(rawCoords, INTERVAL_METERS);

                routeDetails.add(new RouteResponseDTO.RouteDetail(
                        route.legs[0].distance.humanReadable,
                        route.legs[0].distance.inMeters,
                        route.legs[0].duration.humanReadable,
                        cleanedCoords
                ));
            }

            RouteResponseDTO intermediateResponse = new RouteResponseDTO(routeDetails.size(), routeDetails);

            RouteResponseDTO aqiEnhancedResponse = restTemplate.postForObject(AI_SERVICE_URL, intermediateResponse, RouteResponseDTO.class);

            Users currentUser = getAuthenticatedUser();
            saveToDatabase(sLat, sLon, dLat, dLon, aqiEnhancedResponse, currentUser);

            return aqiEnhancedResponse;

        } catch (Exception e) {
            throw new RuntimeException("Routing Error: " + e.getMessage());
        }
    }

    private Users getAuthenticatedUser() {
        String username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User session not found in database"));
    }

    private void saveToDatabase(Double sLat, Double sLon, Double dLat, Double dLon, RouteResponseDTO data, Users user) throws Exception {
        Route newRoute = new Route();
        newRoute.setUser(user);
        newRoute.setStartLat(sLat);
        newRoute.setStartLon(sLon);
        newRoute.setEndLat(dLat);
        newRoute.setEndLon(dLon);
        newRoute.setCachedResponseJson(objectMapper.writeValueAsString(data));

        
        if (!data.getRoutes().isEmpty()) {
            Coordinate[] jtsCoords = data.getRoutes().get(0).getCoordinates().stream()
                    .map(c -> new Coordinate(c.getLng(), c.getLat()))
                    .toArray(Coordinate[]::new);
            newRoute.setGeom(geometryFactory.createLineString(jtsCoords));
        }

        routeRepository.save(newRoute);
    }

    private List<RouteResponseDTO.Coordinate> resamplePath(List<RouteResponseDTO.Coordinate> path, double interval) {
        List<RouteResponseDTO.Coordinate> resampled = new ArrayList<>();
        if (path.isEmpty()) return resampled;

        resampled.add(round(path.get(0)));
        double accumulatedDist = 0.0;

        for (int i = 0; i < path.size() - 1; i++) {
            RouteResponseDTO.Coordinate start = path.get(i);
            RouteResponseDTO.Coordinate end = path.get(i + 1);
            double segmentDist = haversine(start.getLat(), start.getLng(), end.getLat(), end.getLng());

            while (accumulatedDist + segmentDist >= interval) {
                double remainingNeeded = interval - accumulatedDist;
                double ratio = remainingNeeded / segmentDist;
                double nextLat = start.getLat() + (end.getLat() - start.getLat()) * ratio;
                double nextLng = start.getLng() + (end.getLng() - start.getLng()) * ratio;
                RouteResponseDTO.Coordinate nextPoint = new RouteResponseDTO.Coordinate(nextLat, nextLng);
                resampled.add(round(nextPoint));
                start = nextPoint;
                segmentDist -= remainingNeeded;
                accumulatedDist = 0.0;
            }
            accumulatedDist += segmentDist;
        }
        return resampled;
    }

    private RouteResponseDTO.Coordinate round(RouteResponseDTO.Coordinate c) {
        double lat = BigDecimal.valueOf(c.getLat()).setScale(6, RoundingMode.HALF_UP).doubleValue();
        double lng = BigDecimal.valueOf(c.getLng()).setScale(6, RoundingMode.HALF_UP).doubleValue();
        return new RouteResponseDTO.Coordinate(lat, lng);
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * R * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}