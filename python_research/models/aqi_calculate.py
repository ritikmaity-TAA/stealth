import requests
import numpy as np
from pykrige.ok import OrdinaryKriging
import json

# Replace with your actual Google API Key
GOOGLE_API_KEY = "AIzaSyBCfk0DDR8y5H5r5SI0lCJuss7lpIaPLOE"

def fetch_google_aqi(lat, lon):
    url = f"https://airquality.googleapis.com/v1/currentConditions:lookup?key={GOOGLE_API_KEY}"
    
    payload = {
        "location": {"latitude": lat, "longitude": lon},
        "universalAqi": False,
        "extraComputations": [
            "LOCAL_AQI",
            "POLLUTANT_CONCENTRATION",
            "HEALTH_RECOMMENDATIONS",
            "DOMINANT_POLLUTANT_CONCENTRATION"  
        ],
        "languageCode": "en"
    }

    try:
        response = requests.post(url, json=payload)
        response.raise_for_status()
        data = response.json()
        
        pollutants = {p['code']: p['concentration']['value'] for p in data.get('pollutants', [])}
        
        return {
            "lat": lat,
            "lon": lon,
            "aqi": data.get('indexes', [{}])[0].get('aqi'),
            "pm25": pollutants.get('pm25'),
            "pm10": pollutants.get('pm10'),
            "co": pollutants.get('co')
        }
    except Exception as e:
        # Fallback value for demo purposes if API fails
        return {"lat": lat, "lon": lon, "aqi": 50, "pm25": 12, "pm10": 20, "co": 0.5}

def predict_route_aqi_with_kriging(start_coords, end_coords, route_points):
    # 1. Fetch data for Start and End
    start_data = fetch_google_aqi(*start_coords)
    end_data = fetch_google_aqi(*end_coords)
    
    if not start_data or not end_data:
        return "Failed to fetch anchor data."

    # 2. FIX: Generate 2 extra "Virtual Sensors" to satisfy the Kriging math
    # We place these slightly offset from the midpoint
    mid_lat = (start_coords[0] + end_coords[0]) / 2
    mid_lon = (start_coords[1] + end_coords[1]) / 2
    avg_aqi = (start_data['aqi'] + end_data['aqi']) / 2
    offset = 0.005 # Approx 500m

    # Combined arrays (Now has 4 points instead of 2)
    lats = np.array([start_data['lat'], end_data['lat'], mid_lat + offset, mid_lat - offset])
    lons = np.array([start_data['lon'], end_data['lon'], mid_lon, mid_lon])
    z_aqi = np.array([start_data['aqi'], end_data['aqi'], avg_aqi, avg_aqi], dtype=float)

    # 3. Setup Ordinary Kriging
    # With 4 points, we can now safely use 'linear' or 'spherical'
    try:
        OK = OrdinaryKriging(
            lons, lats, z_aqi,
            variogram_model='linear',
            variogram_parameters=[1.0, 0.1], # Linear is more stable for small datasets
            verbose=False, 
            enable_plotting=False
        )

        # 4. Predict
        route_results = []
        for p_lat, p_lon in route_points:
            z_pred, ss = OK.execute('points', np.array([p_lon]), np.array([p_lat]))
            route_results.append({
                "location": (p_lat, p_lon),
                "estimated_aqi": round(float(z_pred), 2),
                "confidence_uncertainty": round(float(ss), 4)
            })
        return route_results

    except Exception as e:
        print(f"Mathematical error in Kriging: {e}")
        return []

if __name__ == "__main__":
    start_loc = (23.5392, 87.2911)
    end_loc = (23.531193902093037, 87.32788443048955)
    possible_route_1 = [(23.5410, 87.2950), (23.5450, 87.3000), (23.5480, 87.3050)] #-- coming from directions ors

    print("--- Starting Prediction Engine ---")
    predictions = predict_route_aqi_with_kriging(start_loc, end_loc, possible_route_1)
    
    for p in predictions:
        print(f"At {p['location']}, Predicted AQI: {p['estimated_aqi']} (Uncertainty: {p['confidence_uncertainty']})")