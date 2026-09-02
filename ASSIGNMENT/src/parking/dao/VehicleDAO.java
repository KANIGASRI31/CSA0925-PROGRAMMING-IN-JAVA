package parking.dao;

import parking.db.DBConnection;
import parking.model.Vehicle;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class VehicleDAO {

    public boolean addVehicle(Vehicle v) throws SQLException {
        String sql = "INSERT INTO vehicles(user_id,license_plate,vehicle_type,make,model,color) VALUES(?,?,?,?,?,?)";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, v.getUserId());
            ps.setString(2, v.getLicensePlate());
            ps.setString(3, v.getVehicleType());
            ps.setString(4, v.getMake());
            ps.setString(5, v.getModel());
            ps.setString(6, v.getColor());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean updateVehicle(Vehicle v) throws SQLException {
        String sql = "UPDATE vehicles SET user_id=?,license_plate=?,vehicle_type=?,make=?,model=?,color=? WHERE vehicle_id=?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, v.getUserId());
            ps.setString(2, v.getLicensePlate());
            ps.setString(3, v.getVehicleType());
            ps.setString(4, v.getMake());
            ps.setString(5, v.getModel());
            ps.setString(6, v.getColor());
            ps.setInt(7, v.getVehicleId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean deleteVehicle(int vehicleId) throws SQLException {
        String sql = "DELETE FROM vehicles WHERE vehicle_id=?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, vehicleId);
            return ps.executeUpdate() > 0;
        }
    }

    public List<Vehicle> getAllVehicles() throws SQLException {
        List<Vehicle> list = new ArrayList<>();
        String sql = "SELECT v.*, u.full_name AS owner_name FROM vehicles v JOIN users u ON v.user_id=u.user_id ORDER BY v.license_plate";
        try (Statement st = DBConnection.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public Vehicle getVehicleById(int vehicleId) throws SQLException {
        String sql = "SELECT v.*, u.full_name AS owner_name FROM vehicles v JOIN users u ON v.user_id=u.user_id WHERE v.vehicle_id=?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, vehicleId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        }
        return null;
    }

    public Vehicle getVehicleByPlate(String plate) throws SQLException {
        String sql = "SELECT v.*, u.full_name AS owner_name FROM vehicles v JOIN users u ON v.user_id=u.user_id WHERE v.license_plate=?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, plate);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        }
        return null;
    }

    public List<Vehicle> getVehiclesByUser(int userId) throws SQLException {
        List<Vehicle> list = new ArrayList<>();
        String sql = "SELECT v.*, u.full_name AS owner_name FROM vehicles v JOIN users u ON v.user_id=u.user_id WHERE v.user_id=?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    public List<Vehicle> searchVehicles(String keyword) throws SQLException {
        List<Vehicle> list = new ArrayList<>();
        String sql = "SELECT v.*, u.full_name AS owner_name FROM vehicles v JOIN users u ON v.user_id=u.user_id WHERE v.license_plate LIKE ? OR v.make LIKE ? OR v.model LIKE ? OR v.color LIKE ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            String k = "%" + keyword + "%";
            ps.setString(1, k); ps.setString(2, k);
            ps.setString(3, k); ps.setString(4, k);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    private Vehicle map(ResultSet rs) throws SQLException {
        Vehicle v = new Vehicle();
        v.setVehicleId(rs.getInt("vehicle_id"));
        v.setUserId(rs.getInt("user_id"));
        v.setLicensePlate(rs.getString("license_plate"));
        v.setVehicleType(rs.getString("vehicle_type"));
        v.setMake(rs.getString("make"));
        v.setModel(rs.getString("model"));
        v.setColor(rs.getString("color"));
        v.setRegisteredAt(rs.getTimestamp("registered_at"));
        try { v.setOwnerName(rs.getString("owner_name")); } catch (SQLException ignored) {}
        return v;
    }
}
