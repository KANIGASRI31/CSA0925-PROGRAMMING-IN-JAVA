package parking.model;

import java.sql.Timestamp;

public class User {
    private int userId;
    private String fullName;
    private String email;
    private String phone;
    private String userType;   // Student, Faculty, Staff, Visitor, Service
    private String idNumber;
    private String address;
    private Timestamp createdAt;

    public User() {}

    public User(int userId, String fullName, String email, String phone,
                String userType, String idNumber, String address, Timestamp createdAt) {
        this.userId    = userId;
        this.fullName  = fullName;
        this.email     = email;
        this.phone     = phone;
        this.userType  = userType;
        this.idNumber  = idNumber;
        this.address   = address;
        this.createdAt = createdAt;
    }

    // Getters & Setters
    public int       getUserId()               { return userId; }
    public void      setUserId(int v)          { userId = v; }
    public String    getFullName()             { return fullName; }
    public void      setFullName(String v)     { fullName = v; }
    public String    getEmail()                { return email; }
    public void      setEmail(String v)        { email = v; }
    public String    getPhone()                { return phone; }
    public void      setPhone(String v)        { phone = v; }
    public String    getUserType()             { return userType; }
    public void      setUserType(String v)     { userType = v; }
    public String    getIdNumber()             { return idNumber; }
    public void      setIdNumber(String v)     { idNumber = v; }
    public String    getAddress()              { return address; }
    public void      setAddress(String v)      { address = v; }
    public Timestamp getCreatedAt()            { return createdAt; }
    public void      setCreatedAt(Timestamp v) { createdAt = v; }

    @Override
    public String toString() { return userId + " - " + fullName + " (" + userType + ")"; }
}
