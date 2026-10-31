
package gameshop_api.model;

import java.math.BigDecimal;

public class BookingStation {

    private int stationId;
    private BigDecimal hourlyRate;

    public BookingStation(int stationId, BigDecimal hourlyRate) {
        this.stationId = stationId;
        this.hourlyRate = hourlyRate;
    }

    public int getStationId() {
        return stationId;
    }

    public BigDecimal getHourlyRate() {
        return hourlyRate;
    }
}
