package parking.ui;

import parking.dao.VehicleDAO;
import parking.dao.ViolationDAO;
import parking.model.Vehicle;
import parking.model.Violation;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

/**
 * ViolationPanel – record, search, update status and delete parking violations.
 */
public class ViolationPanel extends JPanel {

    private final MainFrame parent;
    private final ViolationDAO violationDAO = new ViolationDAO();
    private final VehicleDAO vehicleDAO = new VehicleDAO();

    private JTextField tfPlate, tfType, tfDescription, tfFine, tfSearch;
    private JComboBox<String> cbStatus;
    private JLabel lblVehicleInfo;
    private JButton btnAdd, btnMarkPaid, btnMarkWaived, btnDelete, btnSearch, btnShowAll;

    private JTable table;
    private DefaultTableModel tableModel;
    private int selectedViolationId = -1;

    private static final String[] VIOLATION_TYPES = {
        "No Parking Permit", "Wrong Zone", "Expired Pass", "Blocking Pathway",
        "Unauthorized Spot", "Overstay", "Double Parking", "Fire Lane Violation", "Other"
    };

    public ViolationPanel(MainFrame parent) {
        this.parent = parent;
        setLayout(new BorderLayout(6, 6));
        setBorder(new EmptyBorder(8, 8, 8, 8));
        add(buildForm(), BorderLayout.NORTH);
        add(buildTable(), BorderLayout.CENTER);
        refresh();
    }

    private JPanel buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new TitledBorder("Record Violation"));
        GridBagConstraints gc = gbc();

        tfPlate       = new JTextField(14);
        lblVehicleInfo = new JLabel(" ");
        lblVehicleInfo.setForeground(new Color(0, 100, 0));

        JComboBox<String> cbType = new JComboBox<>(VIOLATION_TYPES);
        cbType.setEditable(true);
        tfType        = new JTextField(20);
        tfDescription = new JTextField(30);
        tfFine        = new JTextField("500.00", 10);
        cbStatus      = new JComboBox<>(new String[]{"Pending","Paid","Waived"});

        // Row 0 – plate + lookup
        gc.gridx=0;gc.gridy=0;gc.weightx=0; form.add(new JLabel("License Plate*:"),gc);
        gc.gridx=1;gc.weightx=1; form.add(tfPlate,gc);
        JButton btnLookup = makeBtn("Lookup", new Color(0,102,204));
        gc.gridx=2;gc.weightx=0; form.add(btnLookup,gc);
        gc.gridx=3;gc.weightx=1; form.add(lblVehicleInfo,gc);

        // Row 1 – violation type
        gc.gridx=0;gc.gridy=1;gc.weightx=0; form.add(new JLabel("Violation Type*:"),gc);
        gc.gridx=1;gc.weightx=1;gc.gridwidth=3; form.add(cbType,gc);

        // Row 2 – description
        gc.gridx=0;gc.gridy=2;gc.gridwidth=1;gc.weightx=0; form.add(new JLabel("Description:"),gc);
        gc.gridx=1;gc.weightx=1;gc.gridwidth=3; form.add(tfDescription,gc);

        // Row 3 – fine + status
        gc.gridx=0;gc.gridy=3;gc.gridwidth=1;gc.weightx=0; form.add(new JLabel("Fine Amount (₹)*:"),gc);
        gc.gridx=1;gc.weightx=1; form.add(tfFine,gc);
        gc.gridx=2;gc.weightx=0; form.add(new JLabel("Status:"),gc);
        gc.gridx=3;gc.weightx=1; form.add(cbStatus,gc);

        // Row 4 – action buttons
        btnAdd        = makeBtn("Add Violation",  new Color(200,40,40));
        btnMarkPaid   = makeBtn("Mark Paid",      new Color(34,139,34));
        btnMarkWaived = makeBtn("Mark Waived",    new Color(180,130,0));
        btnDelete     = makeBtn("Delete",         Color.DARK_GRAY);

        JPanel actionBtns = new JPanel(new FlowLayout(FlowLayout.LEFT,6,0));
        actionBtns.add(btnAdd); actionBtns.add(btnMarkPaid); actionBtns.add(btnMarkWaived); actionBtns.add(btnDelete);
        gc.gridx=0;gc.gridy=4;gc.gridwidth=4; form.add(actionBtns,gc);

        // Row 5 – search
        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.LEFT,6,2));
        searchBar.add(new JLabel("Search (plate / type):"));
        tfSearch = new JTextField(20);
        btnSearch  = makeBtn("Go", new Color(0,102,204));
        btnShowAll = makeBtn("Show All", Color.DARK_GRAY);
        searchBar.add(tfSearch); searchBar.add(btnSearch); searchBar.add(btnShowAll);
        gc.gridx=0;gc.gridy=5;gc.gridwidth=4; form.add(searchBar,gc);

        // Events
        btnLookup.addActionListener(e -> lookupVehicle(tfPlate.getText().trim().toUpperCase()));
        btnAdd.addActionListener(e -> addViolation(cbType));
        btnMarkPaid.addActionListener(e -> updateStatus("Paid"));
        btnMarkWaived.addActionListener(e -> updateStatus("Waived"));
        btnDelete.addActionListener(e -> deleteViolation());
        btnSearch.addActionListener(e -> searchViolations());
        btnShowAll.addActionListener(e -> { tfSearch.setText(""); loadViolations(); });
        tfSearch.addActionListener(e -> searchViolations());

        return form;
    }

    private JScrollPane buildTable() {
        String[] cols = {"ID","License Plate","Slot","Type","Description","Fine (₹)","Date","Status"};
        tableModel = new DefaultTableModel(cols,0){public boolean isCellEditable(int r,int c){return false;}};
        table = new JTable(tableModel) {
            public Component prepareRenderer(TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r,row,col);
                if(!isRowSelected(row)) {
                    String s = str(tableModel.getValueAt(row,7));
                    switch(s){
                        case "Pending": c.setBackground(new Color(255,230,230)); break;
                        case "Paid":    c.setBackground(new Color(230,255,230)); break;
                        case "Waived":  c.setBackground(new Color(230,230,255)); break;
                        default:        c.setBackground(Color.WHITE);
                    }
                }
                return c;
            }
        };
        table.setRowHeight(22);
        table.setFont(new Font("Segoe UI",Font.PLAIN,12));
        table.getTableHeader().setFont(new Font("Segoe UI",Font.BOLD,12));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.getSelectionModel().addListSelectionListener(e -> {
            if(!e.getValueIsAdjusting()){int row=table.getSelectedRow();selectedViolationId=row>=0?(int)tableModel.getValueAt(row,0):-1;}
        });
        return new JScrollPane(table);
    }

    private void lookupVehicle(String plate) {
        if(plate.isEmpty()) return;
        try {
            Vehicle v = vehicleDAO.getVehicleByPlate(plate);
            if(v==null){lblVehicleInfo.setText("Vehicle not found.");return;}
            lblVehicleInfo.setText("Owner: "+v.getOwnerName()+" | Type: "+v.getVehicleType());
        } catch(SQLException ex){parent.showError(ex.getMessage());}
    }

    private void addViolation(JComboBox<String> cbType) {
        String plate = tfPlate.getText().trim().toUpperCase();
        String type  = str(cbType.getSelectedItem()).trim();
        if(plate.isEmpty()){parent.showError("Enter a license plate.");return;}
        if(type.isEmpty())  {parent.showError("Select a violation type.");return;}
        BigDecimal fine;
        try { fine = new BigDecimal(tfFine.getText().trim()); }
        catch(NumberFormatException e){parent.showError("Invalid fine amount.");return;}

        try {
            Vehicle v = vehicleDAO.getVehicleByPlate(plate);
            if(v==null){parent.showError("Vehicle not registered.");return;}
            Violation viol = new Violation();
            viol.setVehicleId(v.getVehicleId());
            viol.setViolationType(type);
            viol.setDescription(tfDescription.getText().trim());
            viol.setFineAmount(fine);
            if(violationDAO.addViolation(viol)){
                parent.setStatus("Violation recorded."); clearForm(); loadViolations();
            }
        } catch(SQLException ex){parent.showError("Add violation failed:\n"+ex.getMessage());}
    }

    private void updateStatus(String status) {
        if(selectedViolationId<0){parent.showError("Select a violation.");return;}
        try{violationDAO.updateViolationStatus(selectedViolationId,status);parent.setStatus("Violation status: "+status);loadViolations();}
        catch(SQLException ex){parent.showError(ex.getMessage());}
    }

    private void deleteViolation() {
        if(selectedViolationId<0){parent.showError("Select a violation.");return;}
        int c=JOptionPane.showConfirmDialog(parent,"Delete this violation record?","Confirm",JOptionPane.YES_NO_OPTION);
        if(c!=JOptionPane.YES_OPTION) return;
        try{violationDAO.deleteViolation(selectedViolationId);parent.setStatus("Violation deleted.");loadViolations();}
        catch(SQLException ex){parent.showError(ex.getMessage());}
    }

    private void searchViolations() {
        String kw = tfSearch.getText().trim();
        if(kw.isEmpty()){loadViolations();return;}
        try {
            List<Violation> list = violationDAO.searchViolations(kw);
            populateTable(list);
            parent.setStatus(list.size()+" violation(s) found.");
        } catch(SQLException ex){parent.showError(ex.getMessage());}
    }

    private void loadViolations() {
        try { populateTable(violationDAO.getAllViolations()); }
        catch(SQLException ex){parent.showError(ex.getMessage());}
    }

    private void populateTable(List<Violation> list) {
        tableModel.setRowCount(0);
        for(Violation v:list)
            tableModel.addRow(new Object[]{v.getViolationId(),v.getLicensePlate(),v.getSlotNumber(),
                    v.getViolationType(),v.getDescription(),v.getFineAmount(),v.getViolationDate(),v.getStatus()});
    }

    private void clearForm() {
        tfPlate.setText(""); tfDescription.setText(""); tfFine.setText("500.00");
        lblVehicleInfo.setText(" "); cbStatus.setSelectedIndex(0);
        table.clearSelection(); selectedViolationId=-1;
    }

    public void refresh() { loadViolations(); }

    private GridBagConstraints gbc(){GridBagConstraints gc=new GridBagConstraints();gc.insets=new Insets(4,8,4,8);gc.fill=GridBagConstraints.HORIZONTAL;return gc;}
    private JButton makeBtn(String t,Color bg){JButton b=new JButton(t);b.setBackground(bg);b.setForeground(Color.WHITE);b.setOpaque(true);b.setBorderPainted(false);b.setFocusPainted(false);b.setFont(new Font("Segoe UI",Font.BOLD,12));return b;}
    private String str(Object o){return o!=null?o.toString():"";}
}
