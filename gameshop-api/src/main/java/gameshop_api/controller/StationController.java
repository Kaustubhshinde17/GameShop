
package gameshop_api.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import gameshop_api.model.GamingStation;
import gameshop_api.service.GamingStationService;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/stations")
public class StationController {

    private final GamingStationService stationService;

    public StationController(GamingStationService stationService) {
        this.stationService = stationService;
    }

    // Get all consoles.
    @GetMapping
    public List<GamingStation> getAllStations() {
        return stationService.getAllStations();
    }

    // Get consoles available for a specific time slot.
    @GetMapping("/availability")
    public List<GamingStation> getAvailableStations(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime startTime,

            @RequestParam int durationHours) {

        return stationService.getAvailableStations(
                startTime, durationHours);
    }
}
