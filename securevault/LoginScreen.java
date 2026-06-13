package securevault;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;

public class LoginScreen extends JFrame {

    private JPasswordField passwordField;
    private JButton unlockButton;
    private JLabel statusLabel;
    private PasswordManager passwordManager;
    private static final String VAULT_FILE = VaultManager.getVaultFilePath();
    private int failedAttempts = 0;

    // Same light palette as dashboard
    static final Color ACCENT   = new Color(108, 93, 211);
    static final Color BG_LEFT  = new Color(108, 93, 211);
    static final Color BG_RIGHT = Color.WHITE;
    static final Color TEXT_DARK = new Color(30, 30, 50);
    static final Color TEXT_MID  = new Color(120, 120, 150);

    public LoginScreen() {
        passwordManager = new PasswordManager(VAULT_FILE);
        initUI();
    }

    private void initUI() {
        setTitle("SecureVault");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(820, 580);
        setLocationRelativeTo(null);
        setResizable(false);
        setUndecorated(true);

        // Root: left panel (purple) + right panel (white form)
        JPanel root = new JPanel(new GridLayout(1, 2));
        root.setBorder(new LineBorder(new Color(200, 195, 235), 1));

        // ── LEFT: branding panel ──
        JPanel left = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // gradient purple bg
                g2.setPaint(new GradientPaint(0, 0, new Color(88, 70, 190), getWidth(), getHeight(), new Color(130, 100, 230)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                // decorative circles
                g2.setColor(new Color(255, 255, 255, 18));
                g2.fillOval(-60, -60, 280, 280);
                g2.fillOval(getWidth() - 100, getHeight() - 120, 220, 220);
                g2.setColor(new Color(255, 255, 255, 10));
                g2.fillOval(40, getHeight() - 160, 180, 180);
                g2.dispose();
            }
        };
        left.setLayout(new GridBagLayout());

        JPanel leftContent = new JPanel();
        leftContent.setOpaque(false);
        leftContent.setLayout(new BoxLayout(leftContent, BoxLayout.Y_AXIS));
        leftContent.setBorder(BorderFactory.createEmptyBorder(0, 30, 0, 30));

        // Shield icon
        JLabel shieldIcon = new JLabel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 40));
                g2.fillRoundRect(0, 0, 80, 80, 20, 20);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 42));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString("🔐", (80 - fm.stringWidth("🔐")) / 2, 58);
                g2.dispose();
            }
        };
        shieldIcon.setPreferredSize(new Dimension(80, 80));
        shieldIcon.setMaximumSize(new Dimension(80, 80));
        shieldIcon.setAlignmentX(CENTER_ALIGNMENT);

        JLabel appName = new JLabel("SecureVault");
        appName.setFont(new Font("Segoe UI", Font.BOLD, 30));
        appName.setForeground(Color.WHITE);
        appName.setAlignmentX(CENTER_ALIGNMENT);

        JLabel tagline = new JLabel("Your private vault");
        tagline.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tagline.setForeground(new Color(255, 255, 255, 160));
        tagline.setAlignmentX(CENTER_ALIGNMENT);

        leftContent.add(shieldIcon);
        leftContent.add(Box.createVerticalStrut(20));
        leftContent.add(appName);
        leftContent.add(Box.createVerticalStrut(8));
        leftContent.add(tagline);
        leftContent.add(Box.createVerticalStrut(40));

        // Feature bullets
        String[][] features = {
            {"🔒", "AES-256 Encryption"},
            {"📁", "All file types supported"},
            {"🛡", "Password protected"},
        };
        for (String[] f : features) {
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
            row.setOpaque(false);
            row.setMaximumSize(new Dimension(260, 36));
            row.setAlignmentX(CENTER_ALIGNMENT);
            JLabel ic = new JLabel(f[0]);
            ic.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
            JLabel tx = new JLabel(f[1]);
            tx.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            tx.setForeground(new Color(255, 255, 255, 200));
            row.add(ic); row.add(tx);
            leftContent.add(row);
        }

        left.add(leftContent);

        // ── RIGHT: form panel ──
        JPanel right = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        right.setLayout(null); // absolute layout for precise placement

        // Drag support on right panel
        MouseAdapter drag = new MouseAdapter() {
            int sx, sy;
            public void mousePressed(MouseEvent e)  { sx = e.getX(); sy = e.getY(); }
            public void mouseDragged(MouseEvent e)  { setLocation(getX()+e.getX()-sx, getY()+e.getY()-sy); }
        };
        right.addMouseListener(drag);
        right.addMouseMotionListener(drag);

        // ── X close button top-right ──
        JButton closeBtn = new JButton("✕") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isRollover()) {
                    g2.setColor(new Color(239, 68, 68));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                    g2.setColor(Color.WHITE);
                } else {
                    g2.setColor(new Color(220, 215, 240));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                    g2.setColor(new Color(100, 100, 130));
                }
                g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString("✕", (getWidth()-fm.stringWidth("✕"))/2, getHeight()/2+fm.getAscent()/2-1);
                g2.dispose();
            }
        };
        closeBtn.setBounds(360, 16, 32, 32);
        closeBtn.setContentAreaFilled(false); closeBtn.setBorderPainted(false); closeBtn.setFocusPainted(false);
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.addActionListener(e -> System.exit(0));
        right.add(closeBtn);

        // Minimize button
        JButton minBtn = new JButton("─") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isRollover()) {
                    g2.setColor(new Color(234, 179, 8));
                    g2.fillRoundRect(0,0,getWidth(),getHeight(),8,8);
                    g2.setColor(Color.WHITE);
                } else {
                    g2.setColor(new Color(220, 215, 240));
                    g2.fillRoundRect(0,0,getWidth(),getHeight(),8,8);
                    g2.setColor(new Color(100,100,130));
                }
                g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString("─",(getWidth()-fm.stringWidth("─"))/2, getHeight()/2+fm.getAscent()/2-1);
                g2.dispose();
            }
        };
        minBtn.setBounds(322, 16, 32, 32);
        minBtn.setContentAreaFilled(false); minBtn.setBorderPainted(false); minBtn.setFocusPainted(false);
        minBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        minBtn.addActionListener(e -> setState(Frame.ICONIFIED));
        right.add(minBtn);

        // Welcome text
        JLabel welcome = new JLabel(passwordManager.isPasswordSet() ? "Welcome back!" : "Create your vault");
        welcome.setFont(new Font("Segoe UI", Font.BOLD, 26));
        welcome.setForeground(TEXT_DARK);
        welcome.setBounds(40, 80, 330, 36);
        right.add(welcome);

        JLabel subtext = new JLabel(passwordManager.isPasswordSet() ? "Enter your password to access vault" : "Set a strong password to protect your files");
        subtext.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtext.setForeground(TEXT_MID);
        subtext.setBounds(40, 122, 330, 22);
        right.add(subtext);

        // Divider line
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(235, 232, 250));
        sep.setBounds(40, 156, 330, 2);
        right.add(sep);

        // Password label
        JLabel passLabel = new JLabel("Password");
        passLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        passLabel.setForeground(TEXT_DARK);
        passLabel.setBounds(40, 172, 200, 20);
        right.add(passLabel);

        // Password field with rounded border
        passwordField = new JPasswordField();
        passwordField.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        passwordField.setForeground(TEXT_DARK);
        passwordField.setBackground(new Color(247, 245, 255));
        passwordField.setCaretColor(ACCENT);
        passwordField.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(210, 205, 240), 1, true),
            BorderFactory.createEmptyBorder(10, 14, 10, 14)
        ));
        passwordField.setEchoChar('●');
        passwordField.setBounds(40, 198, 330, 46);
        right.add(passwordField);

        // Confirm field (first time)
        JPasswordField confirmField = new JPasswordField();
        int confirmBottom = 198;
        if (!passwordManager.isPasswordSet()) {
            JLabel confLabel = new JLabel("Confirm Password");
            confLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
            confLabel.setForeground(TEXT_DARK);
            confLabel.setBounds(40, 256, 200, 20);
            right.add(confLabel);

            confirmField.setFont(new Font("Segoe UI", Font.PLAIN, 15));
            confirmField.setForeground(TEXT_DARK);
            confirmField.setBackground(new Color(247, 245, 255));
            confirmField.setCaretColor(ACCENT);
            confirmField.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(210, 205, 240), 1, true),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)
            ));
            confirmField.setEchoChar('●');
            confirmField.setBounds(40, 280, 330, 46);
            right.add(confirmField);
            confirmBottom = 280;
        }

        int btnY = passwordManager.isPasswordSet() ? 262 : 344;

        // Unlock button
        unlockButton = new JButton(passwordManager.isPasswordSet() ? "🔓  Unlock Vault" : "🔐  Create Vault") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color c1 = getModel().isRollover() ? new Color(95,78,200) : new Color(108,93,211);
                Color c2 = getModel().isRollover() ? new Color(118,88,220) : new Color(130,100,230);
                g2.setPaint(new GradientPaint(0,0,c1,getWidth(),0,c2));
                g2.fillRoundRect(0,0,getWidth(),getHeight(),12,12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        unlockButton.setFont(new Font("Segoe UI", Font.BOLD, 15));
        unlockButton.setForeground(Color.WHITE);
        unlockButton.setContentAreaFilled(false); unlockButton.setBorderPainted(false); unlockButton.setFocusPainted(false);
        unlockButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        unlockButton.setBounds(40, btnY, 330, 48);
        right.add(unlockButton);

        // Status label
        statusLabel = new JLabel("", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setForeground(new Color(239, 68, 68));
        statusLabel.setBounds(40, btnY + 54, 330, 20);
        right.add(statusLabel);

        // Footer
        JLabel footer = new JLabel("Your files are AES-256 encrypted & private", SwingConstants.CENTER);
        footer.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        footer.setForeground(new Color(180, 175, 210));
        footer.setBounds(20, 490, 370, 20);
        right.add(footer);

        passwordField.addActionListener(e -> unlockButton.doClick());
        if (!passwordManager.isPasswordSet()) confirmField.addActionListener(e -> unlockButton.doClick());

        unlockButton.addActionListener(e -> {
            String pass = new String(passwordField.getPassword());
            if (pass.isEmpty()) { shake(); statusLabel.setText("⚠ Password cannot be empty"); return; }
            try {
                if (!passwordManager.isPasswordSet()) {
                    String conf = new String(confirmField.getPassword());
                    if (!pass.equals(conf)) { shake(); statusLabel.setText("⚠ Passwords do not match!"); return; }
                    if (pass.length() < 4) { statusLabel.setText("⚠ Minimum 4 characters required"); return; }
                    passwordManager.setPassword(pass);
                    openDashboard(pass);
                } else {
                    if (passwordManager.verifyPassword(pass)) {
                        openDashboard(pass);
                    } else {
                        failedAttempts++;
                        shake(); passwordField.setText("");
                        if (failedAttempts >= 5) {
                            statusLabel.setText("🔒 Too many attempts. Closing...");
                            javax.swing.Timer t = new javax.swing.Timer(2000, ev -> System.exit(0));
                            t.setRepeats(false); t.start();
                        } else {
                            statusLabel.setText("⚠ Wrong password! Attempt " + failedAttempts + "/5");
                        }
                    }
                }
            } catch (Exception ex) { statusLabel.setText("Error: " + ex.getMessage()); }
        });

        root.add(left);
        root.add(right);
        setContentPane(root);
        setVisible(true);
        passwordField.requestFocusInWindow();
    }

    private void openDashboard(String pass) {
        dispose();
        SwingUtilities.invokeLater(() -> {
            try { new VaultDashboard(VAULT_FILE, pass); }
            catch (Exception e) { JOptionPane.showMessageDialog(null, "Error: " + e.getMessage()); }
        });
    }

    private void shake() {
        final int[] pos = {0}; int ox = getX();
        javax.swing.Timer t = new javax.swing.Timer(30, null);
        t.addActionListener(e -> {
            pos[0]++;
            setLocation(pos[0] % 2 == 0 ? ox + 10 : ox - 10, getY());
            if (pos[0] >= 8) { t.stop(); setLocation(ox, getY()); }
        });
        t.start();
    }
}