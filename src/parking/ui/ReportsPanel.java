package parking.ui;

import parking.db.DBConnection;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ReportsPanel – generates zone-wise, vehicle-wise, occupancy,
 * utilization and revenue reports using plain Statement and PreparedStatement.
 */
public class ReportsPanel extends JPanel {

    private final MainFrame parent;

    private JTable reportTable;
    private DefaultTableModel reportModel;
    private JTextArea summaryArea;
    private JLabel lblReportTitle;

    public ReportsPanel(MainFrame parent) {
        this.parent = parent;
        setLayout(new BorderLayout(6, 6));
        setBorder(new EmptyBorder(8, 8, 8, 8));
        add(buildButtonPanel(), BorderLayout.NORTH);
        add(buildCenter(),      BorderLayout.CENTER);
    }

    // ── Button panel ──────────────────────────────────────────────────────────
    private JPanel buildButtonPanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        p.setBorder(new TitledBorder("Generate Reports"));

        String[][] reports = {
            {"Zone-wise Occupancy",  "zone_occupancy"},
            {"Vehicle-wise History", "vehicle_history"},
            {"Revenue Report",       "revenue"},
            {"Active Sessions",      "active_sessions"},
            {"Utilization %",        "utilization"},
            {"Violation Summary",    "violations_summary"},
            {"Pass Status Report",   "pass_status"},
            {"Daily Revenue",        "daily_revenue"}
        };

        for (String[] r : reports) {
            JButton btn = new JButton(r[0]);
            btn.setBackground(new Color(0, 80, 160));
            btn.setForeground(Color.WHITE);
            btn.setOpaque(true);
            btn.setBorderPainted(false);
            btn.setFocusPainted(false);
            btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
            final String key = r[1];
            btn.addActionListener(e -> runReport(key, r[0]));
            p.add(btn);
        }
        return p;
    }

    private JPanel buildCenter() {
        JPanel p = new JPanel(new BorderLayout(6, 6));

        lblReportTitle = new JLabel("Select a report above to generate.");
        lblReportTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblReportTitle.setForeground(new Color(0, 60, 120));
        p.add(lblReportTitle, BorderLayout.NORTH);

        reportModel = new DefaultTableModel();
        reportTable = new JTable(reportModel);
        reportTable.setRowHeight(22);
        reportTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        reportTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        reportTable.setAutoCreateRowSorter(true);

        summaryArea = new JTextArea(5, 40);
        summaryArea.setEditable(false);
        summaryArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        summaryArea.setBorder(new TitledBorder("Summary"));

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(reportTable), new JScrollPane(summaryArea));
        split.setResizeWeight(0.75);
        p.add(split, BorderLayout.CENTER);

        return p;
    }

    // ── Report dispatcher ─────────────────────────────────────────────────────
    private void runReport(String key, String title) {
        lblReportTitle.setText("Report: " + title);
        reportModel.setRowCount(0); reportModel.setColumnCount(0);
        summaryArea.setText("");
        try {
            switch (key) {
                case "zone_occupancy"    -> zoneOccupancyReport();
                case "vehicle_history"   -> vehicleHistoryReport();
                case "revenue"           -> revenueReport();
                case "active_sessions"   -> activeSessionsReport();
                case "utilization"       -> utilizationReport();
                case "violations_summary"-> violationsSummaryReport();
                case "pass_status"       -> passStatusReport();
                case "daily_revenue"     -> dailyRevenueReport();
            }
            parent.setStatus("Report generated: " + title);
        } catch (SQLException ex) {
            parent.showError("Report error:\n" + ex.getMessage());
        }
    }

    // ── Zone-wise Occupancy ───────────────────────────────────────────────────
    private void zoneOccupancyReport() throws SQLException {
        String sql = """
            SELECT z.zone_name, z.zone_type, z.total_slots,
                   COUNT(CASE WHEN s.status='Available'   THEN 1 END) AS available,
                   COUNT(CASE WHEN s.status='Occupied'    THEN 1 END) AS occupied,
                   COUNT(CASE WHEN s.status='Reserved'    THEN 1 END) AS reserved,
                   COUNT(CASE WHEN s.status='Maintenance' THEN 1 END) AS maintenance
            FROM parking_zones z LEFT JOIN parking_slots s ON z.zone_id=s.zone_id
            GROUP BY z.zone_id, z.zone_name, z.zone_type, z.total_slots
            ORDER BY z.zone_name
            """;
        fillTable(sql, new String[]{"Zone","Type","Total","Available","Occupied","Reserved","Maintenance"});
        buildZoneOccupancySummary();
    }

    private void buildZoneOccupancySummary() throws SQLException {
        String sql = "SELECT SUM(total_slots) AS t, (SELECT COUNT(*) FROM parking_slots WHERE status='Available') AS a, (SELECT COUNT(*) FROM parking_slots WHERE status='Occupied') AS o FROM parking_zones";
        try (Statement st = DBConnection.getConnection().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                int t=rs.getInt("t"), a=rs.getInt("a"), o=rs.getInt("o");
                double pct = t>0 ? (o*100.0/t) : 0;
                summaryArea.setText(String.format(
                    "Total Slots: %d\nAvailable: %d\nOccupied: %d\nOccupancy Rate: %.1f%%", t, a, o, pct));
            }
        }
    }

    // ── Vehicle-wise History ──────────────────────────────────────────────────
    private void vehicleHistoryReport() throws SQLException {
        String sql = """
            SELECT v.license_plate, v.vehicle_type, u.full_name AS owner,
                   COUNT(ps.session_id)   AS total_sessions,
                   COALESCE(SUM(ps.duration_hrs),0) AS total_hours,
                   COALESCE(SUM(ps.fee_amount),0)   AS total_fee
            FROM vehicles v
            JOIN users u ON v.user_id=u.user_id
            LEFT JOIN parking_sessions ps ON v.vehicle_id=ps.vehicle_id
            GROUP BY v.vehicle_id, v.license_plate, v.vehicle_type, u.full_name
            ORDER BY total_sessions DESC
            """;
        fillTable(sql, new String[]{"License Plate","Type","Owner","Sessions","Total Hrs","Total Fee (₹)"});
        String sumSql = "SELECT COUNT(*) AS vehicles, SUM(duration_hrs) AS hrs, SUM(fee_amount) AS fee FROM parking_sessions";
        try (Statement st = DBConnection.getConnection().createStatement(); ResultSet rs = st.executeQuery(sumSql)) {
            if (rs.next())
                summaryArea.setText(String.format("Total Sessions: –\nTotal Hours: %.2f\nTotal Revenue: ₹%.2f",
                    rs.getDouble("hrs"), rs.getDouble("fee")));
        }
    }

    // ── Revenue Report ────────────────────────────────────────────────────────
    private void revenueReport() throws SQLException {
        String sql = """
            SELECT z.zone_name, z.zone_type,
                   COUNT(ps.session_id) AS sessions,
                   COALESCE(SUM(ps.fee_amount),0) AS session_revenue,
                   (SELECT COALESCE(SUM(pp.pass_fee),0) FROM parking_passes pp WHERE pp.zone_id=z.zone_id AND pp.status<>'Cancelled') AS pass_revenue
            FROM parking_zones z
            LEFT JOIN parking_slots sl ON z.zone_id=sl.zone_id
            LEFT JOIN parking_sessions ps ON sl.slot_id=ps.slot_id AND ps.status='Completed'
            GROUP BY z.zone_id, z.zone_name, z.zone_type
            ORDER BY session_revenue DESC
            """;
        fillTable(sql, new String[]{"Zone","Type","Sessions","Session Revenue (₹)","Pass Revenue (₹)"});
        String sumSql = "SELECT COALESCE(SUM(amount),0) AS total FROM payments WHERE status='Paid'";
        try (Statement st = DBConnection.getConnection().createStatement(); ResultSet rs = st.executeQuery(sumSql)) {
            if (rs.next()) summaryArea.setText("Total Revenue Collected: ₹" + rs.getDouble("total"));
        }
    }

    // ── Active Sessions ───────────────────────────────────────────────────────
    private void activeSessionsReport() throws SQLException {
        String sql = """
            SELECT ps.session_id, v.license_plate, v.vehicle_type, u.full_name,
                   z.zone_name, sl.slot_number, ps.entry_time,
                   TIMESTAMPDIFF(MINUTE, ps.entry_time, NOW()) AS minutes_parked
            FROM parking_sessions ps
            JOIN vehicles v ON ps.vehicle_id=v.vehicle_id
            JOIN users u ON ps.user_id=u.user_id
            JOIN parking_slots sl ON ps.slot_id=sl.slot_id
            JOIN parking_zones z ON sl.zone_id=z.zone_id
            WHERE ps.status='Active'
            ORDER BY ps.entry_time
            """;
        fillTable(sql, new String[]{"Session ID","Plate","Type","Owner","Zone","Slot","Entry Time","Mins Parked"});
        long cnt = countQuery("SELECT COUNT(*) FROM parking_sessions WHERE status='Active'");
        summaryArea.setText("Currently Active Sessions: " + cnt);
    }

    // ── Utilization % ─────────────────────────────────────────────────────────
    private void utilizationReport() throws SQLException {
        String sql = """
            SELECT z.zone_name, z.total_slots,
                   COUNT(ps.session_id) AS total_sessions,
                   COALESCE(SUM(ps.duration_hrs),0) AS total_hrs,
                   ROUND(COUNT(CASE WHEN sl.status='Occupied' THEN 1 END)*100.0 / GREATEST(z.total_slots,1), 1) AS occupancy_pct
            FROM parking_zones z
            LEFT JOIN parking_slots sl ON z.zone_id=sl.zone_id
            LEFT JOIN parking_sessions ps ON sl.slot_id=ps.slot_id
            GROUP BY z.zone_id, z.zone_name, z.total_slots
            ORDER BY occupancy_pct DESC
            """;
        fillTable(sql, new String[]{"Zone","Total Slots","Total Sessions","Total Hrs","Occupancy %"});
        summaryArea.setText("Utilization report shows current real-time slot occupancy percentage per zone.");
    }

    // ── Violation Summary ─────────────────────────────────────────────────────
    private void violationsSummaryReport() throws SQLException {
        String sql = """
            SELECT violation_type,
                   COUNT(*) AS count,
                   SUM(fine_amount) AS total_fine,
                   COUNT(CASE WHEN status='Pending' THEN 1 END) AS pending,
                   COUNT(CASE WHEN status='Paid'    THEN 1 END) AS paid,
                   COUNT(CASE WHEN status='Waived'  THEN 1 END) AS waived
            FROM violations
            GROUP BY violation_type
            ORDER BY count DESC
            """;
        fillTable(sql, new String[]{"Violation Type","Count","Total Fine (₹)","Pending","Paid","Waived"});
        String sumSql = "SELECT COUNT(*) AS c, SUM(fine_amount) AS f, SUM(CASE WHEN status='Pending' THEN fine_amount ELSE 0 END) AS pending_f FROM violations";
        try (Statement st = DBConnection.getConnection().createStatement(); ResultSet rs = st.executeQuery(sumSql)) {
            if (rs.next())
                summaryArea.setText(String.format(
                    "Total Violations: %d\nTotal Fines: ₹%.2f\nPending Collection: ₹%.2f",
                    rs.getInt("c"), rs.getDouble("f"), rs.getDouble("pending_f")));
        }
    }

    // ── Pass Status Report ────────────────────────────────────────────────────
    private void passStatusReport() throws SQLException {
        String sql = """
            SELECT z.zone_name, pp.pass_type,
                   COUNT(*) AS total,
                   COUNT(CASE WHEN pp.status='Active'    THEN 1 END) AS active,
                   COUNT(CASE WHEN pp.status='Expired'   THEN 1 END) AS expired,
                   COUNT(CASE WHEN pp.status='Cancelled' THEN 1 END) AS cancelled,
                   COALESCE(SUM(CASE WHEN pp.status='Active' THEN pp.pass_fee ELSE 0 END),0) AS active_revenue
            FROM parking_passes pp
            JOIN parking_zones z ON pp.zone_id=z.zone_id
            GROUP BY z.zone_id, z.zone_name, pp.pass_type
            ORDER BY z.zone_name, pp.pass_type
            """;
        fillTable(sql, new String[]{"Zone","Pass Type","Total","Active","Expired","Cancelled","Active Revenue (₹)"});
        long active = countQuery("SELECT COUNT(*) FROM parking_passes WHERE status='Active'");
        summaryArea.setText("Currently Active Passes: " + active);
    }

    // ── Daily Revenue ─────────────────────────────────────────────────────────
    private void dailyRevenueReport() throws SQLException {
        String sql = """
            SELECT DATE(payment_date) AS date,
                   COUNT(*) AS transactions,
                   SUM(CASE WHEN session_id IS NOT NULL THEN amount ELSE 0 END) AS session_rev,
                   SUM(CASE WHEN pass_id    IS NOT NULL THEN amount ELSE 0 END) AS pass_rev,
                   SUM(amount) AS total
            FROM payments
            WHERE status='Paid'
            GROUP BY DATE(payment_date)
            ORDER BY date DESC
            LIMIT 30
            """;
        fillTable(sql, new String[]{"Date","Transactions","Session Rev (₹)","Pass Rev (₹)","Total (₹)"});
        String sumSql = "SELECT SUM(amount) AS t FROM payments WHERE status='Paid'";
        try (Statement st = DBConnection.getConnection().createStatement(); ResultSet rs = st.executeQuery(sumSql)) {
            if (rs.next()) summaryArea.setText("All-time Total Revenue: ₹" + rs.getDouble("t"));
        }
    }

    // ── Generic table filler ─────────────────────────────────────────────────
    private void fillTable(String sql, String[] columns) throws SQLException {
        reportModel.setColumnCount(0);
        reportModel.setRowCount(0);
        for (String c : columns) reportModel.addColumn(c);

        try (Statement st = DBConnection.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            int cols = rs.getMetaData().getColumnCount();
            while (rs.next()) {
                Object[] row = new Object[cols];
                for (int i = 0; i < cols; i++) row[i] = rs.getObject(i + 1);
                reportModel.addRow(row);
            }
        }
    }

    private long countQuery(String sql) throws SQLException {
        try (Statement st = DBConnection.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getLong(1) : 0;
        }
    }

    public void refresh() { /* reports are on-demand */ }
}
