package parking.ui;

import parking.dao.*;
import parking.model.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

/**
 * EntryExitPanel – record vehicle entry (via stored procedure CallableStatement)
 * and exit (calculates fee). Also shows active sessions and full history.
 */
public class EntryExitPanel extends JPanel {

    private final MainFrame parent;
    private final SessionDAO sessionDAO = new SessionDAO();
    private final VehicleDAO vehicleDAO = new VehicleDAO();
    private final ParkingZoneDAO zoneDAO = new ParkingZoneDAO();
    private final ParkingSlotDAO slotDAO = new ParkingSlotDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();

    // Entry widgets
    private JTextField tfEntryPlate;
    private JComboBox<ZoneItem> cbEntryZone;
    private JComboBox<SlotItem> cbEntrySlot;
    private JButton btnEntry, btnReloadSlots;
    private JLabel lblVehicleInfo;

    // Exit widgets
    private JTextField tfExitPlate;
    private JLabel lblSessionInfo, lblFee;
    private JComboBox<String> cbPayMode;
    private JButton btnLookup, btnExit;
    private ParkingSession currentSession = null;

    // Tables
    private JTable activeTable, historyTable;
    private DefaultTableModel activeModel, historyModel;

    public EntryExitPanel(MainFrame parent) {
        this.parent = parent;
        setLayout(new BorderLayout(6, 6));
        setBorder(new EmptyBorder(8, 8, 8, 8));

        JSplitPane topSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildEntryPanel(), buildExitPanel());
        topSplit.setResizeWeight(0.5);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topSplit, buildSessionTables());
        mainSplit.setResizeWeight(0.4);

        add(mainSplit, BorderLayout.CENTER);
        refresh();
    }

    // ── Entry Panel ──────────────────────────────────────────────────────────
    private JPanel buildEntryPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(new TitledBorder("Vehicle Entry"));
        GridBagConstraints gc = gbc();

        tfEntryPlate  = new JTextField(14);
        cbEntryZone   = new JComboBox<>();
        cbEntrySlot   = new JComboBox<>();
        lblVehicleInfo = new JLabel(" ");
        lblVehicleInfo.setForeground(new Color(0,100,0));
        btnReloadSlots = btn("Find Vehicle / Reload Slots", new Color(0,102,204));
        btnEntry       = btn("Record Entry", new Color(34,139,34));

        row(p,gc,0,"License Plate*:", tfEntryPlate);
        row(p,gc,1,"Zone*:",          cbEntryZone);
        row(p,gc,2,"Available Slot*:",cbEntrySlot);
        gc.gridx=0;gc.gridy=3;gc.gridwidth=4; p.add(lblVehicleInfo,gc);
        gc.gridy=4;
        JPanel btns=new JPanel(new FlowLayout(FlowLayout.LEFT,6,0));
        btns.add(btnReloadSlots); btns.add(btnEntry); p.add(btns,gc);

        cbEntryZone.addActionListener(e -> loadAvailableSlots());
        btnReloadSlots.addActionListener(e -> lookupVehicleForEntry());
        btnEntry.addActionListener(e -> recordEntry());

        return p;
    }

    private void lookupVehicleForEntry() {
        String plate = tfEntryPlate.getText().trim().toUpperCase();
        if (plate.isEmpty()) { parent.showError("Enter a license plate."); return; }
        try {
            Vehicle v = vehicleDAO.getVehicleByPlate(plate);
            if (v == null) { parent.showError("No vehicle found with plate: "+plate); lblVehicleInfo.setText(" "); return; }
            lblVehicleInfo.setText("Owner: "+v.getOwnerName()+"  |  Type: "+v.getVehicleType());
            loadAvailableSlots();
        } catch (SQLException ex) { parent.showError(ex.getMessage()); }
    }

    private void loadAvailableSlots() {
        cbEntrySlot.removeAllItems();
        ZoneItem zi = (ZoneItem) cbEntryZone.getSelectedItem();
        if (zi == null) return;
        try {
            for (ParkingSlot s : slotDAO.getAvailableSlots(zi.id))
                cbEntrySlot.addItem(new SlotItem(s.getSlotId(), s.getSlotNumber()+" ["+s.getSlotType()+"]"));
        } catch (SQLException ex) { parent.showError(ex.getMessage()); }
    }

    private void recordEntry() {
        String plate = tfEntryPlate.getText().trim().toUpperCase();
        if (plate.isEmpty())             { parent.showError("Enter a license plate."); return; }
        if (cbEntrySlot.getItemCount()==0){ parent.showError("No available slot selected."); return; }
        try {
            Vehicle v = vehicleDAO.getVehicleByPlate(plate);
            if (v == null) { parent.showError("Vehicle not registered. Register it first."); return; }
            // Check if already parked
            ParkingSession active = sessionDAO.getActiveSessionByPlate(plate);
            if (active != null) { parent.showError("Vehicle is already parked (Session "+active.getSessionId()+")."); return; }

            SlotItem si = (SlotItem) cbEntrySlot.getSelectedItem();
            int sessionId = sessionDAO.recordEntry(v.getVehicleId(), si.id, v.getUserId());
            parent.setStatus("Entry recorded. Session ID = " + sessionId);
            JOptionPane.showMessageDialog(parent,
                "Entry recorded successfully!\nSession ID: "+sessionId+"\nVehicle: "+plate+"\nSlot: "+si.label,
                "Entry Recorded", JOptionPane.INFORMATION_MESSAGE);
            tfEntryPlate.setText(""); lblVehicleInfo.setText(" ");
            refresh();
        } catch (SQLException ex) { parent.showError("Entry failed:\n"+ex.getMessage()); }
    }

    // ── Exit Panel ───────────────────────────────────────────────────────────
    private JPanel buildExitPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(new TitledBorder("Vehicle Exit & Fee Collection"));
        GridBagConstraints gc = gbc();

        tfExitPlate   = new JTextField(14);
        lblSessionInfo = new JLabel("Session: –");
        lblFee         = new JLabel("Fee: –");
        lblFee.setFont(new Font("Segoe UI",Font.BOLD,14));
        lblFee.setForeground(new Color(180,0,0));
        cbPayMode = new JComboBox<>(new String[]{"Cash","Card","UPI","Online"});
        btnLookup = btn("Lookup Active Session", new Color(0,102,204));
        btnExit   = btn("Record Exit & Pay",     new Color(34,139,34));
        btnExit.setEnabled(false);

        row(p,gc,0,"License Plate*:", tfExitPlate);
        gc.gridx=0;gc.gridy=1;gc.gridwidth=4; p.add(lblSessionInfo,gc);
        gc.gridy=2; p.add(lblFee,gc);
        gc.gridy=3;gc.gridwidth=2; p.add(new JLabel("Payment Mode:"),gc);
        gc.gridx=2; p.add(cbPayMode,gc);
        gc.gridx=0;gc.gridy=4;gc.gridwidth=4;
        JPanel btns=new JPanel(new FlowLayout(FlowLayout.LEFT,6,0));
        btns.add(btnLookup); btns.add(btnExit); p.add(btns,gc);

        btnLookup.addActionListener(e -> lookupSession());
        btnExit.addActionListener(e -> recordExit());

        return p;
    }

    private void lookupSession() {
        String plate = tfExitPlate.getText().trim().toUpperCase();
        if (plate.isEmpty()) { parent.showError("Enter a license plate."); return; }
        try {
            currentSession = sessionDAO.getActiveSessionByPlate(plate);
            if (currentSession == null) {
                lblSessionInfo.setText("No active session for: " + plate);
                lblFee.setText("Fee: –"); btnExit.setEnabled(false); return;
            }
            lblSessionInfo.setText("Session ID: "+currentSession.getSessionId()
                +"  |  Zone: "+currentSession.getZoneName()
                +"  |  Slot: "+currentSession.getSlotNumber()
                +"  |  Entry: "+currentSession.getEntryTime());
            lblFee.setText("Calculating at exit…");
            btnExit.setEnabled(true);
        } catch (SQLException ex) { parent.showError(ex.getMessage()); }
    }

    private void recordExit() {
        if (currentSession == null) { parent.showError("Lookup a session first."); return; }
        try {
            BigDecimal fee = sessionDAO.recordExit(currentSession.getSessionId());
            // Record payment
            Payment pay = new Payment();
            pay.setUserId(currentSession.getUserId());
            pay.setSessionId(currentSession.getSessionId());
            pay.setAmount(fee);
            pay.setPaymentMode((String)cbPayMode.getSelectedItem());
            pay.setStatus("Paid");
            paymentDAO.addPayment(pay);

            lblFee.setText("Fee Charged: ₹" + fee);
            JOptionPane.showMessageDialog(parent,
                "Exit recorded!\nVehicle: "+currentSession.getLicensePlate()
                +"\nDuration: calculated\nFee: ₹"+fee
                +"\nPayment Mode: "+cbPayMode.getSelectedItem(),
                "Exit & Payment", JOptionPane.INFORMATION_MESSAGE);
            parent.setStatus("Exit recorded. Fee ₹"+fee+" collected.");
            tfExitPlate.setText(""); lblSessionInfo.setText("Session: –");
            btnExit.setEnabled(false); currentSession = null;
            refresh();
        } catch (SQLException ex) { parent.showError("Exit failed:\n"+ex.getMessage()); }
    }

    // ── Session Tables ────────────────────────────────────────────────────────
    private JPanel buildSessionTables() {
        JPanel p = new JPanel(new GridLayout(1,2,6,0));

        // Active sessions
        String[] aCols = {"Session ID","Vehicle","User","Zone","Slot","Entry Time","Status"};
        activeModel = new DefaultTableModel(aCols,0){public boolean isCellEditable(int r,int c){return false;}};
        activeTable = new JTable(activeModel);
        styleTable(activeTable);
        JPanel ap = new JPanel(new BorderLayout());
        ap.setBorder(new TitledBorder("Active Sessions"));
        ap.add(new JScrollPane(activeTable));
        p.add(ap);

        // History
        String[] hCols = {"Session ID","Vehicle","Zone","Slot","Entry","Exit","Hours","Fee (₹)","Status"};
        historyModel = new DefaultTableModel(hCols,0){public boolean isCellEditable(int r,int c){return false;}};
        historyTable = new JTable(historyModel);
        styleTable(historyTable);
        JPanel hp = new JPanel(new BorderLayout());
        hp.setBorder(new TitledBorder("Parking History"));
        hp.add(new JScrollPane(historyTable));
        p.add(hp);

        return p;
    }

    private void loadActiveSessions() {
        try {
            activeModel.setRowCount(0);
            for (ParkingSession s : sessionDAO.getActiveSessions())
                activeModel.addRow(new Object[]{s.getSessionId(),s.getLicensePlate(),s.getUserName(),
                        s.getZoneName(),s.getSlotNumber(),s.getEntryTime(),s.getStatus()});
        } catch (SQLException ex) { parent.showError(ex.getMessage()); }
    }

    private void loadHistory() {
        try {
            historyModel.setRowCount(0);
            for (ParkingSession s : sessionDAO.getAllSessions())
                historyModel.addRow(new Object[]{s.getSessionId(),s.getLicensePlate(),s.getZoneName(),
                        s.getSlotNumber(),s.getEntryTime(),s.getExitTime(),s.getDurationHrs(),s.getFeeAmount(),s.getStatus()});
        } catch (SQLException ex) { parent.showError(ex.getMessage()); }
    }

    private void loadZoneCombo() {
        cbEntryZone.removeAllItems();
        try { for(ParkingZone z : zoneDAO.getAllZones()) cbEntryZone.addItem(new ZoneItem(z.getZoneId(),z.getZoneName()+" ("+z.getZoneType()+")")); }
        catch (SQLException ex){ parent.showError(ex.getMessage()); }
    }

    public void refresh() { loadZoneCombo(); loadAvailableSlots(); loadActiveSessions(); loadHistory(); }

    // ── Shared ───────────────────────────────────────────────────────────────
    private GridBagConstraints gbc(){GridBagConstraints gc=new GridBagConstraints();gc.insets=new Insets(5,8,5,8);gc.fill=GridBagConstraints.HORIZONTAL;return gc;}
    private void row(JPanel p,GridBagConstraints gc,int r,String l,JComponent f){gc.gridx=0;gc.gridy=r;gc.gridwidth=1;gc.weightx=0;p.add(new JLabel(l),gc);gc.gridx=1;gc.weightx=1;gc.gridwidth=3;p.add(f,gc);}
    private void styleTable(JTable t){t.setRowHeight(22);t.setFont(new Font("Segoe UI",Font.PLAIN,12));t.getTableHeader().setFont(new Font("Segoe UI",Font.BOLD,12));t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);t.setAutoCreateRowSorter(true);}
    private JButton btn(String t,Color bg){JButton b=new JButton(t);b.setBackground(bg);b.setForeground(Color.WHITE);b.setOpaque(true);b.setBorderPainted(false);b.setFocusPainted(false);b.setFont(new Font("Segoe UI",Font.BOLD,12));return b;}

    static class ZoneItem{final int id;final String l;ZoneItem(int i,String l){id=i;this.l=l;}public String toString(){return l;}}
    static class SlotItem{final int id;final String label;SlotItem(int i,String l){id=i;this.label=l;}public String toString(){return label;}}
}
