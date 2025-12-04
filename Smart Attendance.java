import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

public class SmartAttendance {
    // File paths
    private static final String STUDENTS_FILE = "students.csv";
    private static final String ATTENDANCE_FILE = "attendance.csv";

    // Default credentials
    private static final String ADMIN_USER = "admin";
    private static final String ADMIN_PASS = "admin123";

    private JFrame frame;
    private CardLayout cardLayout;
    private JPanel cards;

    // Dashboard components
    private DefaultTableModel studentsTableModel;
    private JTable studentsTable;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                // set system look and feel
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
            new SmartAttendance().createAndShowGUI();
        });
    }

    private void createAndShowGUI() {
        ensureDataFilesExist();

        frame = new JFrame("Smart Attendance Management System");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 600);
        frame.setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        cards = new JPanel(cardLayout);

        cards.add(createLoginPanel(), "login");
        cards.add(createDashboardPanel(), "dashboard");

        frame.getContentPane().add(cards);
        frame.setVisible(true);

        cardLayout.show(cards, "login");
    }

    private JPanel createLoginPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 8, 8, 8);

        JLabel title = new JLabel("Smart Attendance Login");
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        p.add(title, c);

        c.gridwidth = 1;

        c.gridy = 1; c.gridx = 0; p.add(new JLabel("Username:"), c);
        JTextField userField = new JTextField(15);
        c.gridx = 1; p.add(userField, c);

        c.gridy = 2; c.gridx = 0; p.add(new JLabel("Password:"), c);
        JPasswordField passField = new JPasswordField(15);
        c.gridx = 1; p.add(passField, c);

        JButton loginBtn = new JButton("Login");
        c.gridy = 3; c.gridx = 0; c.gridwidth = 2;
        p.add(loginBtn, c);

        JLabel info = new JLabel("Default credentials: admin / admin123");
        info.setForeground(Color.DARK_GRAY);
        c.gridy = 4; p.add(info, c);

        loginBtn.addActionListener(e -> {
            String user = userField.getText().trim();
            String pass = new String(passField.getPassword());
            if (ADMIN_USER.equals(user) && ADMIN_PASS.equals(pass)) {
                refreshStudentTable();
                cardLayout.show(cards, "dashboard");
            } else {
                JOptionPane.showMessageDialog(frame, "Incorrect username or password.", "Login Failed", JOptionPane.ERROR_MESSAGE);
            }
        });

        return p;
    }

    private JPanel createDashboardPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // Top toolbar
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addStudentBtn = new JButton("Add Student");
        JButton removeStudentBtn = new JButton("Remove Selected");
        JButton markAttendanceBtn = new JButton("Mark Attendance");
        JButton genReportBtn = new JButton("Generate Report");
        JButton logoutBtn = new JButton("Logout");

        top.add(addStudentBtn);
        top.add(removeStudentBtn);
        top.add(markAttendanceBtn);
        top.add(genReportBtn);
        top.add(logoutBtn);

        panel.add(top, BorderLayout.NORTH);

        // Student table
        String[] cols = {"ID", "Name", "Year", "Department"};
        studentsTableModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int row, int column) { return false; }
        };
        studentsTable = new JTable(studentsTableModel);
        studentsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scroll = new JScrollPane(studentsTable);
        panel.add(scroll, BorderLayout.CENTER);

        // Bottom status
        JLabel status = new JLabel("Ready");
        panel.add(status, BorderLayout.SOUTH);

        // Button actions
        addStudentBtn.addActionListener(e -> showAddStudentDialog());
        removeStudentBtn.addActionListener(e -> removeSelectedStudent());
        markAttendanceBtn.addActionListener(e -> showMarkAttendanceDialog());
        genReportBtn.addActionListener(e -> showReportDialog());
        logoutBtn.addActionListener(e -> cardLayout.show(cards, "login"));

        return panel;
    }

    // ----------------- Student methods -----------------
    private void showAddStudentDialog() {
        JTextField idField = new JTextField(10);
        JTextField nameField = new JTextField(20);
        JTextField yearField = new JTextField(6);
        JTextField deptField = new JTextField(12);

        JPanel p = new JPanel(new GridLayout(0,2,6,6));
        p.add(new JLabel("Student ID:")); p.add(idField);
        p.add(new JLabel("Name:")); p.add(nameField);
        p.add(new JLabel("Year:")); p.add(yearField);
        p.add(new JLabel("Department:")); p.add(deptField);

        int res = JOptionPane.showConfirmDialog(frame, p, "Add Student", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res == JOptionPane.OK_OPTION) {
            String id = idField.getText().trim();
            String name = nameField.getText().trim();
            String year = yearField.getText().trim();
            String dept = deptField.getText().trim();
            if (id.isEmpty() || name.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "ID and Name required.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (studentExists(id)) {
                JOptionPane.showMessageDialog(frame, "Student ID already exists.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            appendLineToFile(STUDENTS_FILE, String.join(",", escapeCSV(id), escapeCSV(name), escapeCSV(year), escapeCSV(dept)));
            refreshStudentTable();
        }
    }

    private boolean studentExists(String id) {
        List<String[]> students = readStudents();
        for (String[] s : students) if (s[0].equals(id)) return true;
        return false;
    }

    private void removeSelectedStudent() {
        int row = studentsTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(frame, "Select a student to remove.", "Remove", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String id = studentsTableModel.getValueAt(row, 0).toString();
        int confirm = JOptionPane.showConfirmDialog(frame, "Remove student ID " + id + "?","Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            removeStudentById(id);
            refreshStudentTable();
        }
    }

    private void removeStudentById(String id) {
        List<String[]> students = readStudents();
        List<String> newLines = new ArrayList<>();
        for (String[] s : students) {
            if (!s[0].equals(id)) newLines.add(String.join(",", escapeCSV(s[0]), escapeCSV(s[1]), escapeCSV(s[2]), escapeCSV(s[3])));
        }
        writeLinesToFile(STUDENTS_FILE, newLines);
        // Also remove attendance related to this student
        List<String[]> atts = readAttendance();
        List<String> attLines = new ArrayList<>();
        for (String[] a : atts) {
            if (!a[1].equals(id)) attLines.add(String.join(",", escapeCSV(a[0]), escapeCSV(a[1]), escapeCSV(a[2])));
        }
        writeLinesToFile(ATTENDANCE_FILE, attLines);
    }

    private void refreshStudentTable() {
        studentsTableModel.setRowCount(0);
        for (String[] s : readStudents()) {
            studentsTableModel.addRow(new Object[]{s[0], s[1], s[2], s[3]});
        }
    }

    // ----------------- Attendance methods -----------------
    private void showMarkAttendanceDialog() {
        List<String[]> students = readStudents();
        if (students.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "No students present. Add students first.", "Mark Attendance", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JPanel panel = new JPanel(new BorderLayout(8,8));
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controls.add(new JLabel("Date:"));
        SpinnerDateModel sdm = new SpinnerDateModel(new Date(), null, null, Calendar.DAY_OF_MONTH);
        JSpinner dateSpinner = new JSpinner(sdm);
        dateSpinner.setEditor(new JSpinner.DateEditor(dateSpinner, "yyyy-MM-dd"));
        controls.add(dateSpinner);
        panel.add(controls, BorderLayout.NORTH);

        String[] cols = {"ID","Name","Status"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int row, int column) { return column==2; }
        };
        for (String[] s : students) model.addRow(new Object[]{s[0], s[1], "Present"});
        JTable tbl = new JTable(model);
        panel.add(new JScrollPane(tbl), BorderLayout.CENTER);

        int res = JOptionPane.showConfirmDialog(frame, panel, "Mark Attendance", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res == JOptionPane.OK_OPTION) {
            Date d = (Date) dateSpinner.getValue();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            String dateStr = sdf.format(d);
            // Save each student's status
            List<String> lines = new ArrayList<>();
            for (int i=0;i<model.getRowCount();i++) {
                String id = model.getValueAt(i,0).toString();
                String status = model.getValueAt(i,2).toString();
                lines.add(String.join(",", escapeCSV(dateStr), escapeCSV(id), escapeCSV(status)));
            }
            // Append; but first remove existing records for same date and student to avoid duplicates
            List<String[]> existing = readAttendance();
            List<String> combined = new ArrayList<>();
            // keep attendance that is not for this date
            for (String[] a : existing) {
                if (!a[0].equals(dateStr)) combined.add(String.join(",", escapeCSV(a[0]), escapeCSV(a[1]), escapeCSV(a[2])));
            }
            combined.addAll(lines);
            writeLinesToFile(ATTENDANCE_FILE, combined);
            JOptionPane.showMessageDialog(frame, "Attendance saved for " + dateStr);
        }
    }

    private void showReportDialog() {
        String[] options = {"Daily Report (choose a date)", "Monthly Report (choose month)", "Export All Attendance CSV"};
        int choice = JOptionPane.showOptionDialog(frame, "Choose report type:", "Generate Report",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
        if (choice == 0) {
            SpinnerDateModel sdm = new SpinnerDateModel(new Date(), null, null, Calendar.DAY_OF_MONTH);
            JSpinner dateSpinner = new JSpinner(sdm);
            dateSpinner.setEditor(new JSpinner.DateEditor(dateSpinner, "yyyy-MM-dd"));
            int res = JOptionPane.showConfirmDialog(frame, dateSpinner, "Select Date", JOptionPane.OK_CANCEL_OPTION);
            if (res == JOptionPane.OK_OPTION) {
                Date d = (Date) dateSpinner.getValue();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                String dateStr = sdf.format(d);
                showDailyReport(dateStr);
            }
        } else if (choice == 1) {
            JPanel p = new JPanel(new FlowLayout());
            SpinnerNumberModel monthModel = new SpinnerNumberModel(Calendar.getInstance().get(Calendar.MONTH)+1, 1, 12, 1);
            JSpinner monthSpinner = new JSpinner(monthModel);
            SpinnerNumberModel yearModel = new SpinnerNumberModel(Calendar.getInstance().get(Calendar.YEAR), 2000, 3000, 1);
            JSpinner yearSpinner = new JSpinner(yearModel);
            p.add(new JLabel("Month (1-12):")); p.add(monthSpinner);
            p.add(new JLabel("Year:")); p.add(yearSpinner);
            int res = JOptionPane.showConfirmDialog(frame, p, "Select Month", JOptionPane.OK_CANCEL_OPTION);
            if (res == JOptionPane.OK_OPTION) {
                int month = (Integer) monthSpinner.getValue();
                int year = (Integer) yearSpinner.getValue();
                showMonthlyReport(month, year);
            }
        } else if (choice == 2) {
            JOptionPane.showMessageDialog(frame, "CSV saved at: " + ATTENDANCE_FILE);
        }
    }

    private void showDailyReport(String dateStr) {
        List<String[]> students = readStudents();
        List<String[]> atts = readAttendance();
        Map<String,String> map = new HashMap<>();
        for (String[] a: atts) {
            if (a[0].equals(dateStr)) map.put(a[1], a[2]);
        }
        String[] cols = {"ID","Name","Status"};
        DefaultTableModel model = new DefaultTableModel(cols,0) { public boolean isCellEditable(int r,int c){return false;} };
        for (String[] s: students) {
            String st = map.getOrDefault(s[0], "Absent");
            model.addRow(new Object[]{s[0], s[1], st});
        }
        JTable tbl = new JTable(model);
        JScrollPane sp = new JScrollPane(tbl);
        sp.setPreferredSize(new Dimension(600, 400));
        int res = JOptionPane.showConfirmDialog(frame, sp, "Daily Report - " + dateStr, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res == JOptionPane.OK_OPTION) {
            // export to CSV
            String out = "report_daily_" + dateStr + ".csv";
            List<String> lines = new ArrayList<>();
            lines.add("ID,Name,Status");
            for (int i=0;i<model.getRowCount();i++) lines.add(String.join(",", escapeCSV(model.getValueAt(i,0).toString()), escapeCSV(model.getValueAt(i,1).toString()), escapeCSV(model.getValueAt(i,2).toString())));
            writeLinesToFile(out, lines);
            JOptionPane.showMessageDialog(frame, "Daily report exported to " + out);
        }
    }

    private void showMonthlyReport(int month, int year) {
        List<String[]> students = readStudents();
        List<String[]> atts = readAttendance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Map<String, Integer> presentCount = new HashMap<>();
        Map<String, Integer> totalCount = new HashMap<>();
        for (String[] s : students) { presentCount.put(s[0], 0); totalCount.put(s[0], 0); }
        for (String[] a : atts) {
            try {
                Date d = sdf.parse(a[0]);
                Calendar cal = Calendar.getInstance(); cal.setTime(d);
                int m = cal.get(Calendar.MONTH) + 1; int y = cal.get(Calendar.YEAR);
                if (m == month && y == year) {
                    totalCount.put(a[1], totalCount.getOrDefault(a[1], 0) + 1);
                    if ("Present".equalsIgnoreCase(a[2])) presentCount.put(a[1], presentCount.getOrDefault(a[1],0) + 1);
                }
            } catch (Exception ex) { /* ignore parse errors */ }
        }
        String[] cols = {"ID","Name","PresentDays","TotalMarked","%"};
        DefaultTableModel model = new DefaultTableModel(cols,0) { public boolean isCellEditable(int r,int c){return false;} };
        for (String[] s : students) {
            int p = presentCount.getOrDefault(s[0], 0);
            int t = totalCount.getOrDefault(s[0], 0);
            double pct = (t==0)?0: (100.0 * p / t);
            model.addRow(new Object[]{s[0], s[1], p, t, String.format("%.1f", pct)});
        }
        JTable tbl = new JTable(model);
        JScrollPane sp = new JScrollPane(tbl);
        sp.setPreferredSize(new Dimension(700, 400));
        int res = JOptionPane.showConfirmDialog(frame, sp, String.format("Monthly Report - %02d/%04d", month, year), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res == JOptionPane.OK_OPTION) {
            String out = String.format("report_monthly_%04d_%02d.csv", year, month);
            List<String> lines = new ArrayList<>();
            lines.add("ID,Name,PresentDays,TotalMarked,Percent");
            for (int i=0;i<model.getRowCount();i++) lines.add(String.join(",", escapeCSV(model.getValueAt(i,0).toString()), escapeCSV(model.getValueAt(i,1).toString()), escapeCSV(model.getValueAt(i,2).toString()), escapeCSV(model.getValueAt(i,3).toString()), escapeCSV(model.getValueAt(i,4).toString())));
            writeLinesToFile(out, lines);
            JOptionPane.showMessageDialog(frame, "Monthly report exported to " + out);
        }
    }

    // ----------------- File helpers -----------------
    private void ensureDataFilesExist() {
        try {
            if (!Files.exists(Paths.get(STUDENTS_FILE))) Files.createFile(Paths.get(STUDENTS_FILE));
            if (!Files.exists(Paths.get(ATTENDANCE_FILE))) Files.createFile(Paths.get(ATTENDANCE_FILE));
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Error initializing data files: " + e.getMessage());
            System.exit(1);
        }
    }

    private List<String[]> readStudents() {
        List<String[]> out = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(STUDENTS_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = parseCSVLine(line, 4);
                if (parts.length < 4) parts = Arrays.copyOf(parts, 4);
                for (int i=0;i<4;i++) if (parts[i]==null) parts[i] = "";
                out.add(parts);
            }
        } catch (IOException ignored) {}
        return out;
    }

    private List<String[]> readAttendance() {
        List<String[]> out = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(ATTENDANCE_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = parseCSVLine(line, 3);
                if (parts.length < 3) parts = Arrays.copyOf(parts, 3);
                for (int i=0;i<3;i++) if (parts[i]==null) parts[i] = "";
                out.add(parts);
            }
        } catch (IOException ignored) {}
        return out;
    }

    private void appendLineToFile(String file, String line) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, true))) {
            bw.write(line);
            bw.newLine();
        } catch (IOException e) { JOptionPane.showMessageDialog(frame, "Error writing file: " + e.getMessage()); }
    }

    private void writeLinesToFile(String file, List<String> lines) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, false))) {
            for (String l : lines) { bw.write(l); bw.newLine(); }
        } catch (IOException e) { JOptionPane.showMessageDialog(frame, "Error writing file: " + e.getMessage()); }
    }

    // ----------------- Simple CSV helpers -----------------
    private static String escapeCSV(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            s = s.replace("\"", "\"\"");
            return "\"" + s + "\"";
        }
        return s;
    }

    private static String[] parseCSVLine(String line, int expected) {
        List<String> cells = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        char[] ch = line.toCharArray();
        for (int i=0;i<ch.length;i++) {
            char c = ch[i];
            if (inQuotes) {
                if (c == '"') {
                    if (i+1 < ch.length && ch[i+1] == '"') { cur.append('"'); i++; }
                    else inQuotes = false;
                } else cur.append(c);
            } else {
                if (c == ',') { cells.add(cur.toString()); cur.setLength(0); }
                else if (c == '"') inQuotes = true;
                else cur.append(c);
            }
        }
        cells.add(cur.toString());
        // pad or trim
        while (cells.size() < expected) cells.add("");
        return cells.toArray(new String[0]);
    }
}
