package parking.model;

public class ParkingSlot {
    private int slotId;
    private int zoneId;
    private String slotNumber;
    private String slotType;   // Regular, Handicapped, EV, Reserved
    private String status;     // Available, Occupied, Reserved, Maintenance
    // for display joins
    private String zoneName;

    public ParkingSlot() {}

    public ParkingSlot(int slotId, int zoneId, String slotNumber, String slotType, String status) {
        this.slotId     = slotId;
        this.zoneId     = zoneId;
        this.slotNumber = slotNumber;
        this.slotType   = slotType;
        this.status     = status;
    }

    public int    getSlotId()              { return slotId; }
    public void   setSlotId(int v)         { slotId = v; }
    public int    getZoneId()              { return zoneId; }
    public void   setZoneId(int v)         { zoneId = v; }
    public String getSlotNumber()          { return slotNumber; }
    public void   setSlotNumber(String v)  { slotNumber = v; }
    public String getSlotType()            { return slotType; }
    public void   setSlotType(String v)    { slotType = v; }
    public String getStatus()              { return status; }
    public void   setStatus(String v)      { status = v; }
    public String getZoneName()            { return zoneName; }
    public void   setZoneName(String v)    { zoneName = v; }

    @Override
    public String toString() { return slotId + " - " + slotNumber + " [" + status + "]"; }
}
