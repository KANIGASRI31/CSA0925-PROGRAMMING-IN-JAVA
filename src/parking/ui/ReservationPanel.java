package parking.ui;

import parking.dao.*;
import parking.model.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * ReservationPanel – create/cancel/view reservations.
 * Demonstrates: JComboBox chaining, JTextField date input,
 * conflict-checking PreparedStatement, status filtering.
 */
public class ReservationPanel extends JPanel {

    private final MainFrame parent;
    private final ReservationDAO reservationDAO = new ReservationDAO();
    private final UserDAO userDAO = new UserDAO();
    private final VehicleDAO vehicleDAO = new VehicleDAO();
    private final ParkingZoneDAO zoneDAO = new ParkingZoneDAO();
    private final ParkingSlotDAO slotDAO = new ParkingSlotDAO();

    private JComboBox<UserItem>    cbUser;
    private JComboBox<VehicleItem> cbVehicle;
    private JComboBox<ZoneItem>    cbZone;
    private JComboBox<SlotItem>    cbSlot;
    private JTextField tfFrom, tfUntil;
    private JLabel lblAvailable;
    private JButton btnReserve, btnCancel, btnRefresh;
    private JComboBox<String> cbStatusFilter;

    private JTable table;
    private DefaultTableModel tableModel;
    private int selectedReservationId = -1;

    private static final String DATE_FMT = "yyyy-MM-dd HH:mm";

    public ReservationPanel(MainFrame parent) {
        this.parent = parent;
        setLayout(new BorderLayout(6, 6));
        setBorder(new EmptyBorder(8, 8, 8, 8));
        add(buildForm(), BorderLayout.NORTH);
        add(buildTable(), BorderLayout.CENTER);
        refresh();
    }

    private JPanel buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new TitledBorder("New Reservation"));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(5, 8, 5, 8);
        gc.fill = GridBagConstraints.HORIZONTAL;

        cbUser    = new JComboBox<>();
        cbVehicle = new JComboBox<>();
        cbZone    = new JComboBox<>();
        cbSlot    = new JComboBox<>();
        lblAvailable = new JLabel("Available slots: –");

        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FMT);
        tfFrom  = new JTextField(sdf.format(new Date()), 16);
        tfUntil = new JTextField(sdf.format(new Date(System.currentTimeMillis() + 3600_000L)), 16);

        // Row 0
        gc.gridx=0; gc.gridy=0; gc.weightx=0; form.add(new JLabel("User*:"), gc);
        gc.gridx=1; gc.weightx=1; form.add(cbUser, gc);
        gc.gridx=2; gc.weightx=0; form.add(new JLabel("Vehicle*:"), gc);
        gc.gridx=3; gc.weightx=1; form.add(cbVehicle, gc);

        // Row 1
        gc.gridx=0; gc.gridy=1; gc.weightx=0; form.add(new JLabel("Zone*:"), gc);
        gc.gridx=1; gc.weightx=1; form.add(cbZone, gc);
        gc.gridx=2; gc.weightx=0; form.add(new JLabel("Slot*:"), gc);
        gc.gridx=3; gc.weightx=1; form.add(cbSlot, gc);

        // Row 2
        gc.gridx=0; gc.gridy=2; gc.weightx=0; form.add(new JLabel("From (yyyy-MM-dd HH:mm)*:"), gc);
        gc.gridx=1; gc.weightx=1; form.add(tfFrom, gc);
        gc.gridx=2; gc.weightx=0; form.add(new JLabel("Until (yyyy-MM-dd HH:mm)*:"), gc);
        gc.gridx=3; gc.weightx=1; form.add(tfUntil, gc);

        // Row 3 – availability info + buttons
        gc.gridx=0; gc.gridy=3; gc.gridwidth=2; form.add(lblAvailable, gc);
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        btnReserve = makeBtn("Reserve Slot", new Color(34,139,34));
        btnCancel  = makeBtn("Cancel Selected", new Color(200,40,40));
        btnRefresh = makeBtn("Refresh", Color.DARK_GRAY);
        cbStatusFilter = new JComboBox<>(new String[]{"All","Active","Completed","Cancelled","Expired"});
        btns.add(btnReserve); btns.add(btnCancel); btns.add(btnRefresh);
        btns.add(new JLabel("  Filter:")); btns.add(cbStatusFilter);
        gc.gridx=2; gc.gridwidth=2; form.add(btns, gc);

        // Events
        cbUser.addActionListener(e -> reloadVehicles());
        cbZone.addActionListener(e -> reloadSlots());
        btnReserve.addActionListener(e -> makeReservation());
        btnCancel.addActionListener(e -> cancelReservation());
        btnRefresh.addActionListener(e -> refresh());
        cbStatusFilter.addActionListener(e -> loadReservations());

        return form;
    }

    private JScrollPane buildTable() {
        String[] cols = {"ID","User","License Plate","Zone","Slot","From","Until","Status","Created"};
        tableModel = new DefaultTableModel(cols, 0){ public boolean isCellEditable(int r,int c){return false;} };
        table = new JTable(tableModel);
        table.setRowHeight(22);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = table.getSelectedRow();
                selectedReservationId = row >= 0 ? (int) tableModel.getValueAt(row, 0) : -1;
            }
        });
        return new JScrollPane(table);
    }

    private void makeReservation() {
        if (cbUser.getSelectedIndex()<0)    { parent.showError("Select a user."); return; }
        if (cbVehicle.getSelectedIndex()<0) { parent.showError("Select a vehicle."); return; }
        if (cbZone.getSelectedIndex()<0)    { parent.showError("Select a zone."); return; }
        if (cbSlot.getSelectedIndex()<0)    { parent.showError("Select a slot."); return; }

        Timestamp from, until;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(DATE_FMT);
            from  = new Timestamp(sdf.parse(tfFrom.getText().trim()).getTime());
            until = new Timestamp(sdf.parse(tfUntil.getText().trim()).getTime());
        } catch (Exception ex) {
            parent.showError("Invalid date format. Use: " + DATE_FMT); return;
        }
        if (!until.after(from)) { parent.showError("'Until' must be after 'From'."); return; }

        Reservation r = new Reservation();
        r.setUserId(((UserItem)cbUser.getSelectedItem()).id);
        r.setVehicleId(((VehicleItem)cbVehicle.getSelectedItem()).id);
        r.setSlotId(((SlotItem)cbSlot.getSelectedItem()).id);
        r.setReservedFrom(from);
        r.setReservedUntil(until);

        try {
            int id = reservationDAO.addReservation(r);
            parent.setStatus("Reservation created. ID = " + id);
            refresh();
        } catch (SQLException ex) {
            parent.showError("Reservation failed:\n" + ex.getMessage());
        }
    }

    private void cancelReservation() {
        if (selectedReservationId < 0) { parent.showError("Select a reservation to cancel."); return; }
        int c = JOptionPane.showConfirmDialog(parent,"Cancel this reservation?","Confirm",JOptionPane.YES_NO_OPTION);
        if (c!=JOptionPane.YES_OPTION) return;
        try {
            reservationDAO.cancelReservation(selectedReservationId);
            parent.setStatus("Reservation cancelled."); refresh();
        } catch (SQLException ex) { parent.showError(ex.getMessage()); }
    }

    private void reloadVehicles() {
        cbVehicle.removeAllItems();
        UserItem ui = (UserItem) cbUser.getSelectedItem();
        if (ui == null) return;
        try {
            for (Vehicle v : vehicleDAO.getVehiclesByUser(ui.id))
                cbVehicle.addItem(new VehicleItem(v.getVehicleId(), v.getLicensePlate()+" ("+v.getVehicleType()+")"));
        } catch (SQLException ex) { parent.showError(ex.getMessage()); }
    }

    private void reloadSlots() {
        cbSlot.removeAllItems();
        ZoneItem zi = (ZoneItem) cbZone.getSelectedItem();
        if (zi == null) return;
        try {
            List<ParkingSlot> available = slotDAO.getAvailableSlots(zi.id);
            lblAvailable.setText("Available slots: " + available.size());
            for (ParkingSlot s : available)
                cbSlot.addItem(new SlotItem(s.getSlotId(), s.getSlotNumber()+" ["+s.getSlotType()+"]"));
        } catch (SQLException ex) { parent.showError(ex.getMessage()); }
    }

    private void loadReservations() {
        try {
            tableModel.setRowCount(0);
            List<Reservation> list = reservationDAO.getAllReservations();
            String filter = (String) cbStatusFilter.getSelectedItem();
            for (Reservation r : list) {
                if (!"All".equals(filter) && !filter.equals(r.getStatus())) continue;
                tableModel.addRow(new Object[]{r.getReservationId(), r.getUserName(), r.getLicensePlate(),
                        r.getZoneName(), r.getSlotNumber(), r.getReservedFrom(), r.getReservedUntil(),
                        r.getStatus(), r.getCreatedAt()});
            }
        } catch (SQLException ex) { parent.showError(ex.getMessage()); }
    }

    private void loadCombos() {
        cbUser.removeAllItems();
        cbZone.removeAllItems();
        try {
            for (User u : userDAO.getAllUsers())
                cbUser.addItem(new UserItem(u.getUserId(), u.getFullName()+" ("+u.getUserType()+")"));
            for (ParkingZone z : zoneDAO.getAllZones())
                cbZone.addItem(new ZoneItem(z.getZoneId(), z.getZoneName()+" – "+z.getZoneType()));
        } catch (SQLException ex) { parent.showError(ex.getMessage()); }
        reloadVehicles();
        reloadSlots();
    }

    public void refresh() { loadCombos(); loadReservations(); }

    private JButton makeBtn(String t, Color bg) {
        JButton b=new JButton(t); b.setBackground(bg); b.setForeground(Color.WHITE);
        b.setOpaque(true); b.setBorderPainted(false);
        b.setFocusPainted(false); b.setFont(new Font("Segoe UI",Font.BOLD,12)); return b;
    }

    static class UserItem    { final int id; final String l; UserItem(int i,String l){id=i;this.l=l;} public String toString(){return l;} }
    static class VehicleItem { final int id; final String l; VehicleItem(int i,String l){id=i;this.l=l;} public String toString(){return l;} }
    static class ZoneItem    { final int id; final String l; ZoneItem(int i,String l){id=i;this.l=l;} public String toString(){return l;} }
    static class SlotItem    { final int id; final String l; SlotItem(int i,String l){id=i;this.l=l;} public String toString(){return l;} }
}
