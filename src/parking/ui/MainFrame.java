package parking.ui;

import parking.db.DBConnection;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * MainFrame – root JFrame with a JTabbedPane and a full menu bar.
 * Demonstrates: JFrame, JMenuBar, JMenu, JMenuItem, JTabbedPane,
 * BorderLayout, event handling via ActionListener.
 */
public class MainFrame extends JFrame {

    private JTabbedPane tabs;
    private JLabel statusBar;

    // Panel instances (lazily created once)
    private UserVehiclePanel   userVehiclePanel;
    private ZoneSlotPanel      zoneSlotPanel;
    private ReservationPanel   reservationPanel;
    private EntryExitPanel     entryExitPanel;
    private PassPaymentPanel   passPaymentPanel;
    private ViolationPanel     violationPanel;
    private ReportsPanel       reportsPanel;

    public MainFrame() {
        super("Smart Campus Parking & Traffic Management System");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(1200, 750);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { confirmExit(); }
        });

        initUI();
        testDBOnStartup();
    }

    // ── UI SETUP ─────────────────────────────────────────────────────────────
    private void initUI() {
        setLayout(new BorderLayout());

        // ── Header banner ──
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(0, 70, 127));
        header.setPreferredSize(new Dimension(0, 55));
        JLabel title = new JLabel("  \uD83C\uDFDB  Smart Campus Parking & Traffic Management System", JLabel.LEFT);
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        header.add(title, BorderLayout.WEST);
        JLabel dateLabel = new JLabel(new java.util.Date().toString() + "  ", JLabel.RIGHT);
        dateLabel.setForeground(new Color(200, 230, 255));
        dateLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        header.add(dateLabel, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // ── Tabbed pane ──
        tabs = new JTabbedPane(JTabbedPane.TOP);
        tabs.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        userVehiclePanel = new UserVehiclePanel(this);
        zoneSlotPanel    = new ZoneSlotPanel(this);
        reservationPanel = new ReservationPanel(this);
        entryExitPanel   = new EntryExitPanel(this);
        passPaymentPanel = new PassPaymentPanel(this);
        violationPanel   = new ViolationPanel(this);
        reportsPanel     = new ReportsPanel(this);

        tabs.addTab("\uD83D\uDC64 Users & Vehicles",   userVehiclePanel);
        tabs.addTab("\uD83C\uDD7F Zones & Slots",      zoneSlotPanel);
        tabs.addTab("\uD83D\uDDD3 Reservations",        reservationPanel);
        tabs.addTab("\uD83D\uDE97 Entry / Exit",        entryExitPanel);
        tabs.addTab("\uD83C\uDF9F Passes & Payments",   passPaymentPanel);
        tabs.addTab("\u26A0 Violations",                violationPanel);
        tabs.addTab("\uD83D\uDCC8 Reports",             reportsPanel);

        add(tabs, BorderLayout.CENTER);

        // ── Status bar ──
        statusBar = new JLabel("  Ready");
        statusBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Color.GRAY),
                BorderFactory.createEmptyBorder(3, 6, 3, 6)));
        statusBar.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        add(statusBar, BorderLayout.SOUTH);

        // ── Menu bar ──
        setJMenuBar(buildMenuBar());
    }

    // ── MENU BAR ──────────────────────────────────────────────────────────────
    private JMenuBar buildMenuBar() {
        JMenuBar mb = new JMenuBar();

        // File menu
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        JMenuItem miRefresh = new JMenuItem("Refresh All", KeyEvent.VK_R);
        miRefresh.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
        miRefresh.addActionListener(e -> refreshAll());
        JMenuItem miExit = new JMenuItem("Exit", KeyEvent.VK_X);
        miExit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK));
        miExit.addActionListener(e -> confirmExit());
        fileMenu.add(miRefresh);
        fileMenu.addSeparator();
        fileMenu.add(miExit);

        // Manage menu
        JMenu manageMenu = new JMenu("Manage");
        String[] labels = {"Users & Vehicles", "Zones & Slots", "Reservations", "Entry / Exit", "Passes & Payments", "Violations"};
        for (int i = 0; i < labels.length; i++) {
            final int idx = i;
            JMenuItem mi = new JMenuItem(labels[i]);
            mi.addActionListener(e -> tabs.setSelectedIndex(idx));
            manageMenu.add(mi);
        }

        // Reports menu
        JMenu reportsMenu = new JMenu("Reports");
        JMenuItem miReports = new JMenuItem("Open Reports");
        miReports.addActionListener(e -> tabs.setSelectedIndex(6));
        reportsMenu.add(miReports);

        // Database menu
        JMenu dbMenu = new JMenu("Database");
        JMenuItem miTest = new JMenuItem("Test Connection");
        miTest.addActionListener(e -> {
            String err = DBConnection.testConnection();
            if (err == null) showInfo("Database connection is OK.");
            else showError("Connection failed:\n" + err);
        });
        dbMenu.add(miTest);

        // Help menu
        JMenu helpMenu = new JMenu("Help");
        JMenuItem miAbout = new JMenuItem("About");
        miAbout.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "Smart Campus Parking & Traffic Management System\n" +
                "Version 1.0  |  CSA09 – Programming in Java\n" +
                "Built with Java Swing + JDBC (MySQL)\n\n" +
                "Covers: AWT/Swing, Event Handling, Layout Managers,\n" +
                "JDBC, PreparedStatement, CallableStatement, SQL CRUD",
                "About", JOptionPane.INFORMATION_MESSAGE));
        helpMenu.add(miAbout);

        mb.add(fileMenu);
        mb.add(manageMenu);
        mb.add(reportsMenu);
        mb.add(dbMenu);
        mb.add(helpMenu);
        return mb;
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────
    public void setStatus(String msg) {
        statusBar.setText("  " + msg);
    }

    public void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public void showInfo(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    public void refreshAll() {
        userVehiclePanel.refresh();
        zoneSlotPanel.refresh();
        reservationPanel.refresh();
        entryExitPanel.refresh();
        passPaymentPanel.refresh();
        violationPanel.refresh();
        reportsPanel.refresh();
        setStatus("All panels refreshed.");
    }

    private void testDBOnStartup() {
        SwingUtilities.invokeLater(() -> {
            String err = DBConnection.testConnection();
            if (err != null) {
                setStatus("⚠ DB connection failed – check credentials in DBConnection.java");
                JOptionPane.showMessageDialog(this,
                        "Could not connect to MySQL:\n" + err +
                        "\n\nPlease:\n1. Start MySQL server\n2. Run database.sql\n3. Update DBConnection.java with your password.",
                        "Database Connection Error", JOptionPane.WARNING_MESSAGE);
            } else {
                setStatus("Connected to campus_parking database.");
            }
        });
    }

    private void confirmExit() {
        int r = JOptionPane.showConfirmDialog(this,
                "Exit the application?", "Confirm Exit",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (r == JOptionPane.YES_OPTION) {
            DBConnection.close();
            dispose();
            System.exit(0);
        }
    }

    // ── MAIN ──────────────────────────────────────────────────────────────────
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}
