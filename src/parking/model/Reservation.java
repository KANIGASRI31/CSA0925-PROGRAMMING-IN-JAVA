package parking.model;

import java.sql.Timestamp;

public class Reservation {
    private int reservationId;
    private int userId;
    private int vehicleId;
    private int slotId;
    private Timestamp reservedFrom;
    private Timestamp reservedUntil;
    private String status;   // Active, Completed, Cancelled, Expired
    private Timestamp createdAt;
    // display joins
    private String userName;
    private String licensePlate;
    private String slotNumber;
    private String zoneName;

    public Reservation() {}

    public int       getReservationId()              { return reservationId; }
    public void      setReservationId(int v)         { reservationId = v; }
    public int       getUserId()                     { return userId; }
    public void      setUserId(int v)                { userId = v; }
    public int       getVehicleId()                  { return vehicleId; }
    public void      setVehicleId(int v)             { vehicleId = v; }
    public int       getSlotId()                     { return slotId; }
    public void      setSlotId(int v)                { slotId = v; }
    public Timestamp getReservedFrom()               { return reservedFrom; }
    public void      setReservedFrom(Timestamp v)    { reservedFrom = v; }
    public Timestamp getReservedUntil()              { return reservedUntil; }
    public void      setReservedUntil(Timestamp v)   { reservedUntil = v; }
    public String    getStatus()                     { return status; }
    public void      setStatus(String v)             { status = v; }
    public Timestamp getCreatedAt()                  { return createdAt; }
    public void      setCreatedAt(Timestamp v)       { createdAt = v; }
    public String    getUserName()                   { return userName; }
    public void      setUserName(String v)           { userName = v; }
    public String    getLicensePlate()               { return licensePlate; }
    public void      setLicensePlate(String v)       { licensePlate = v; }
    public String    getSlotNumber()                 { return slotNumber; }
    public void      setSlotNumber(String v)         { slotNumber = v; }
    public String    getZoneName()                   { return zoneName; }
    public void      setZoneName(String v)           { zoneName = v; }
}
