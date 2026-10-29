
package gameshop_api.model;

import java.math.BigDecimal;

public class GamingStation {

    private int stationId;
    private String stationName;
    private BigDecimal hourlyRate;
    private boolean operational;

    public GamingStation(int stationId, String stationName,
                         BigDecimal hourlyRate, boolean operational) {
        this.stationId = stationId;
        this.stationName = stationName;
        this.hourlyRate = hourlyRate;
        this.operational = operational;
    }

    public int getStationId() {
        return stationId;
    }

    public String getStationName() {
        return stationName;
    }

    public BigDecimal getHourlyRate() {
        return hourlyRate;
    }

    public boolean isOperational() {
        return operational;
    }
}
