package ai.theaware.stealth.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ai.theaware.stealth.entity.Users;
import ai.theaware.stealth.service.GoogleRoutingService;

@RestController
@RequestMapping("/api/v1/routes")
public class RouteController {

    private final GoogleRoutingService googleRoutingService;

    // Inject only the Service
    public RouteController(GoogleRoutingService googleRoutingService) {
        this.googleRoutingService = googleRoutingService;
    }

    @GetMapping
    public ResponseEntity<?> getRoute(
            @RequestParam Double sLat, @RequestParam Double sLon,
            @RequestParam Double dLat, @RequestParam Double dLon,
            @AuthenticationPrincipal Users user) { // Dynamically injected from JWT Filter

        // Trigger the background task
        googleRoutingService.getSmartRoute(sLat, sLon, dLat, dLon);

        // Return 202 Accepted (Standard for Async tasks)
        return ResponseEntity.accepted().body(Map.of(
            "message", "Route processing initiated asynchronously.",
            "status", "processing"
        ));
    }
}