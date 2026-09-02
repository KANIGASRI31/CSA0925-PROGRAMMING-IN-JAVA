package parking.dao;

import parking.db.DBConnection;
import parking.model.ParkingZone;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ParkingZoneDAO {

    public boolean addZone(ParkingZone z) throws SQLException {
        String sql = "INSERT INTO parking_zones(zone_name,zone_type,location,total_slots,hourly_rate,description) VALUES(?,?,?,?,?,?)";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, z.getZoneName());
            ps.setString(2, z.getZoneType());
            ps.setString(3, z.getLocation());
            ps.setInt(4, z.getTotalSlots());
            ps.setBigDecimal(5, z.getHourlyRate());
            ps.setString(6, z.getDescription());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean updateZone(ParkingZone z) throws SQLException {
        String sql = "UPDATE parking_zones SET zone_name=?,zone_type=?,location=?,total_slots=?,hourly_rate=?,description=? WHERE zone_id=?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, z.getZoneName());
            ps.setString(2, z.getZoneType());
            ps.setString(3, z.getLocation());
            ps.setInt(4, z.getTotalSlots());
            ps.setBigDecimal(5, z.getHourlyRate());
            ps.setString(6, z.getDescription());
            ps.setInt(7, z.getZoneId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean deleteZone(int zoneId) throws SQLException {
        String sql = "DELETE FROM parking_zones WHERE zone_id=?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, zoneId);
            return ps.executeUpdate() > 0;
        }
    }

    public List<ParkingZone> getAllZones() throws SQLException {
        List<ParkingZone> list = new ArrayList<>();
        String sql = "SELECT z.*, (SELECT COUNT(*) FROM parking_slots s WHERE s.zone_id=z.zone_id AND s.status='Available') AS available_slots FROM parking_zones z ORDER BY z.zone_name";
        try (Statement st = DBConnection.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs, true));
        }
        return list;
    }

    public ParkingZone getZoneById(int zoneId) throws SQLException {
        String sql = "SELECT z.*, (SELECT COUNT(*) FROM parking_slots s WHERE s.zone_id=z.zone_id AND s.status='Available') AS available_slots FROM parking_zones z WHERE z.zone_id=?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, zoneId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs, true);
            }
        }
        return null;
    }

    private ParkingZone map(ResultSet rs, boolean withAvailable) throws SQLException {
        ParkingZone z = new ParkingZone();
        z.setZoneId(rs.getInt("zone_id"));
        z.setZoneName(rs.getString("zone_name"));
        z.setZoneType(rs.getString("zone_type"));
        z.setLocation(rs.getString("location"));
        z.setTotalSlots(rs.getInt("total_slots"));
        z.setHourlyRate(rs.getBigDecimal("hourly_rate"));
        z.setDescription(rs.getString("description"));
        if (withAvailable) {
            try { z.setAvailableSlots(rs.getInt("available_slots")); } catch (SQLException ignored) {}
        }
        return z;
    }
}
