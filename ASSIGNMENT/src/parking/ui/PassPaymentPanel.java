package parking.ui;

import parking.dao.*;
import parking.model.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.List;

/**
 * PassPaymentPanel – issue/cancel parking passes and view all payments.
 * Uses two sub-tabs: Parking Passes and Payment History.
 */
public class PassPaymentPanel extends JPanel {

    private final MainFrame parent;
    private final PassDAO passDAO = new PassDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final UserDAO userDAO = new UserDAO();
    private final VehicleDAO vehicleDAO = new VehicleDAO();
    private final ParkingZoneDAO zoneDAO = new ParkingZoneDAO();

    // Pass widgets
    private JComboBox<UserItem>    cbUser;
    private JComboBox<VehicleItem> cbVehicle;
    private JComboBox<ZoneItem>    cbZone;
    private JComboBox<String>      cbPassType, cbPayMode;
    private JTextField tfStartDate, tfEndDate, tfPassFee;
    private JButton btnIssuePass, btnCancelPass, btnExpire;
    private JTable passTable;
    private DefaultTableModel passModel;
    private int selectedPassId = -1;

    // Payment widgets
    private JTable payTable;
    private DefaultTableModel payModel;

    public PassPaymentPanel(MainFrame parent) {
        this.parent = parent;
        setLayout(new BorderLayout());
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Parking Passes", buildPassTab());
        tabs.addTab("Payment History", buildPaymentTab());
        add(tabs, BorderLayout.CENTER);
        refresh();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PARKING PASS TAB
    // ════════════════════════════════════════════════════════════════════════
    private JPanel buildPassTab() {
        JPanel panel = new JPanel(new BorderLayout(6,6));
        panel.setBorder(new EmptyBorder(8,8,8,8));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new TitledBorder("Issue Parking Pass"));
        GridBagConstraints gc = gbc();

        cbUser     = new JComboBox<>();
        cbVehicle  = new JComboBox<>();
        cbZone     = new JComboBox<>();
        cbPassType = new JComboBox<>(new String[]{"Monthly","Semester","Annual"});
        cbPayMode  = new JComboBox<>(new String[]{"Cash","Card","UPI","Online"});

        java.time.LocalDate today = java.time.LocalDate.now();
        tfStartDate = new JTextField(today.toString(), 12);
        tfEndDate   = new JTextField(today.plusMonths(1).toString(), 12);
        tfPassFee   = new JTextField("500.00", 10);

        row(form,gc,0,"User*:",        cbUser);
        row(form,gc,1,"Vehicle*:",     cbVehicle);
        row(form,gc,2,"Zone*:",        cbZone);
        row(form,gc,3,"Pass Type:",    cbPassType);
        row(form,gc,4,"Start Date (yyyy-MM-dd)*:", tfStartDate);
        row(form,gc,5,"End Date (yyyy-MM-dd)*:",   tfEndDate);
        row(form,gc,6,"Pass Fee (₹)*:", tfPassFee);
        row(form,gc,7,"Payment Mode:", cbPayMode);

        btnIssuePass  = btn("Issue Pass",        new Color(34,139,34));
        btnCancelPass = btn("Cancel Selected",   new Color(200,40,40));
        btnExpire     = btn("Auto-Expire Passes",new Color(120,60,180));

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT,6,4));
        btns.add(btnIssuePass); btns.add(btnCancelPass); btns.add(btnExpire);
        gc.gridx=0;gc.gridy=8;gc.gridwidth=4; form.add(btns,gc);
        panel.add(form, BorderLayout.NORTH);

        // Table
        String[] cols = {"Pass ID","User","Vehicle","Zone","Type","Start","End","Fee (₹)","Status","Issued"};
        passModel = new DefaultTableModel(cols,0){public boolean isCellEditable(int r,int c){return false;}};
        passTable = new JTable(passModel);
        styleTable(passTable);
        passTable.getSelectionModel().addListSelectionListener(e->{
            if(!e.getValueIsAdjusting()){int row=passTable.getSelectedRow();selectedPassId=row>=0?(int)passModel.getValueAt(row,0):-1;}
        });
        panel.add(new JScrollPane(passTable), BorderLayout.CENTER);

        cbUser.addActionListener(e -> reloadVehicles());
        cbPassType.addActionListener(e -> autoSetEndDate());
        btnIssuePass.addActionListener(e -> issuePass());
        btnCancelPass.addActionListener(e -> cancelPass());
        btnExpire.addActionListener(e -> autoExpire());

        return panel;
    }

    private void issuePass() {
        if (cbUser.getSelectedIndex()<0)    {parent.showError("Select a user.");return;}
        if (cbVehicle.getSelectedIndex()<0) {parent.showError("Select a vehicle.");return;}
        if (cbZone.getSelectedIndex()<0)    {parent.showError("Select a zone.");return;}
        Date startDate, endDate; BigDecimal fee;
        try {
            startDate = Date.valueOf(tfStartDate.getText().trim());
            endDate   = Date.valueOf(tfEndDate.getText().trim());
            fee       = new BigDecimal(tfPassFee.getText().trim());
        } catch (Exception ex) {
            parent.showError("Invalid date or fee. Use yyyy-MM-dd and numeric fee."); return;
        }
        if (!endDate.after(startDate)){parent.showError("End date must be after start date.");return;}

        ParkingPass p = new ParkingPass();
        p.setUserId(((UserItem)cbUser.getSelectedItem()).id);
        p.setVehicleId(((VehicleItem)cbVehicle.getSelectedItem()).id);
        p.setZoneId(((ZoneItem)cbZone.getSelectedItem()).id);
        p.setPassType((String)cbPassType.getSelectedItem());
        p.setStartDate(startDate);
        p.setEndDate(endDate);
        p.setPassFee(fee);

        try {
            int passId = passDAO.addPass(p);
            // Record payment
            Payment pay = new Payment();
            pay.setUserId(p.getUserId());
            pay.setPassId(passId);
            pay.setAmount(fee);
            pay.setPaymentMode((String)cbPayMode.getSelectedItem());
            pay.setStatus("Paid");
            paymentDAO.addPayment(pay);

            parent.setStatus("Pass issued. ID = "+passId);
            JOptionPane.showMessageDialog(parent,"Pass issued successfully!\nPass ID: "+passId+"\nFee paid: ₹"+fee,"Pass Issued",JOptionPane.INFORMATION_MESSAGE);
            refresh();
        } catch (SQLException ex){ parent.showError("Issue pass failed:\n"+ex.getMessage()); }
    }

    private void cancelPass() {
        if(selectedPassId<0){parent.showError("Select a pass.");return;}
        int c=JOptionPane.showConfirmDialog(parent,"Cancel this pass?","Confirm",JOptionPane.YES_NO_OPTION);
        if(c!=JOptionPane.YES_OPTION) return;
        try{ passDAO.updatePassStatus(selectedPassId,"Cancelled"); parent.setStatus("Pass cancelled."); refresh(); }
        catch(SQLException ex){parent.showError(ex.getMessage());}
    }

    private void autoExpire() {
        try{ passDAO.expireOldPasses(); parent.setStatus("Expired passes updated."); refresh(); }
        catch(SQLException ex){parent.showError(ex.getMessage());}
    }

    private void autoSetEndDate() {
        try {
            java.time.LocalDate start = java.time.LocalDate.parse(tfStartDate.getText().trim());
            String type = (String)cbPassType.getSelectedItem();
            java.time.LocalDate end = switch(type) {
                case "Monthly"  -> start.plusMonths(1);
                case "Semester" -> start.plusMonths(6);
                case "Annual"   -> start.plusYears(1);
                default         -> start.plusMonths(1);
            };
            tfEndDate.setText(end.toString());
        } catch (Exception ignored){}
    }

    private void loadPasses() {
        try {
            passModel.setRowCount(0);
            for (ParkingPass p : passDAO.getAllPasses())
                passModel.addRow(new Object[]{p.getPassId(),p.getUserName(),p.getLicensePlate(),p.getZoneName(),
                        p.getPassType(),p.getStartDate(),p.getEndDate(),p.getPassFee(),p.getStatus(),p.getIssuedAt()});
        } catch (SQLException ex){parent.showError(ex.getMessage());}
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PAYMENT HISTORY TAB
    // ════════════════════════════════════════════════════════════════════════
    private JPanel buildPaymentTab() {
        JPanel panel = new JPanel(new BorderLayout(6,6));
        panel.setBorder(new EmptyBorder(8,8,8,8));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT,10,4));
        top.add(new JLabel("Payment History"));
        JButton btnRefresh = btn("Refresh", Color.DARK_GRAY);
        btnRefresh.addActionListener(e -> loadPayments());
        top.add(btnRefresh);
        panel.add(top, BorderLayout.NORTH);

        String[] cols = {"Pay ID","User","Session ID","Pass ID","Amount (₹)","Mode","Date","Status"};
        payModel = new DefaultTableModel(cols,0){public boolean isCellEditable(int r,int c){return false;}};
        payTable = new JTable(payModel);
        styleTable(payTable);
        panel.add(new JScrollPane(payTable), BorderLayout.CENTER);

        return panel;
    }

    private void loadPayments() {
        try {
            payModel.setRowCount(0);
            for (Payment p : paymentDAO.getAllPayments())
                payModel.addRow(new Object[]{p.getPaymentId(),p.getUserName(),p.getSessionId(),p.getPassId(),
                        p.getAmount(),p.getPaymentMode(),p.getPaymentDate(),p.getStatus()});
        } catch (SQLException ex){parent.showError(ex.getMessage());}
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private void reloadVehicles() {
        cbVehicle.removeAllItems();
        UserItem ui=(UserItem)cbUser.getSelectedItem(); if(ui==null) return;
        try { for(Vehicle v:vehicleDAO.getVehiclesByUser(ui.id)) cbVehicle.addItem(new VehicleItem(v.getVehicleId(),v.getLicensePlate()+" ("+v.getVehicleType()+")")); }
        catch(SQLException ex){parent.showError(ex.getMessage());}
    }

    private void loadCombos() {
        cbUser.removeAllItems(); cbZone.removeAllItems();
        try {
            for(User u:userDAO.getAllUsers()) cbUser.addItem(new UserItem(u.getUserId(),u.getFullName()+" ("+u.getUserType()+")"));
            for(ParkingZone z:zoneDAO.getAllZones()) cbZone.addItem(new ZoneItem(z.getZoneId(),z.getZoneName()));
        } catch(SQLException ex){parent.showError(ex.getMessage());}
        reloadVehicles();
    }

    public void refresh() { loadCombos(); loadPasses(); loadPayments(); }

    private GridBagConstraints gbc(){GridBagConstraints gc=new GridBagConstraints();gc.insets=new Insets(4,8,4,8);gc.fill=GridBagConstraints.HORIZONTAL;return gc;}
    private void row(JPanel p,GridBagConstraints gc,int r,String l,JComponent f){gc.gridx=0;gc.gridy=r;gc.gridwidth=1;gc.weightx=0;p.add(new JLabel(l),gc);gc.gridx=1;gc.weightx=1;gc.gridwidth=3;p.add(f,gc);}
    private void styleTable(JTable t){t.setRowHeight(22);t.setFont(new Font("Segoe UI",Font.PLAIN,12));t.getTableHeader().setFont(new Font("Segoe UI",Font.BOLD,12));t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);t.setAutoCreateRowSorter(true);}
    private JButton btn(String t,Color bg){JButton b=new JButton(t);b.setBackground(bg);b.setForeground(Color.WHITE);b.setOpaque(true);b.setBorderPainted(false);b.setFocusPainted(false);b.setFont(new Font("Segoe UI",Font.BOLD,12));return b;}

    static class UserItem    {final int id;final String l;UserItem(int i,String l){id=i;this.l=l;}public String toString(){return l;}}
    static class VehicleItem {final int id;final String l;VehicleItem(int i,String l){id=i;this.l=l;}public String toString(){return l;}}
    static class ZoneItem    {final int id;final String l;ZoneItem(int i,String l){id=i;this.l=l;}public String toString(){return l;}}
}
