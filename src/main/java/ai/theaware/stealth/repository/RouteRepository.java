package ai.theaware.stealth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ai.theaware.stealth.entity.Route;

public interface RouteRepository extends JpaRepository<Route, Long> {

    @Query(value = "SELECT * FROM routes r WHERE " +
           "ST_DWithin(ST_SetSRID(ST_Point(:sLon, :sLat), 4326), ST_SetSRID(ST_Point(r.start_lon, r.start_lat), 4326), 0.0005) AND " +
           "ST_DWithin(ST_SetSRID(ST_Point(:dLon, :dLat), 4326), ST_SetSRID(ST_Point(r.end_lon, r.end_lat), 4326), 0.0005) " +
           "LIMIT 1", nativeQuery = true)
    Optional<Route> findCachedRoute(@Param("sLat") Double sLat, @Param("sLon") Double sLon, 
                                    @Param("dLat") Double dLat, @Param("dLon") Double dLon);
}