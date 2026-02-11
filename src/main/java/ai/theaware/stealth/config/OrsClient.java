package ai.theaware.stealth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;



@Service
public class OrsClient {
    @Value("${ors.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public String getDirectionsJson(String profile, String startCoords, String endCoords) {
        String url = String.format("https://api.openrouteservice.org/v2/directions/%s?api_key=%s&start=%s&end=%s", 
                                    profile, apiKey, startCoords, endCoords);
        return restTemplate.getForObject(url, String.class);
    }
}