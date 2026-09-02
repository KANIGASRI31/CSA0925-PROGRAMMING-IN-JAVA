package parking.dao;

import parking.db.DBConnection;
import parking.model.ParkingSession;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * SessionDAO – uses CallableStatement for entry/exit stored procedures
 * and PreparedStatement for queries.
 */
public class SessionDAO {

    private static final String JOIN_SQL =
        "SELECT ps.*, v.license_plate, sl.slot_number, z.zone_name, u.full_name AS user_name " +
        "FROM parking_sessions ps " +
        "JOIN vehicles v  ON ps.vehicle_id=v.vehicle_id " +
        "JOIN parking_slots sl ON ps.slot_id=sl.slot_id " +
        "JOIN parking_zones z  ON sl.zone_id=z.zone_id " +
        "JOIN users u ON ps.user_id=u.user_id ";

    // ── ENTRY via CallableStatement ──────────────────────────────────────────
    public int recordEntry(int vehicleId, int slotId, int userId) throws SQLException {
        try (CallableStatement cs = DBConnection.getConnection().prepareCall("{CALL sp_vehicle_entry(?,?,?,?)}")) {
            cs.setInt(1, vehicleId);
            cs.setInt(2, slotId);
            cs.setInt(3, userId);
            cs.registerOutParameter(4, Types.INTEGER);
            cs.execute();
            return cs.getInt(4);
        }
    }

    // ── EXIT via CallableStatement ───────────────────────────────────────────
    public BigDecimal recordExit(int sessionId) throws SQLException {
        try (CallableStatement cs = DBConnection.getConnection().prepareCall("{CALL sp_vehicle_exit(?,?)}")) {
            cs.setInt(1, sessionId);
            cs.registerOutParameter(2, Types.DECIMAL);
            cs.execute();
            return cs.getBigDecimal(2);
        }
    }

    // ── Active session by license plate ─────────────────────────────────────
    public ParkingSession getActiveSessionByPlate(String plate) throws SQLException {
        String sql = JOIN_SQL + "WHERE v.license_plate=? AND ps.status='Active'";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, plate);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        }
        return null;
    }

    public ParkingSession getSessionById(int sessionId) throws SQLException {
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(JOIN_SQL + "WHERE ps.session_id=?")) {
            ps.setInt(1, sessionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        }
        return null;
    }

    public List<ParkingSession> getActiveSessions() throws SQLException {
        List<ParkingSession> list = new ArrayList<>();
        try (Statement st = DBConnection.getConnection().createStatement();
             ResultSet rs = st.executeQuery(JOIN_SQL + "WHERE ps.status='Active' ORDER BY ps.entry_time")) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public List<ParkingSession> getAllSessions() throws SQLException {
        List<ParkingSession> list = new ArrayList<>();
        try (Statement st = DBConnection.getConnection().createStatement();
             ResultSet rs = st.executeQuery(JOIN_SQL + "ORDER BY ps.entry_time DESC")) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public List<ParkingSession> getSessionsByVehicle(int vehicleId) throws SQLException {
        List<ParkingSession> list = new ArrayList<>();
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(JOIN_SQL + "WHERE ps.vehicle_id=? ORDER BY ps.entry_time DESC")) {
            ps.setInt(1, vehicleId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    private ParkingSession map(ResultSet rs) throws SQLException {
        ParkingSession s = new ParkingSession();
        s.setSessionId(rs.getInt("session_id"));
        s.setVehicleId(rs.getInt("vehicle_id"));
        s.setSlotId(rs.getInt("slot_id"));
        s.setUserId(rs.getInt("user_id"));
        s.setEntryTime(rs.getTimestamp("entry_time"));
        s.setExitTime(rs.getTimestamp("exit_time"));
        s.setDurationHrs(rs.getBigDecimal("duration_hrs"));
        s.setFeeAmount(rs.getBigDecimal("fee_amount"));
        s.setStatus(rs.getString("status"));
        try { s.setLicensePlate(rs.getString("license_plate")); } catch (SQLException ignored) {}
        try { s.setSlotNumber(rs.getString("slot_number")); }     catch (SQLException ignored) {}
        try { s.setZoneName(rs.getString("zone_name")); }         catch (SQLException ignored) {}
        try { s.setUserName(rs.getString("user_name")); }         catch (SQLException ignored) {}
        return s;
    }
}
