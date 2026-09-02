package parking.ui;

import parking.dao.UserDAO;
import parking.dao.VehicleDAO;
import parking.model.User;
import parking.model.Vehicle;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.SQLException;
import java.util.List;

/**
 * UserVehiclePanel – split into two sub-tabs: Users and Vehicles.
 * Demonstrates: JTabbedPane, GridBagLayout, JTable, JScrollPane,
 * JComboBox, JTextField, JButton, input validation, event handling.
 */
public class UserVehiclePanel extends JPanel {

    private final MainFrame parent;
    private final UserDAO userDAO = new UserDAO();
    private final VehicleDAO vehicleDAO = new VehicleDAO();

    // ── User widgets ──
    private JTable userTable;
    private DefaultTableModel userModel;
    private JTextField tfUserName, tfEmail, tfPhone, tfIdNumber, tfAddress, tfUserSearch;
    private JComboBox<String> cbUserType;
    private JButton btnAddUser, btnUpdateUser, btnDeleteUser, btnClearUser, btnSearchUser;
    private int selectedUserId = -1;

    // ── Vehicle widgets ──
    private JTable vehicleTable;
    private DefaultTableModel vehicleModel;
    private JTextField tfPlate, tfMake, tfModel, tfColor, tfVehicleSearch;
    private JComboBox<String> cbVehicleType;
    private JComboBox<UserItem> cbVehicleUser;
    private JButton btnAddVehicle, btnUpdateVehicle, btnDeleteVehicle, btnClearVehicle, btnSearchVehicle;
    private int selectedVehicleId = -1;

    public UserVehiclePanel(MainFrame parent) {
        this.parent = parent;
        setLayout(new BorderLayout());
        JTabbedPane sub = new JTabbedPane();
        sub.addTab("Users", buildUserTab());
        sub.addTab("Vehicles", buildVehicleTab());
        add(sub, BorderLayout.CENTER);
        refresh();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  USER TAB
    // ════════════════════════════════════════════════════════════════════════
    private JPanel buildUserTab() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(new EmptyBorder(8, 8, 8, 8));

        // ── Form ──
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("User Details"));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 6, 4, 6);
        gc.fill = GridBagConstraints.HORIZONTAL;

        tfUserName = new JTextField(18);
        tfEmail    = new JTextField(18);
        tfPhone    = new JTextField(14);
        tfIdNumber = new JTextField(14);
        tfAddress  = new JTextField(24);
        cbUserType = new JComboBox<>(new String[]{"Student","Faculty","Staff","Visitor","Service"});

        addFormRow(form, gc, 0, "Full Name*:",  tfUserName);
        addFormRow(form, gc, 1, "Email*:",      tfEmail);
        addFormRow(form, gc, 2, "Phone*:",      tfPhone);
        addFormRow(form, gc, 3, "ID Number*:",  tfIdNumber);
        addFormRow(form, gc, 4, "User Type:",   cbUserType);
        addFormRow(form, gc, 5, "Address:",     tfAddress);

        // ── Buttons ──
        btnAddUser    = makeBtn("Add",    new Color(34, 139, 34));
        btnUpdateUser = makeBtn("Update", new Color(0, 102, 204));
        btnDeleteUser = makeBtn("Delete", new Color(200, 40, 40));
        btnClearUser  = makeBtn("Clear",  Color.GRAY);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        btns.add(btnAddUser); btns.add(btnUpdateUser);
        btns.add(btnDeleteUser); btns.add(btnClearUser);

        gc.gridx=0; gc.gridy=6; gc.gridwidth=4;
        form.add(btns, gc);

        // ── Search bar ──
        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        searchBar.add(new JLabel("Search:"));
        tfUserSearch = new JTextField(20);
        btnSearchUser = makeBtn("Go", new Color(0,102,204));
        JButton btnShowAll = makeBtn("Show All", Color.DARK_GRAY);
        searchBar.add(tfUserSearch); searchBar.add(btnSearchUser); searchBar.add(btnShowAll);

        JPanel top = new JPanel(new BorderLayout());
        top.add(form, BorderLayout.CENTER);
        top.add(searchBar, BorderLayout.SOUTH);
        panel.add(top, BorderLayout.NORTH);

        // ── Table ──
        String[] cols = {"ID","Name","Email","Phone","Type","ID Number","Address","Registered"};
        userModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c){ return false; }
        };
        userTable = new JTable(userModel);
        styleTable(userTable);
        userTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) populateUserForm();
        });
        panel.add(new JScrollPane(userTable), BorderLayout.CENTER);

        // ── Events ──
        btnAddUser.addActionListener(e -> addUser());
        btnUpdateUser.addActionListener(e -> updateUser());
        btnDeleteUser.addActionListener(e -> deleteUser());
        btnClearUser.addActionListener(e -> clearUserForm());
        btnSearchUser.addActionListener(e -> searchUsers());
        btnShowAll.addActionListener(e -> { tfUserSearch.setText(""); loadUsers(); });
        tfUserSearch.addActionListener(e -> searchUsers());

        return panel;
    }

    private void addUser() {
        if (!validateUserForm()) return;
        try {
            User u = buildUserFromForm();
            if (userDAO.addUser(u)) {
                parent.setStatus("User added successfully.");
                clearUserForm(); loadUsers();
            }
        } catch (SQLException ex) {
            parent.showError("Add user failed:\n" + ex.getMessage());
        }
    }

    private void updateUser() {
        if (selectedUserId < 0) { parent.showError("Select a user to update."); return; }
        if (!validateUserForm()) return;
        try {
            User u = buildUserFromForm();
            u.setUserId(selectedUserId);
            if (userDAO.updateUser(u)) {
                parent.setStatus("User updated."); clearUserForm(); loadUsers();
            }
        } catch (SQLException ex) {
            parent.showError("Update failed:\n" + ex.getMessage());
        }
    }

    private void deleteUser() {
        if (selectedUserId < 0) { parent.showError("Select a user to delete."); return; }
        int c = JOptionPane.showConfirmDialog(parent,
                "Delete this user and all associated vehicles/sessions?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (c != JOptionPane.YES_OPTION) return;
        try {
            userDAO.deleteUser(selectedUserId);
            parent.setStatus("User deleted."); clearUserForm(); loadUsers();
        } catch (SQLException ex) {
            parent.showError("Delete failed:\n" + ex.getMessage());
        }
    }

    private void searchUsers() {
        String kw = tfUserSearch.getText().trim();
        if (kw.isEmpty()) { loadUsers(); return; }
        try {
            List<User> list = userDAO.searchUsers(kw);
            populateUserTable(list);
            parent.setStatus(list.size() + " user(s) found.");
        } catch (SQLException ex) {
            parent.showError(ex.getMessage());
        }
    }

    private void loadUsers() {
        try { populateUserTable(userDAO.getAllUsers()); } catch (SQLException ex) { parent.showError(ex.getMessage()); }
    }

    private void populateUserTable(List<User> list) {
        userModel.setRowCount(0);
        for (User u : list)
            userModel.addRow(new Object[]{u.getUserId(), u.getFullName(), u.getEmail(),
                    u.getPhone(), u.getUserType(), u.getIdNumber(), u.getAddress(), u.getCreatedAt()});
    }

    private void populateUserForm() {
        int row = userTable.getSelectedRow();
        if (row < 0) return;
        selectedUserId = (int) userModel.getValueAt(row, 0);
        tfUserName.setText((String) userModel.getValueAt(row, 1));
        tfEmail.setText((String) userModel.getValueAt(row, 2));
        tfPhone.setText((String) userModel.getValueAt(row, 3));
        cbUserType.setSelectedItem(userModel.getValueAt(row, 4));
        tfIdNumber.setText((String) userModel.getValueAt(row, 5));
        Object addr = userModel.getValueAt(row, 6);
        tfAddress.setText(addr != null ? addr.toString() : "");
    }

    private boolean validateUserForm() {
        if (tfUserName.getText().trim().isEmpty()) { parent.showError("Full Name is required."); return false; }
        if (tfEmail.getText().trim().isEmpty())    { parent.showError("Email is required."); return false; }
        if (tfPhone.getText().trim().isEmpty())    { parent.showError("Phone is required."); return false; }
        if (tfIdNumber.getText().trim().isEmpty()) { parent.showError("ID Number is required."); return false; }
        if (!tfEmail.getText().trim().contains("@")) { parent.showError("Enter a valid email address."); return false; }
        return true;
    }

    private User buildUserFromForm() {
        User u = new User();
        u.setFullName(tfUserName.getText().trim());
        u.setEmail(tfEmail.getText().trim());
        u.setPhone(tfPhone.getText().trim());
        u.setUserType((String) cbUserType.getSelectedItem());
        u.setIdNumber(tfIdNumber.getText().trim());
        u.setAddress(tfAddress.getText().trim());
        return u;
    }

    private void clearUserForm() {
        selectedUserId = -1;
        tfUserName.setText(""); tfEmail.setText(""); tfPhone.setText("");
        tfIdNumber.setText(""); tfAddress.setText("");
        cbUserType.setSelectedIndex(0);
        userTable.clearSelection();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  VEHICLE TAB
    // ════════════════════════════════════════════════════════════════════════
    private JPanel buildVehicleTab() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(new EmptyBorder(8, 8, 8, 8));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder("Vehicle Details"));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 6, 4, 6);
        gc.fill = GridBagConstraints.HORIZONTAL;

        cbVehicleUser = new JComboBox<>();
        tfPlate       = new JTextField(14);
        cbVehicleType = new JComboBox<>(new String[]{"Car","Motorcycle","Truck","Bus","Bicycle","Other"});
        tfMake        = new JTextField(14);
        tfModel       = new JTextField(14);
        tfColor       = new JTextField(10);

        addFormRow(form, gc, 0, "Owner (User)*:", cbVehicleUser);
        addFormRow(form, gc, 1, "License Plate*:", tfPlate);
        addFormRow(form, gc, 2, "Vehicle Type:",   cbVehicleType);
        addFormRow(form, gc, 3, "Make:",           tfMake);
        addFormRow(form, gc, 4, "Model:",          tfModel);
        addFormRow(form, gc, 5, "Color:",          tfColor);

        btnAddVehicle    = makeBtn("Add",    new Color(34, 139, 34));
        btnUpdateVehicle = makeBtn("Update", new Color(0, 102, 204));
        btnDeleteVehicle = makeBtn("Delete", new Color(200, 40, 40));
        btnClearVehicle  = makeBtn("Clear",  Color.GRAY);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        btns.add(btnAddVehicle); btns.add(btnUpdateVehicle);
        btns.add(btnDeleteVehicle); btns.add(btnClearVehicle);
        gc.gridx=0; gc.gridy=6; gc.gridwidth=4;
        form.add(btns, gc);

        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        searchBar.add(new JLabel("Search:"));
        tfVehicleSearch = new JTextField(20);
        btnSearchVehicle = makeBtn("Go", new Color(0,102,204));
        JButton btnShowAll = makeBtn("Show All", Color.DARK_GRAY);
        searchBar.add(tfVehicleSearch); searchBar.add(btnSearchVehicle); searchBar.add(btnShowAll);

        JPanel top = new JPanel(new BorderLayout());
        top.add(form, BorderLayout.CENTER);
        top.add(searchBar, BorderLayout.SOUTH);
        panel.add(top, BorderLayout.NORTH);

        String[] cols = {"ID","Owner","License Plate","Type","Make","Model","Color","Registered"};
        vehicleModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c){ return false; }
        };
        vehicleTable = new JTable(vehicleModel);
        styleTable(vehicleTable);
        vehicleTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) populateVehicleForm();
        });
        panel.add(new JScrollPane(vehicleTable), BorderLayout.CENTER);

        btnAddVehicle.addActionListener(e -> addVehicle());
        btnUpdateVehicle.addActionListener(e -> updateVehicle());
        btnDeleteVehicle.addActionListener(e -> deleteVehicle());
        btnClearVehicle.addActionListener(e -> clearVehicleForm());
        btnSearchVehicle.addActionListener(e -> searchVehicles());
        btnShowAll.addActionListener(e -> { tfVehicleSearch.setText(""); loadVehicles(); });
        tfVehicleSearch.addActionListener(e -> searchVehicles());

        return panel;
    }

    private void addVehicle() {
        if (!validateVehicleForm()) return;
        try {
            Vehicle v = buildVehicleFromForm();
            if (vehicleDAO.addVehicle(v)) {
                parent.setStatus("Vehicle registered."); clearVehicleForm(); loadVehicles();
            }
        } catch (SQLException ex) {
            parent.showError("Add vehicle failed:\n" + ex.getMessage());
        }
    }

    private void updateVehicle() {
        if (selectedVehicleId < 0) { parent.showError("Select a vehicle to update."); return; }
        if (!validateVehicleForm()) return;
        try {
            Vehicle v = buildVehicleFromForm();
            v.setVehicleId(selectedVehicleId);
            if (vehicleDAO.updateVehicle(v)) {
                parent.setStatus("Vehicle updated."); clearVehicleForm(); loadVehicles();
            }
        } catch (SQLException ex) { parent.showError("Update failed:\n" + ex.getMessage()); }
    }

    private void deleteVehicle() {
        if (selectedVehicleId < 0) { parent.showError("Select a vehicle."); return; }
        int c = JOptionPane.showConfirmDialog(parent, "Delete this vehicle?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (c != JOptionPane.YES_OPTION) return;
        try {
            vehicleDAO.deleteVehicle(selectedVehicleId);
            parent.setStatus("Vehicle deleted."); clearVehicleForm(); loadVehicles();
        } catch (SQLException ex) { parent.showError("Delete failed:\n" + ex.getMessage()); }
    }

    private void searchVehicles() {
        String kw = tfVehicleSearch.getText().trim();
        if (kw.isEmpty()) { loadVehicles(); return; }
        try {
            List<Vehicle> list = vehicleDAO.searchVehicles(kw);
            populateVehicleTable(list);
            parent.setStatus(list.size() + " vehicle(s) found.");
        } catch (SQLException ex) { parent.showError(ex.getMessage()); }
    }

    private void loadVehicles() {
        try { populateVehicleTable(vehicleDAO.getAllVehicles()); } catch (SQLException ex) { parent.showError(ex.getMessage()); }
    }

    private void populateVehicleTable(List<Vehicle> list) {
        vehicleModel.setRowCount(0);
        for (Vehicle v : list)
            vehicleModel.addRow(new Object[]{v.getVehicleId(), v.getOwnerName(), v.getLicensePlate(),
                    v.getVehicleType(), v.getMake(), v.getModel(), v.getColor(), v.getRegisteredAt()});
    }

    private void populateVehicleForm() {
        int row = vehicleTable.getSelectedRow();
        if (row < 0) return;
        selectedVehicleId = (int) vehicleModel.getValueAt(row, 0);
        tfPlate.setText((String) vehicleModel.getValueAt(row, 2));
        cbVehicleType.setSelectedItem(vehicleModel.getValueAt(row, 3));
        tfMake.setText(str(vehicleModel.getValueAt(row, 4)));
        tfModel.setText(str(vehicleModel.getValueAt(row, 5)));
        tfColor.setText(str(vehicleModel.getValueAt(row, 6)));
    }

    private boolean validateVehicleForm() {
        if (cbVehicleUser.getSelectedIndex() < 0) { parent.showError("Select an owner."); return false; }
        if (tfPlate.getText().trim().isEmpty())    { parent.showError("License plate is required."); return false; }
        return true;
    }

    private Vehicle buildVehicleFromForm() {
        Vehicle v = new Vehicle();
        UserItem ui = (UserItem) cbVehicleUser.getSelectedItem();
        if (ui != null) v.setUserId(ui.id);
        v.setLicensePlate(tfPlate.getText().trim().toUpperCase());
        v.setVehicleType((String) cbVehicleType.getSelectedItem());
        v.setMake(tfMake.getText().trim());
        v.setModel(tfModel.getText().trim());
        v.setColor(tfColor.getText().trim());
        return v;
    }

    private void clearVehicleForm() {
        selectedVehicleId = -1;
        tfPlate.setText(""); tfMake.setText(""); tfModel.setText(""); tfColor.setText("");
        cbVehicleType.setSelectedIndex(0);
        if (cbVehicleUser.getItemCount() > 0) cbVehicleUser.setSelectedIndex(0);
        vehicleTable.clearSelection();
    }

    private void loadUserCombo() {
        cbVehicleUser.removeAllItems();
        try {
            for (User u : userDAO.getAllUsers())
                cbVehicleUser.addItem(new UserItem(u.getUserId(), u.getFullName() + " (" + u.getUserType() + ")"));
        } catch (SQLException ex) { parent.showError(ex.getMessage()); }
    }

    // ── Shared helpers ────────────────────────────────────────────────────────
    public void refresh() { loadUsers(); loadVehicles(); loadUserCombo(); }

    private void addFormRow(JPanel p, GridBagConstraints gc, int row, String label, JComponent field) {
        gc.gridx=0; gc.gridy=row; gc.gridwidth=1; gc.weightx=0;
        p.add(new JLabel(label), gc);
        gc.gridx=1; gc.weightx=1; gc.gridwidth=3;
        p.add(field, gc);
    }

    private void styleTable(JTable t) {
        t.setRowHeight(22);
        t.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        t.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setAutoCreateRowSorter(true);
    }

    private JButton makeBtn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setOpaque(true);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        return b;
    }

    private String str(Object o) { return o != null ? o.toString() : ""; }

    /** ComboBox item wrapper for users */
    static class UserItem {
        final int id; final String label;
        UserItem(int id, String label){ this.id=id; this.label=label; }
        public String toString(){ return label; }
    }
}
