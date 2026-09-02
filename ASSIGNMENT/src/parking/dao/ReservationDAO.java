package parking.dao;

import parking.db.DBConnection;
import parking.model.Reservation;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReservationDAO {

    private static final String JOIN_SQL =
        "SELECT r.*, u.full_name AS user_name, v.license_plate, s.slot_number, z.zone_name " +
        "FROM reservations r " +
        "JOIN users u ON r.user_id=u.user_id " +
        "JOIN vehicles v ON r.vehicle_id=v.vehicle_id " +
        "JOIN parking_slots s ON r.slot_id=s.slot_id " +
        "JOIN parking_zones z ON s.zone_id=z.zone_id ";

    public int addReservation(Reservation r) throws SQLException {
        // Check for conflicting active reservation on the same slot
        String checkSql = "SELECT COUNT(*) FROM reservations WHERE slot_id=? AND status='Active' AND NOT (reserved_until<=? OR reserved_from>=?)";
        try (PreparedStatement chk = DBConnection.getConnection().prepareStatement(checkSql)) {
            chk.setInt(1, r.getSlotId());
            chk.setTimestamp(2, r.getReservedFrom());
            chk.setTimestamp(3, r.getReservedUntil());
            try (ResultSet rs = chk.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0)
                    throw new SQLException("Conflicting reservation exists for this slot in the chosen time window.");
            }
        }
        String sql = "INSERT INTO reservations(user_id,vehicle_id,slot_id,reserved_from,reserved_until,status) VALUES(?,?,?,?,?,'Active')";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, r.getUserId());
            ps.setInt(2, r.getVehicleId());
            ps.setInt(3, r.getSlotId());
            ps.setTimestamp(4, r.getReservedFrom());
            ps.setTimestamp(5, r.getReservedUntil());
            ps.executeUpdate();
            // Mark slot as Reserved
            new ParkingSlotDAO().updateSlotStatus(r.getSlotId(), "Reserved");
            try (ResultSet gk = ps.getGeneratedKeys()) {
                if (gk.next()) return gk.getInt(1);
            }
        }
        return -1;
    }

    public boolean cancelReservation(int reservationId) throws SQLException {
        // fetch slot first
        Reservation r = getReservationById(reservationId);
        String sql = "UPDATE reservations SET status='Cancelled' WHERE reservation_id=?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, reservationId);
            boolean ok = ps.executeUpdate() > 0;
            if (ok && r != null)
                new ParkingSlotDAO().updateSlotStatus(r.getSlotId(), "Available");
            return ok;
        }
    }

    public boolean updateStatus(int reservationId, String status) throws SQLException {
        String sql = "UPDATE reservations SET status=? WHERE reservation_id=?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, reservationId);
            return ps.executeUpdate() > 0;
        }
    }

    public List<Reservation> getAllReservations() throws SQLException {
        List<Reservation> list = new ArrayList<>();
        try (Statement st = DBConnection.getConnection().createStatement();
             ResultSet rs = st.executeQuery(JOIN_SQL + "ORDER BY r.created_at DESC")) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public Reservation getReservationById(int id) throws SQLException {
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(JOIN_SQL + "WHERE r.reservation_id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        }
        return null;
    }

    public List<Reservation> getActiveReservationsBySlot(int slotId) throws SQLException {
        List<Reservation> list = new ArrayList<>();
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(JOIN_SQL + "WHERE r.slot_id=? AND r.status='Active'")) {
            ps.setInt(1, slotId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    private Reservation map(ResultSet rs) throws SQLException {
        Reservation r = new Reservation();
        r.setReservationId(rs.getInt("reservation_id"));
        r.setUserId(rs.getInt("user_id"));
        r.setVehicleId(rs.getInt("vehicle_id"));
        r.setSlotId(rs.getInt("slot_id"));
        r.setReservedFrom(rs.getTimestamp("reserved_from"));
        r.setReservedUntil(rs.getTimestamp("reserved_until"));
        r.setStatus(rs.getString("status"));
        r.setCreatedAt(rs.getTimestamp("created_at"));
        try { r.setUserName(rs.getString("user_name")); }    catch (SQLException ignored) {}
        try { r.setLicensePlate(rs.getString("license_plate")); } catch (SQLException ignored) {}
        try { r.setSlotNumber(rs.getString("slot_number")); }  catch (SQLException ignored) {}
        try { r.setZoneName(rs.getString("zone_name")); }     catch (SQLException ignored) {}
        return r;
    }
}
