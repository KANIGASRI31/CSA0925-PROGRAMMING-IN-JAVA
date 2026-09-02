package parking.dao;

import parking.db.DBConnection;
import parking.model.ParkingPass;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PassDAO {

    private static final String JOIN_SQL =
        "SELECT pp.*, u.full_name AS user_name, v.license_plate, z.zone_name " +
        "FROM parking_passes pp " +
        "JOIN users u ON pp.user_id=u.user_id " +
        "JOIN vehicles v ON pp.vehicle_id=v.vehicle_id " +
        "JOIN parking_zones z ON pp.zone_id=z.zone_id ";

    public int addPass(ParkingPass p) throws SQLException {
        String sql = "INSERT INTO parking_passes(user_id,vehicle_id,zone_id,pass_type,start_date,end_date,pass_fee,status) VALUES(?,?,?,?,?,?,?,'Active')";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, p.getUserId());
            ps.setInt(2, p.getVehicleId());
            ps.setInt(3, p.getZoneId());
            ps.setString(4, p.getPassType());
            ps.setDate(5, p.getStartDate());
            ps.setDate(6, p.getEndDate());
            ps.setBigDecimal(7, p.getPassFee());
            ps.executeUpdate();
            try (ResultSet gk = ps.getGeneratedKeys()) {
                if (gk.next()) return gk.getInt(1);
            }
        }
        return -1;
    }

    public boolean updatePassStatus(int passId, String status) throws SQLException {
        String sql = "UPDATE parking_passes SET status=? WHERE pass_id=?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, passId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean deletePass(int passId) throws SQLException {
        String sql = "DELETE FROM parking_passes WHERE pass_id=?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, passId);
            return ps.executeUpdate() > 0;
        }
    }

    public List<ParkingPass> getAllPasses() throws SQLException {
        List<ParkingPass> list = new ArrayList<>();
        try (Statement st = DBConnection.getConnection().createStatement();
             ResultSet rs = st.executeQuery(JOIN_SQL + "ORDER BY pp.issued_at DESC")) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public ParkingPass getPassById(int passId) throws SQLException {
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(JOIN_SQL + "WHERE pp.pass_id=?")) {
            ps.setInt(1, passId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        }
        return null;
    }

    public List<ParkingPass> getPassesByUser(int userId) throws SQLException {
        List<ParkingPass> list = new ArrayList<>();
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(JOIN_SQL + "WHERE pp.user_id=? ORDER BY pp.issued_at DESC")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    /** Expire passes whose end_date < today */
    public void expireOldPasses() throws SQLException {
        String sql = "UPDATE parking_passes SET status='Expired' WHERE end_date < CURDATE() AND status='Active'";
        try (Statement st = DBConnection.getConnection().createStatement()) {
            st.executeUpdate(sql);
        }
    }

    private ParkingPass map(ResultSet rs) throws SQLException {
        ParkingPass p = new ParkingPass();
        p.setPassId(rs.getInt("pass_id"));
        p.setUserId(rs.getInt("user_id"));
        p.setVehicleId(rs.getInt("vehicle_id"));
        p.setZoneId(rs.getInt("zone_id"));
        p.setPassType(rs.getString("pass_type"));
        p.setStartDate(rs.getDate("start_date"));
        p.setEndDate(rs.getDate("end_date"));
        p.setPassFee(rs.getBigDecimal("pass_fee"));
        p.setStatus(rs.getString("status"));
        p.setIssuedAt(rs.getTimestamp("issued_at"));
        try { p.setUserName(rs.getString("user_name")); }    catch (SQLException ignored) {}
        try { p.setLicensePlate(rs.getString("license_plate")); } catch (SQLException ignored) {}
        try { p.setZoneName(rs.getString("zone_name")); }    catch (SQLException ignored) {}
        return p;
    }
}
