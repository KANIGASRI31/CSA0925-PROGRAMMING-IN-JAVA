package parking.dao;

import parking.db.DBConnection;
import parking.model.Payment;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PaymentDAO {

    private static final String JOIN_SQL =
        "SELECT p.*, u.full_name AS user_name FROM payments p JOIN users u ON p.user_id=u.user_id ";

    public int addPayment(Payment p) throws SQLException {
        String sql = "INSERT INTO payments(user_id,session_id,pass_id,amount,payment_mode,status) VALUES(?,?,?,?,?,?)";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, p.getUserId());
            if (p.getSessionId() != null) ps.setInt(2, p.getSessionId()); else ps.setNull(2, Types.INTEGER);
            if (p.getPassId()    != null) ps.setInt(3, p.getPassId());    else ps.setNull(3, Types.INTEGER);
            ps.setBigDecimal(4, p.getAmount());
            ps.setString(5, p.getPaymentMode());
            ps.setString(6, p.getStatus() != null ? p.getStatus() : "Paid");
            ps.executeUpdate();
            try (ResultSet gk = ps.getGeneratedKeys()) {
                if (gk.next()) return gk.getInt(1);
            }
        }
        return -1;
    }

    public boolean updatePaymentStatus(int paymentId, String status) throws SQLException {
        String sql = "UPDATE payments SET status=? WHERE payment_id=?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, paymentId);
            return ps.executeUpdate() > 0;
        }
    }

    public List<Payment> getAllPayments() throws SQLException {
        List<Payment> list = new ArrayList<>();
        try (Statement st = DBConnection.getConnection().createStatement();
             ResultSet rs = st.executeQuery(JOIN_SQL + "ORDER BY p.payment_date DESC")) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public List<Payment> getPaymentsByUser(int userId) throws SQLException {
        List<Payment> list = new ArrayList<>();
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(JOIN_SQL + "WHERE p.user_id=? ORDER BY p.payment_date DESC")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    /** Total revenue grouped by date – used in reports */
    public ResultSet getDailyRevenue() throws SQLException {
        String sql = "SELECT DATE(payment_date) AS pay_date, SUM(amount) AS total FROM payments WHERE status='Paid' GROUP BY DATE(payment_date) ORDER BY pay_date DESC";
        return DBConnection.getConnection().createStatement().executeQuery(sql);
    }

    private Payment map(ResultSet rs) throws SQLException {
        Payment p = new Payment();
        p.setPaymentId(rs.getInt("payment_id"));
        p.setUserId(rs.getInt("user_id"));
        int sid = rs.getInt("session_id"); p.setSessionId(rs.wasNull() ? null : sid);
        int pid = rs.getInt("pass_id");    p.setPassId(rs.wasNull() ? null : pid);
        p.setAmount(rs.getBigDecimal("amount"));
        p.setPaymentMode(rs.getString("payment_mode"));
        p.setPaymentDate(rs.getTimestamp("payment_date"));
        p.setStatus(rs.getString("status"));
        try { p.setUserName(rs.getString("user_name")); } catch (SQLException ignored) {}
        return p;
    }
}
