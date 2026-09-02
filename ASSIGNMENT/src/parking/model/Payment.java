package parking.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class Payment {
    private int paymentId;
    private int userId;
    private Integer sessionId;   // nullable
    private Integer passId;      // nullable
    private BigDecimal amount;
    private String paymentMode;  // Cash, Card, UPI, Online
    private Timestamp paymentDate;
    private String status;       // Paid, Pending, Failed
    // display joins
    private String userName;

    public Payment() {}

    public int        getPaymentId()               { return paymentId; }
    public void       setPaymentId(int v)          { paymentId = v; }
    public int        getUserId()                  { return userId; }
    public void       setUserId(int v)             { userId = v; }
    public Integer    getSessionId()               { return sessionId; }
    public void       setSessionId(Integer v)      { sessionId = v; }
    public Integer    getPassId()                  { return passId; }
    public void       setPassId(Integer v)         { passId = v; }
    public BigDecimal getAmount()                  { return amount; }
    public void       setAmount(BigDecimal v)      { amount = v; }
    public String     getPaymentMode()             { return paymentMode; }
    public void       setPaymentMode(String v)     { paymentMode = v; }
    public Timestamp  getPaymentDate()             { return paymentDate; }
    public void       setPaymentDate(Timestamp v)  { paymentDate = v; }
    public String     getStatus()                  { return status; }
    public void       setStatus(String v)          { status = v; }
    public String     getUserName()                { return userName; }
    public void       setUserName(String v)        { userName = v; }
}
