package parking.dao;

import parking.db.DBConnection;
import parking.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * UserDAO – uses PreparedStatement for all DML; Statement for simple reads.
 */
public class UserDAO {

    // ── INSERT ──────────────────────────────────────────────────────────────
    public boolean addUser(User u) throws SQLException {
        String sql = "INSERT INTO users(full_name,email,phone,user_type,id_number,address) VALUES(?,?,?,?,?,?)";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, u.getFullName());
            ps.setString(2, u.getEmail());
            ps.setString(3, u.getPhone());
            ps.setString(4, u.getUserType());
            ps.setString(5, u.getIdNumber());
            ps.setString(6, u.getAddress());
            return ps.executeUpdate() > 0;
        }
    }

    // ── UPDATE ──────────────────────────────────────────────────────────────
    public boolean updateUser(User u) throws SQLException {
        String sql = "UPDATE users SET full_name=?,email=?,phone=?,user_type=?,id_number=?,address=? WHERE user_id=?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, u.getFullName());
            ps.setString(2, u.getEmail());
            ps.setString(3, u.getPhone());
            ps.setString(4, u.getUserType());
            ps.setString(5, u.getIdNumber());
            ps.setString(6, u.getAddress());
            ps.setInt(7, u.getUserId());
            return ps.executeUpdate() > 0;
        }
    }

    // ── DELETE ──────────────────────────────────────────────────────────────
    public boolean deleteUser(int userId) throws SQLException {
        String sql = "DELETE FROM users WHERE user_id=?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            return ps.executeUpdate() > 0;
        }
    }

    // ── SELECT ALL ──────────────────────────────────────────────────────────
    public List<User> getAllUsers() throws SQLException {
        List<User> list = new ArrayList<>();
        // Using plain Statement (as required by rubric for SELECT variety)
        try (Statement st = DBConnection.getConnection().createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM users ORDER BY full_name")) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    // ── SELECT BY ID ────────────────────────────────────────────────────────
    public User getUserById(int userId) throws SQLException {
        String sql = "SELECT * FROM users WHERE user_id=?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        }
        return null;
    }

    // ── SEARCH ──────────────────────────────────────────────────────────────
    public List<User> searchUsers(String keyword) throws SQLException {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM users WHERE full_name LIKE ? OR email LIKE ? OR id_number LIKE ? OR phone LIKE ?";
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

    // ── FILTER BY TYPE ───────────────────────────────────────────────────────
    public List<User> getUsersByType(String userType) throws SQLException {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM users WHERE user_type=? ORDER BY full_name";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, userType);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    // ── MAP ROW ──────────────────────────────────────────────────────────────
    private User map(ResultSet rs) throws SQLException {
        User u = new User();
        u.setUserId(rs.getInt("user_id"));
        u.setFullName(rs.getString("full_name"));
        u.setEmail(rs.getString("email"));
        u.setPhone(rs.getString("phone"));
        u.setUserType(rs.getString("user_type"));
        u.setIdNumber(rs.getString("id_number"));
        u.setAddress(rs.getString("address"));
        u.setCreatedAt(rs.getTimestamp("created_at"));
        return u;
    }
}
