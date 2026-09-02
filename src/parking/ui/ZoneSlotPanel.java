package parking.ui;

import parking.dao.ParkingZoneDAO;
import parking.dao.ParkingSlotDAO;
import parking.model.ParkingZone;
import parking.model.ParkingSlot;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

/**
 * ZoneSlotPanel – manage parking zones and their slots.
 * Demonstrates: JSplitPane, JTable with colour-coded rows, GridBagLayout,
 * JSpinner, JComboBox, PreparedStatement-backed DAO calls.
 */
public class ZoneSlotPanel extends JPanel {

    private final MainFrame parent;
    private final ParkingZoneDAO zoneDAO = new ParkingZoneDAO();
    private final ParkingSlotDAO slotDAO = new ParkingSlotDAO();

    // Zone widgets
    private JTable zoneTable;
    private DefaultTableModel zoneModel;
    private JTextField tfZoneName, tfLocation, tfDescription;
    private JComboBox<String> cbZoneType;
    private JSpinner spnTotalSlots;
    private JTextField tfHourlyRate;
    private JButton btnAddZone, btnUpdateZone, btnDeleteZone, btnClearZone;
    private int selectedZoneId = -1;

    // Slot widgets
    private JTable slotTable;
    private DefaultTableModel slotModel;
    private JComboBox<ZoneItem> cbSlotZone;
    private JTextField tfSlotNumber;
    private JComboBox<String> cbSlotType, cbSlotStatus, cbFilterStatus;
    private JButton btnAddSlot, btnUpdateSlot, btnDeleteSlot, btnClearSlot;
    private int selectedSlotId = -1;

    public ZoneSlotPanel(MainFrame parent) {
        this.parent = parent;
        setLayout(new BorderLayout());
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, buildZonePanel(), buildSlotPanel());
        split.setResizeWeight(0.45);
        split.setDividerSize(6);
        add(split, BorderLayout.CENTER);
        refresh();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  ZONE PANEL
    // ════════════════════════════════════════════════════════════════════════
    private JPanel buildZonePanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(new TitledBorder("Parking Zones"));

        // Form
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gc = gbc();

        tfZoneName    = new JTextField(14);
        cbZoneType    = new JComboBox<>(new String[]{"Student","Faculty","Staff","Visitor","Service","General"});
        tfLocation    = new JTextField(18);
        spnTotalSlots = new JSpinner(new SpinnerNumberModel(10, 1, 500, 1));
        tfHourlyRate  = new JTextField("0.00", 8);
        tfDescription = new JTextField(22);

        row(form, gc, 0, "Zone Name*:", tfZoneName);
        row(form, gc, 1, "Zone Type:",  cbZoneType);
        row(form, gc, 2, "Location:",   tfLocation);
        row(form, gc, 3, "Total Slots:", spnTotalSlots);
        row(form, gc, 4, "Hourly Rate (₹):", tfHourlyRate);
        row(form, gc, 5, "Description:", tfDescription);

        btnAddZone    = btn("Add Zone",    new Color(34,139,34));
        btnUpdateZone = btn("Update",      new Color(0,102,204));
        btnDeleteZone = btn("Delete",      new Color(200,40,40));
        btnClearZone  = btn("Clear",       Color.GRAY);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        btns.add(btnAddZone); btns.add(btnUpdateZone); btns.add(btnDeleteZone); btns.add(btnClearZone);
        gc.gridx=0; gc.gridy=6; gc.gridwidth=4; form.add(btns, gc);

        panel.add(form, BorderLayout.WEST);

        // Table
        String[] cols = {"ID","Zone Name","Type","Location","Total","Available","Rate (₹)","Description"};
        zoneModel = new DefaultTableModel(cols, 0){ public boolean isCellEditable(int r,int c){return false;} };
        zoneTable = new JTable(zoneModel);
        styleTable(zoneTable);
        zoneTable.getSelectionModel().addListSelectionListener(e -> { if(!e.getValueIsAdjusting()) fillZoneForm(); });
        panel.add(new JScrollPane(zoneTable), BorderLayout.CENTER);

        btnAddZone.addActionListener(e -> addZone());
        btnUpdateZone.addActionListener(e -> updateZone());
        btnDeleteZone.addActionListener(e -> deleteZone());
        btnClearZone.addActionListener(e -> clearZoneForm());

        return panel;
    }

    private void addZone() {
        if (!validateZoneForm()) return;
        try {
            ParkingZone z = buildZoneFromForm();
            if (zoneDAO.addZone(z)) { parent.setStatus("Zone added."); clearZoneForm(); loadZones(); loadZoneCombo(); }
        } catch (SQLException ex) { parent.showError("Add zone failed:\n" + ex.getMessage()); }
    }

    private void updateZone() {
        if (selectedZoneId < 0) { parent.showError("Select a zone."); return; }
        if (!validateZoneForm()) return;
        try {
            ParkingZone z = buildZoneFromForm(); z.setZoneId(selectedZoneId);
            if (zoneDAO.updateZone(z)) { parent.setStatus("Zone updated."); clearZoneForm(); loadZones(); loadZoneCombo(); }
        } catch (SQLException ex) { parent.showError("Update failed:\n" + ex.getMessage()); }
    }

    private void deleteZone() {
        if (selectedZoneId < 0) { parent.showError("Select a zone."); return; }
        int c = JOptionPane.showConfirmDialog(parent,"Delete this zone and all its slots?","Confirm",JOptionPane.YES_NO_OPTION);
        if (c!=JOptionPane.YES_OPTION) return;
        try { zoneDAO.deleteZone(selectedZoneId); parent.setStatus("Zone deleted."); clearZoneForm(); loadZones(); loadZoneCombo(); }
        catch (SQLException ex) { parent.showError("Delete failed:\n" + ex.getMessage()); }
    }

    private void fillZoneForm() {
        int row = zoneTable.getSelectedRow(); if(row<0) return;
        selectedZoneId = (int) zoneModel.getValueAt(row,0);
        tfZoneName.setText((String)zoneModel.getValueAt(row,1));
        cbZoneType.setSelectedItem(zoneModel.getValueAt(row,2));
        tfLocation.setText(str(zoneModel.getValueAt(row,3)));
        spnTotalSlots.setValue(zoneModel.getValueAt(row,4));
        tfHourlyRate.setText(str(zoneModel.getValueAt(row,6)));
        tfDescription.setText(str(zoneModel.getValueAt(row,7)));
        // load slots for this zone
        loadSlotsForZone(selectedZoneId);
    }

    private void clearZoneForm() {
        selectedZoneId=-1; tfZoneName.setText(""); tfLocation.setText("");
        tfHourlyRate.setText("0.00"); tfDescription.setText("");
        spnTotalSlots.setValue(10); cbZoneType.setSelectedIndex(0);
        zoneTable.clearSelection();
    }

    private boolean validateZoneForm() {
        if (tfZoneName.getText().trim().isEmpty()) { parent.showError("Zone name required."); return false; }
        try { new BigDecimal(tfHourlyRate.getText().trim()); } catch (NumberFormatException e) { parent.showError("Invalid hourly rate."); return false; }
        return true;
    }

    private ParkingZone buildZoneFromForm() {
        ParkingZone z = new ParkingZone();
        z.setZoneName(tfZoneName.getText().trim());
        z.setZoneType((String)cbZoneType.getSelectedItem());
        z.setLocation(tfLocation.getText().trim());
        z.setTotalSlots((int)spnTotalSlots.getValue());
        z.setHourlyRate(new BigDecimal(tfHourlyRate.getText().trim()));
        z.setDescription(tfDescription.getText().trim());
        return z;
    }

    private void loadZones() {
        try {
            zoneModel.setRowCount(0);
            for (ParkingZone z : zoneDAO.getAllZones())
                zoneModel.addRow(new Object[]{z.getZoneId(),z.getZoneName(),z.getZoneType(),z.getLocation(),
                        z.getTotalSlots(),z.getAvailableSlots(),z.getHourlyRate(),z.getDescription()});
        } catch (SQLException ex) { parent.showError(ex.getMessage()); }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  SLOT PANEL
    // ════════════════════════════════════════════════════════════════════════
    private JPanel buildSlotPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(new TitledBorder("Parking Slots"));

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gc = gbc();

        cbSlotZone   = new JComboBox<>();
        tfSlotNumber = new JTextField(8);
        cbSlotType   = new JComboBox<>(new String[]{"Regular","Handicapped","EV","Reserved"});
        cbSlotStatus = new JComboBox<>(new String[]{"Available","Occupied","Reserved","Maintenance"});
        cbFilterStatus = new JComboBox<>(new String[]{"All","Available","Occupied","Reserved","Maintenance"});

        row(form, gc, 0, "Zone*:",       cbSlotZone);
        row(form, gc, 1, "Slot Number*:",tfSlotNumber);
        row(form, gc, 2, "Slot Type:",   cbSlotType);
        row(form, gc, 3, "Status:",      cbSlotStatus);
        row(form, gc, 4, "Filter Status:", cbFilterStatus);

        btnAddSlot    = btn("Add Slot",  new Color(34,139,34));
        btnUpdateSlot = btn("Update",    new Color(0,102,204));
        btnDeleteSlot = btn("Delete",    new Color(200,40,40));
        btnClearSlot  = btn("Clear",     Color.GRAY);
        JButton btnFilter = btn("Filter", new Color(120,60,180));

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        btns.add(btnAddSlot); btns.add(btnUpdateSlot); btns.add(btnDeleteSlot); btns.add(btnClearSlot); btns.add(btnFilter);
        gc.gridx=0; gc.gridy=5; gc.gridwidth=4; form.add(btns, gc);

        panel.add(form, BorderLayout.WEST);

        String[] cols = {"ID","Zone","Slot No","Type","Status"};
        slotModel = new DefaultTableModel(cols,0){ public boolean isCellEditable(int r,int c){return false;} };
        slotTable = new JTable(slotModel) {
            public Component prepareRenderer(TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r,row,col);
                String status = str(slotModel.getValueAt(row,4));
                if (!isRowSelected(row)) {
                    switch(status) {
                        case "Available":   c.setBackground(new Color(220,255,220)); break;
                        case "Occupied":    c.setBackground(new Color(255,220,220)); break;
                        case "Reserved":    c.setBackground(new Color(255,255,200)); break;
                        case "Maintenance": c.setBackground(new Color(220,220,220)); break;
                        default:            c.setBackground(Color.WHITE);
                    }
                }
                return c;
            }
        };
        styleTable(slotTable);
        slotTable.getSelectionModel().addListSelectionListener(e -> { if(!e.getValueIsAdjusting()) fillSlotForm(); });
        panel.add(new JScrollPane(slotTable), BorderLayout.CENTER);

        // Legend
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 2));
        legend.add(colorLabel("Available", new Color(220,255,220)));
        legend.add(colorLabel("Occupied",  new Color(255,220,220)));
        legend.add(colorLabel("Reserved",  new Color(255,255,200)));
        legend.add(colorLabel("Maintenance",new Color(220,220,220)));
        panel.add(legend, BorderLayout.SOUTH);

        btnAddSlot.addActionListener(e -> addSlot());
        btnUpdateSlot.addActionListener(e -> updateSlot());
        btnDeleteSlot.addActionListener(e -> deleteSlot());
        btnClearSlot.addActionListener(e -> clearSlotForm());
        btnFilter.addActionListener(e -> filterSlots());
        cbSlotZone.addActionListener(e -> {
            ZoneItem zi = (ZoneItem) cbSlotZone.getSelectedItem();
            if(zi!=null) loadSlotsForZone(zi.id);
        });

        return panel;
    }

    private void addSlot() {
        if(!validateSlotForm()) return;
        try {
            ParkingSlot s = buildSlotFromForm();
            if(slotDAO.addSlot(s)) { parent.setStatus("Slot added."); clearSlotForm(); ZoneItem zi=(ZoneItem)cbSlotZone.getSelectedItem(); if(zi!=null) loadSlotsForZone(zi.id); }
        } catch(SQLException ex){ parent.showError("Add slot failed:\n"+ex.getMessage()); }
    }

    private void updateSlot() {
        if(selectedSlotId<0){ parent.showError("Select a slot."); return; }
        if(!validateSlotForm()) return;
        try {
            ParkingSlot s = buildSlotFromForm(); s.setSlotId(selectedSlotId);
            if(slotDAO.updateSlot(s)){ parent.setStatus("Slot updated."); clearSlotForm(); ZoneItem zi=(ZoneItem)cbSlotZone.getSelectedItem(); if(zi!=null) loadSlotsForZone(zi.id); }
        } catch(SQLException ex){ parent.showError("Update failed:\n"+ex.getMessage()); }
    }

    private void deleteSlot() {
        if(selectedSlotId<0){ parent.showError("Select a slot."); return; }
        int c=JOptionPane.showConfirmDialog(parent,"Delete this slot?","Confirm",JOptionPane.YES_NO_OPTION);
        if(c!=JOptionPane.YES_OPTION) return;
        try{ slotDAO.deleteSlot(selectedSlotId); parent.setStatus("Slot deleted."); clearSlotForm(); ZoneItem zi=(ZoneItem)cbSlotZone.getSelectedItem(); if(zi!=null) loadSlotsForZone(zi.id); }
        catch(SQLException ex){ parent.showError(ex.getMessage()); }
    }

    private void filterSlots() {
        String status = (String) cbFilterStatus.getSelectedItem();
        try {
            slotModel.setRowCount(0);
            List<ParkingSlot> list = "All".equals(status) ? slotDAO.getAllSlots() : slotDAO.getSlotsByStatus(status);
            for(ParkingSlot s : list)
                slotModel.addRow(new Object[]{s.getSlotId(),s.getZoneName(),s.getSlotNumber(),s.getSlotType(),s.getStatus()});
            parent.setStatus(list.size()+" slot(s) shown.");
        } catch(SQLException ex){ parent.showError(ex.getMessage()); }
    }

    private void loadSlotsForZone(int zoneId) {
        try {
            slotModel.setRowCount(0);
            for(ParkingSlot s : slotDAO.getSlotsByZone(zoneId))
                slotModel.addRow(new Object[]{s.getSlotId(),s.getZoneName(),s.getSlotNumber(),s.getSlotType(),s.getStatus()});
        } catch(SQLException ex){ parent.showError(ex.getMessage()); }
    }

    private void fillSlotForm() {
        int row = slotTable.getSelectedRow(); if(row<0) return;
        selectedSlotId = (int) slotModel.getValueAt(row,0);
        tfSlotNumber.setText((String)slotModel.getValueAt(row,2));
        cbSlotType.setSelectedItem(slotModel.getValueAt(row,3));
        cbSlotStatus.setSelectedItem(slotModel.getValueAt(row,4));
    }

    private void clearSlotForm() {
        selectedSlotId=-1; tfSlotNumber.setText("");
        cbSlotType.setSelectedIndex(0); cbSlotStatus.setSelectedIndex(0);
        slotTable.clearSelection();
    }

    private boolean validateSlotForm() {
        if(cbSlotZone.getSelectedIndex()<0){ parent.showError("Select a zone."); return false; }
        if(tfSlotNumber.getText().trim().isEmpty()){ parent.showError("Slot number required."); return false; }
        return true;
    }

    private ParkingSlot buildSlotFromForm() {
        ParkingSlot s = new ParkingSlot();
        ZoneItem zi = (ZoneItem) cbSlotZone.getSelectedItem();
        if(zi!=null) s.setZoneId(zi.id);
        s.setSlotNumber(tfSlotNumber.getText().trim().toUpperCase());
        s.setSlotType((String)cbSlotType.getSelectedItem());
        s.setStatus((String)cbSlotStatus.getSelectedItem());
        return s;
    }

    private void loadZoneCombo() {
        cbSlotZone.removeAllItems();
        try { for(ParkingZone z : zoneDAO.getAllZones()) cbSlotZone.addItem(new ZoneItem(z.getZoneId(), z.getZoneName()+" ("+z.getZoneType()+")")); }
        catch(SQLException ex){ parent.showError(ex.getMessage()); }
    }

    // ── Shared ───────────────────────────────────────────────────────────────
    public void refresh() { loadZones(); loadZoneCombo(); }

    private GridBagConstraints gbc() {
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4,6,4,6); gc.fill=GridBagConstraints.HORIZONTAL; return gc;
    }
    private void row(JPanel p, GridBagConstraints gc, int r, String lbl, JComponent f) {
        gc.gridx=0; gc.gridy=r; gc.gridwidth=1; gc.weightx=0; p.add(new JLabel(lbl),gc);
        gc.gridx=1; gc.weightx=1; gc.gridwidth=3; p.add(f,gc);
    }
    private void styleTable(JTable t) {
        t.setRowHeight(22); t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setFont(new Font("Segoe UI",Font.PLAIN,12));
        t.getTableHeader().setFont(new Font("Segoe UI",Font.BOLD,12));
        t.setAutoCreateRowSorter(true);
    }
    private JButton btn(String text, Color bg) {
        JButton b=new JButton(text); b.setBackground(bg); b.setForeground(Color.WHITE);
        b.setOpaque(true); b.setBorderPainted(false);
        b.setFocusPainted(false); b.setFont(new Font("Segoe UI",Font.BOLD,12)); return b;
    }
    private JLabel colorLabel(String text, Color bg) {
        JLabel l = new JLabel("  "+text+"  "); l.setOpaque(true); l.setBackground(bg);
        l.setBorder(BorderFactory.createLineBorder(Color.GRAY)); return l;
    }
    private String str(Object o){ return o!=null?o.toString():""; }

    static class ZoneItem { final int id; final String label;
        ZoneItem(int id,String label){this.id=id;this.label=label;}
        public String toString(){return label;} }
}
