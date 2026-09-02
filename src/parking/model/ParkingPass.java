package parking.model;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;

public class ParkingPass {
    private int passId;
    private int userId;
    private int vehicleId;
    private int zoneId;
    private String passType;   // Monthly, Semester, Annual
    private Date startDate;
    private Date endDate;
    private BigDecimal passFee;
    private String status;     // Active, Expired, Cancelled
    private Timestamp issuedAt;
    // display joins
    private String userName;
    private String licensePlate;
    private String zoneName;

    public ParkingPass() {}

    public int        getPassId()               { return passId; }
    public void       setPassId(int v)          { passId = v; }
    public int        getUserId()               { return userId; }
    public void       setUserId(int v)          { userId = v; }
    public int        getVehicleId()            { return vehicleId; }
    public void       setVehicleId(int v)       { vehicleId = v; }
    public int        getZoneId()               { return zoneId; }
    public void       setZoneId(int v)          { zoneId = v; }
    public String     getPassType()             { return passType; }
    public void       setPassType(String v)     { passType = v; }
    public Date       getStartDate()            { return startDate; }
    public void       setStartDate(Date v)      { startDate = v; }
    public Date       getEndDate()              { return endDate; }
    public void       setEndDate(Date v)        { endDate = v; }
    public BigDecimal getPassFee()              { return passFee; }
    public void       setPassFee(BigDecimal v)  { passFee = v; }
    public String     getStatus()               { return status; }
    public void       setStatus(String v)       { status = v; }
    public Timestamp  getIssuedAt()             { return issuedAt; }
    public void       setIssuedAt(Timestamp v)  { issuedAt = v; }
    public String     getUserName()             { return userName; }
    public void       setUserName(String v)     { userName = v; }
    public String     getLicensePlate()         { return licensePlate; }
    public void       setLicensePlate(String v) { licensePlate = v; }
    public String     getZoneName()             { return zoneName; }
    public void       setZoneName(String v)     { zoneName = v; }
}
