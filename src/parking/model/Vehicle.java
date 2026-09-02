package parking.model;

import java.sql.Timestamp;

public class Vehicle {
    private int vehicleId;
    private int userId;
    private String licensePlate;
    private String vehicleType;   // Car, Motorcycle, Truck, Bus, Bicycle, Other
    private String make;
    private String model;
    private String color;
    private Timestamp registeredAt;
    // for display joins
    private String ownerName;

    public Vehicle() {}

    public Vehicle(int vehicleId, int userId, String licensePlate, String vehicleType,
                   String make, String model, String color, Timestamp registeredAt) {
        this.vehicleId    = vehicleId;
        this.userId       = userId;
        this.licensePlate = licensePlate;
        this.vehicleType  = vehicleType;
        this.make         = make;
        this.model        = model;
        this.color        = color;
        this.registeredAt = registeredAt;
    }

    public int       getVehicleId()               { return vehicleId; }
    public void      setVehicleId(int v)          { vehicleId = v; }
    public int       getUserId()                  { return userId; }
    public void      setUserId(int v)             { userId = v; }
    public String    getLicensePlate()            { return licensePlate; }
    public void      setLicensePlate(String v)    { licensePlate = v; }
    public String    getVehicleType()             { return vehicleType; }
    public void      setVehicleType(String v)     { vehicleType = v; }
    public String    getMake()                    { return make; }
    public void      setMake(String v)            { make = v; }
    public String    getModel()                   { return model; }
    public void      setModel(String v)           { model = v; }
    public String    getColor()                   { return color; }
    public void      setColor(String v)           { color = v; }
    public Timestamp getRegisteredAt()            { return registeredAt; }
    public void      setRegisteredAt(Timestamp v) { registeredAt = v; }
    public String    getOwnerName()               { return ownerName; }
    public void      setOwnerName(String v)       { ownerName = v; }

    @Override
    public String toString() { return vehicleId + " - " + licensePlate + " (" + vehicleType + ")"; }
}
