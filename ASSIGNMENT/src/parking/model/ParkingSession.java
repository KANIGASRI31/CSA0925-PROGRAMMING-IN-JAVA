package parking.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class ParkingSession {
    private int sessionId;
    private int vehicleId;
    private int slotId;
    private int userId;
    private Timestamp entryTime;
    private Timestamp exitTime;
    private BigDecimal durationHrs;
    private BigDecimal feeAmount;
    private String status;   // Active, Completed
    // display joins
    private String licensePlate;
    private String slotNumber;
    private String zoneName;
    private String userName;

    public ParkingSession() {}

    public int        getSessionId()               { return sessionId; }
    public void       setSessionId(int v)          { sessionId = v; }
    public int        getVehicleId()               { return vehicleId; }
    public void       setVehicleId(int v)          { vehicleId = v; }
    public int        getSlotId()                  { return slotId; }
    public void       setSlotId(int v)             { slotId = v; }
    public int        getUserId()                  { return userId; }
    public void       setUserId(int v)             { userId = v; }
    public Timestamp  getEntryTime()               { return entryTime; }
    public void       setEntryTime(Timestamp v)    { entryTime = v; }
    public Timestamp  getExitTime()                { return exitTime; }
    public void       setExitTime(Timestamp v)     { exitTime = v; }
    public BigDecimal getDurationHrs()             { return durationHrs; }
    public void       setDurationHrs(BigDecimal v) { durationHrs = v; }
    public BigDecimal getFeeAmount()               { return feeAmount; }
    public void       setFeeAmount(BigDecimal v)   { feeAmount = v; }
    public String     getStatus()                  { return status; }
    public void       setStatus(String v)          { status = v; }
    public String     getLicensePlate()            { return licensePlate; }
    public void       setLicensePlate(String v)    { licensePlate = v; }
    public String     getSlotNumber()              { return slotNumber; }
    public void       setSlotNumber(String v)      { slotNumber = v; }
    public String     getZoneName()                { return zoneName; }
    public void       setZoneName(String v)        { zoneName = v; }
    public String     getUserName()                { return userName; }
    public void       setUserName(String v)        { userName = v; }
}
