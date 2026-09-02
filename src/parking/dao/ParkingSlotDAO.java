package parking.dao;

import parking.db.DBConnection;
import parking.model.ParkingSlot;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ParkingSlotDAO {

    public boolean addSlot(ParkingSlot s) throws SQLException {
        String sql = "INSERT INTO parking_slots(zone_id,slot_number,slot_type,status) VALUES(?,?,?,?)";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, s.getZoneId());
            ps.setString(2, s.getSlotNumber());
            ps.setString(3, s.getSlotType());
            ps.setString(4, s.getStatus());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean updateSlot(ParkingSlot s) throws SQLException {
        String sql = "UPDATE parking_slots SET zone_id=?,slot_number=?,slot_type=?,status=? WHERE slot_id=?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, s.getZoneId());
            ps.setString(2, s.getSlotNumber());
            ps.setString(3, s.getSlotType());
            ps.setString(4, s.getStatus());
            ps.setInt(5, s.getSlotId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean updateSlotStatus(int slotId, String status) throws SQLException {
        String sql = "UPDATE parking_slots SET status=? WHERE slot_id=?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, slotId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean deleteSlot(int slotId) throws SQLException {
        String sql = "DELETE FROM parking_slots WHERE slot_id=?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, slotId);
            return ps.executeUpdate() > 0;
        }
    }

    public List<ParkingSlot> getAllSlots() throws SQLException {
        List<ParkingSlot> list = new ArrayList<>();
        String sql = "SELECT s.*, z.zone_name FROM parking_slots s JOIN parking_zones z ON s.zone_id=z.zone_id ORDER BY z.zone_name, s.slot_number";
        try (Statement st = DBConnection.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public List<ParkingSlot> getSlotsByZone(int zoneId) throws SQLException {
        List<ParkingSlot> list = new ArrayList<>();
        String sql = "SELECT s.*, z.zone_name FROM parking_slots s JOIN parking_zones z ON s.zone_id=z.zone_id WHERE s.zone_id=? ORDER BY s.slot_number";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, zoneId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    public List<ParkingSlot> getAvailableSlots(int zoneId) throws SQLException {
        List<ParkingSlot> list = new ArrayList<>();
        String sql = "SELECT s.*, z.zone_name FROM parking_slots s JOIN parking_zones z ON s.zone_id=z.zone_id WHERE s.zone_id=? AND s.status='Available' ORDER BY s.slot_number";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, zoneId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    public List<ParkingSlot> getSlotsByStatus(String status) throws SQLException {
        List<ParkingSlot> list = new ArrayList<>();
        String sql = "SELECT s.*, z.zone_name FROM parking_slots s JOIN parking_zones z ON s.zone_id=z.zone_id WHERE s.status=? ORDER BY z.zone_name, s.slot_number";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    public ParkingSlot getSlotById(int slotId) throws SQLException {
        String sql = "SELECT s.*, z.zone_name FROM parking_slots s JOIN parking_zones z ON s.zone_id=z.zone_id WHERE s.slot_id=?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, slotId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        }
        return null;
    }

    private ParkingSlot map(ResultSet rs) throws SQLException {
        ParkingSlot s = new ParkingSlot();
        s.setSlotId(rs.getInt("slot_id"));
        s.setZoneId(rs.getInt("zone_id"));
        s.setSlotNumber(rs.getString("slot_number"));
        s.setSlotType(rs.getString("slot_type"));
        s.setStatus(rs.getString("status"));
        try { s.setZoneName(rs.getString("zone_name")); } catch (SQLException ignored) {}
        return s;
    }
}
