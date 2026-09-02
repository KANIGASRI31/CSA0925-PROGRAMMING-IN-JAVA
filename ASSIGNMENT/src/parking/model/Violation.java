package parking.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class Violation {
    private int violationId;
    private int vehicleId;
    private Integer slotId;      // nullable
    private String violationType;
    private String description;
    private BigDecimal fineAmount;
    private Timestamp violationDate;
    private String status;       // Pending, Paid, Waived
    // display joins
    private String licensePlate;
    private String slotNumber;

    public Violation() {}

    public int        getViolationId()              { return violationId; }
    public void       setViolationId(int v)         { violationId = v; }
    public int        getVehicleId()                { return vehicleId; }
    public void       setVehicleId(int v)           { vehicleId = v; }
    public Integer    getSlotId()                   { return slotId; }
    public void       setSlotId(Integer v)          { slotId = v; }
    public String     getViolationType()            { return violationType; }
    public void       setViolationType(String v)    { violationType = v; }
    public String     getDescription()              { return description; }
    public void       setDescription(String v)      { description = v; }
    public BigDecimal getFineAmount()               { return fineAmount; }
    public void       setFineAmount(BigDecimal v)   { fineAmount = v; }
    public Timestamp  getViolationDate()            { return violationDate; }
    public void       setViolationDate(Timestamp v) { violationDate = v; }
    public String     getStatus()                   { return status; }
    public void       setStatus(String v)           { status = v; }
    public String     getLicensePlate()             { return licensePlate; }
    public void       setLicensePlate(String v)     { licensePlate = v; }
    public String     getSlotNumber()               { return slotNumber; }
    public void       setSlotNumber(String v)       { slotNumber = v; }
}
