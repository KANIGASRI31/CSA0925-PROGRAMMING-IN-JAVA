package parking.model;

import java.math.BigDecimal;

public class ParkingZone {
    private int zoneId;
    private String zoneName;
    private String zoneType;      // Student, Faculty, Staff, Visitor, Service, General
    private String location;
    private int totalSlots;
    private BigDecimal hourlyRate;
    private String description;
    // computed at runtime
    private int availableSlots;

    public ParkingZone() {}

    public ParkingZone(int zoneId, String zoneName, String zoneType, String location,
                       int totalSlots, BigDecimal hourlyRate, String description) {
        this.zoneId      = zoneId;
        this.zoneName    = zoneName;
        this.zoneType    = zoneType;
        this.location    = location;
        this.totalSlots  = totalSlots;
        this.hourlyRate  = hourlyRate;
        this.description = description;
    }

    public int        getZoneId()               { return zoneId; }
    public void       setZoneId(int v)          { zoneId = v; }
    public String     getZoneName()             { return zoneName; }
    public void       setZoneName(String v)     { zoneName = v; }
    public String     getZoneType()             { return zoneType; }
    public void       setZoneType(String v)     { zoneType = v; }
    public String     getLocation()             { return location; }
    public void       setLocation(String v)     { location = v; }
    public int        getTotalSlots()           { return totalSlots; }
    public void       setTotalSlots(int v)      { totalSlots = v; }
    public BigDecimal getHourlyRate()           { return hourlyRate; }
    public void       setHourlyRate(BigDecimal v){ hourlyRate = v; }
    public String     getDescription()          { return description; }
    public void       setDescription(String v)  { description = v; }
    public int        getAvailableSlots()       { return availableSlots; }
    public void       setAvailableSlots(int v)  { availableSlots = v; }

    @Override
    public String toString() { return zoneId + " - " + zoneName + " (" + zoneType + ")"; }
}
