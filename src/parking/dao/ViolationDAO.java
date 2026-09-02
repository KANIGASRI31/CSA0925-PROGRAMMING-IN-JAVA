package parking.dao;

import parking.db.DBConnection;
import parking.model.Violation;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ViolationDAO {

    private static final String JOIN_SQL =
        "SELECT vl.*, v.license_plate, s.slot_number " +
        "FROM violations vl " +
        "JOIN vehicles v ON vl.vehicle_id=v.vehicle_id " +
        "LEFT JOIN parking_slots s ON vl.slot_id=s.slot_id ";

    public boolean addViolation(Violation viol) throws SQLException {
        String sql = "INSERT INTO violations(vehicle_id,slot_id,violation_type,description,fine_amount,status) VALUES(?,?,?,?,?,'Pending')";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, viol.getVehicleId());
            if (viol.getSlotId() != null) ps.setInt(2, viol.getSlotId()); else ps.setNull(2, Types.INTEGER);
            ps.setString(3, viol.getViolationType());
            ps.setString(4, viol.getDescription());
            ps.setBigDecimal(5, viol.getFineAmount());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean updateViolationStatus(int violationId, String status) throws SQLException {
        String sql = "UPDATE violations SET status=? WHERE violation_id=?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, violationId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean deleteViolation(int violationId) throws SQLException {
        String sql = "DELETE FROM violations WHERE violation_id=?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, violationId);
            return ps.executeUpdate() > 0;
        }
    }

    public List<Violation> getAllViolations() throws SQLException {
        List<Violation> list = new ArrayList<>();
        try (Statement st = DBConnection.getConnection().createStatement();
             ResultSet rs = st.executeQuery(JOIN_SQL + "ORDER BY vl.violation_date DESC")) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public List<Violation> getPendingViolations() throws SQLException {
        List<Violation> list = new ArrayList<>();
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(JOIN_SQL + "WHERE vl.status='Pending' ORDER BY vl.violation_date DESC")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    public List<Violation> getViolationsByVehicle(int vehicleId) throws SQLException {
        List<Violation> list = new ArrayList<>();
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(JOIN_SQL + "WHERE vl.vehicle_id=? ORDER BY vl.violation_date DESC")) {
            ps.setInt(1, vehicleId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    public List<Violation> searchViolations(String keyword) throws SQLException {
        List<Violation> list = new ArrayList<>();
        String sql = JOIN_SQL + "WHERE v.license_plate LIKE ? OR vl.violation_type LIKE ? ORDER BY vl.violation_date DESC";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            String k = "%" + keyword + "%";
            ps.setString(1, k); ps.setString(2, k);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    private Violation map(ResultSet rs) throws SQLException {
        Violation v = new Violation();
        v.setViolationId(rs.getInt("violation_id"));
        v.setVehicleId(rs.getInt("vehicle_id"));
        int sid = rs.getInt("slot_id"); v.setSlotId(rs.wasNull() ? null : sid);
        v.setViolationType(rs.getString("violation_type"));
        v.setDescription(rs.getString("description"));
        v.setFineAmount(rs.getBigDecimal("fine_amount"));
        v.setViolationDate(rs.getTimestamp("violation_date"));
        v.setStatus(rs.getString("status"));
        try { v.setLicensePlate(rs.getString("license_plate")); } catch (SQLException ignored) {}
        try { v.setSlotNumber(rs.getString("slot_number")); }     catch (SQLException ignored) {}
        return v;
    }
}
