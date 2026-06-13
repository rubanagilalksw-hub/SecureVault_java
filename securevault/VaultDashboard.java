package securevault;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

public class VaultDashboard extends JFrame {

    private VaultManager vaultManager;
    private PasswordManager passwordManager;
    private String vaultFilePath;
    private String password;
    private JPanel filesPanel;
    private JLabel usedLabel, percentLabel;
    private StorageArc storageArc;
    private String currentFilter = "ALL";
    private static final long MAX_STORAGE = getTotalDriveSpace();

    private static long getTotalDriveSpace() {
        try {
            File root = new File(VaultManager.getVaultFilePath()).getParentFile();
            return root.getTotalSpace();
        } catch (Exception e) {
            return 10L * 1024 * 1024 * 1024;
        }
    }

    private static long getFreeDriveSpace() {
        try {
            File root = new File(VaultManager.getVaultFilePath()).getParentFile();
            return root.getFreeSpace();
        } catch (Exception e) {
            return 0;
        }
    }

    // ── color palette (light theme) ──
    static final Color BG        = new Color(248, 248, 255);
    static final Color SIDEBAR   = Color.WHITE;
    static final Color CARD_BG   = Color.WHITE;
    static final Color ACCENT    = new Color(108, 93, 211);
    static final Color ACCENT2   = new Color(130, 112, 240);
    static final Color TEXT_DARK = new Color(30,  30,  50);
    static final Color TEXT_MID  = new Color(100, 100, 130);
    static final Color TEXT_LITE = new Color(160, 160, 190);
    static final Color BORDER_C  = new Color(230, 228, 245);

    public VaultDashboard(String vaultFilePath, String password) throws Exception {
        this.vaultFilePath = vaultFilePath;
        this.password      = password;
        this.vaultManager  = new VaultManager(vaultFilePath, password);
        this.passwordManager = new PasswordManager(vaultFilePath);
        initUI();
    }

    // ════════════════════════════════════════════════════
    //  INIT
    // ════════════════════════════════════════════════════
    private void initUI() {
        setTitle("SecureVault");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1060, 700);
        setLocationRelativeTo(null);
        setUndecorated(true);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG);
        root.setBorder(new LineBorder(BORDER_C, 1));

        root.add(buildSidebar(), BorderLayout.WEST);
        root.add(buildContent(), BorderLayout.CENTER);

        setContentPane(root);
        setVisible(true);
        refreshGrid();
    }

    // ════════════════════════════════════════════════════
    //  SIDEBAR
    // ════════════════════════════════════════════════════
    private JPanel buildSidebar() {
        JPanel sb = new JPanel();
        sb.setBackground(SIDEBAR);
        sb.setPreferredSize(new Dimension(210, 0));
        sb.setLayout(new BoxLayout(sb, BoxLayout.Y_AXIS));
        sb.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 0, 0, 1, BORDER_C),
            BorderFactory.createEmptyBorder(24, 16, 24, 16)
        ));

        // Logo
        JPanel logoRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        logoRow.setOpaque(false);
        logoRow.setAlignmentX(LEFT_ALIGNMENT);
        JLabel logoIcon = new JLabel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ACCENT);
                g2.fillRoundRect(0, 0, 32, 32, 10, 10);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI Emoji", Font.BOLD, 16));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString("🔒", (32 - fm.stringWidth("🔒")) / 2, 22);
                g2.dispose();
            }
        };
        logoIcon.setPreferredSize(new Dimension(32, 32));
        JPanel logoText = new JPanel();
        logoText.setOpaque(false);
        logoText.setLayout(new BoxLayout(logoText, BoxLayout.Y_AXIS));
        JLabel logoName = new JLabel("SecureVault");
        logoName.setFont(new Font("Segoe UI", Font.BOLD, 15));
        logoName.setForeground(TEXT_DARK);
        JLabel logoSub = new JLabel("v1.0 — Encrypted");
        logoSub.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        logoSub.setForeground(ACCENT);
        logoText.add(logoName);
        logoText.add(logoSub);
        logoRow.add(logoIcon);
        logoRow.add(logoText);
        sb.add(logoRow);
        sb.add(Box.createVerticalStrut(28));

        // Nav items
        String[][] navItems = {
            {"ALL",  "🗂", "All Files"},
            {"IMG",  "🖼", "Images"},
            {"VID",  "▶",  "Videos"},
            {"AUD",  "♪",  "Audio"},
            {"DOC",  "📄", "Documents"},
        };
        for (String[] item : navItems) {
            JPanel btn = makeNavBtn(item[0], item[1], item[2]);
            sb.add(btn);
            sb.add(Box.createVerticalStrut(4));
        }

        sb.add(Box.createVerticalGlue());

        // simple file count label
        usedLabel = new JLabel("0 files");
        usedLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        usedLabel.setForeground(TEXT_MID);
        usedLabel.setAlignmentX(LEFT_ALIGNMENT);
        sb.add(usedLabel);
        sb.add(Box.createVerticalStrut(4));

        percentLabel = new JLabel("");
        percentLabel.setVisible(false);
        storageArc = new StorageArc();

        // Lock + Change Password
        JButton lockBtn = makeSolidBtn("🔒  Lock Vault", new Color(239, 68, 68), Color.WHITE);
        lockBtn.addActionListener(e -> { dispose(); SwingUtilities.invokeLater(LoginScreen::new); });
        lockBtn.setAlignmentX(LEFT_ALIGNMENT);
        sb.add(lockBtn);
        sb.add(Box.createVerticalStrut(8));

        JButton changePwBtn = makeOutlineBtn("🔑  Change Password", ACCENT);
        changePwBtn.addActionListener(e -> changePassword());
        changePwBtn.setAlignmentX(LEFT_ALIGNMENT);
        sb.add(changePwBtn);

        return sb;
    }

    // ════════════════════════════════════════════════════
    //  CONTENT
    // ════════════════════════════════════════════════════
    private JPanel buildContent() {
        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(BG);

        // Top bar
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(Color.WHITE);
        topBar.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0, 0, 1, 0, BORDER_C),
            BorderFactory.createEmptyBorder(16, 28, 16, 20)
        ));

        JLabel title = new JLabel("Your Vault");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(TEXT_DARK);
        topBar.add(title, BorderLayout.WEST);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnRow.setOpaque(false);

        JButton addFilesBtn = makeSolidBtn("＋  Add Files", ACCENT, Color.WHITE);
        addFilesBtn.addActionListener(e -> addFiles());
        btnRow.add(addFilesBtn);

        JButton addFolderBtn = makeOutlineBtn("📂  Add Folder", ACCENT);
        addFolderBtn.addActionListener(e -> addFolder());
        btnRow.add(addFolderBtn);

        // Window close
        JButton closeBtn = new JButton("✕") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isRollover()) {
                    g2.setColor(new Color(239, 68, 68));
                    g2.fillRoundRect(0,0,getWidth(),getHeight(),8,8);
                    g2.setColor(Color.WHITE);
                } else {
                    g2.setColor(new Color(240, 238, 252));
                    g2.fillRoundRect(0,0,getWidth(),getHeight(),8,8);
                    g2.setColor(TEXT_MID);
                }
                g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString("✕", (getWidth()-fm.stringWidth("✕"))/2, getHeight()/2+fm.getAscent()/2-1);
                g2.dispose();
            }
        };
        closeBtn.setPreferredSize(new Dimension(32, 32));
        closeBtn.setContentAreaFilled(false); closeBtn.setBorderPainted(false); closeBtn.setFocusPainted(false);
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.addActionListener(e -> System.exit(0));

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
                    g2.setColor(new Color(240, 238, 252));
                    g2.fillRoundRect(0,0,getWidth(),getHeight(),8,8);
                    g2.setColor(TEXT_MID);
                }
                g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString("─",(getWidth()-fm.stringWidth("─"))/2, getHeight()/2+fm.getAscent()/2-1);
                g2.dispose();
            }
        };
        minBtn.setPreferredSize(new Dimension(32, 32));
        minBtn.setContentAreaFilled(false); minBtn.setBorderPainted(false); minBtn.setFocusPainted(false);
        minBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        minBtn.addActionListener(e -> setState(Frame.ICONIFIED));
        btnRow.add(Box.createHorizontalStrut(8));
        btnRow.add(minBtn);
        btnRow.add(closeBtn);

        topBar.add(btnRow, BorderLayout.EAST);

        // Drag
        MouseAdapter drag = new MouseAdapter() {
            int sx, sy;
            public void mousePressed(MouseEvent e)  { sx = e.getX(); sy = e.getY(); }
            public void mouseDragged(MouseEvent e)  { setLocation(getX()+e.getX()-sx, getY()+e.getY()-sy); }
        };
        topBar.addMouseListener(drag);
        topBar.addMouseMotionListener(drag);

        content.add(topBar, BorderLayout.NORTH);

        // Files grid
        filesPanel = new JPanel();
        filesPanel.setBackground(BG);
        filesPanel.setLayout(new WrapLayout(FlowLayout.LEFT, 18, 18));

        JScrollPane sp = new JScrollPane(filesPanel);
        sp.setBackground(BG);
        sp.getViewport().setBackground(BG);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        sp.getVerticalScrollBar().setBackground(BG);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BG);
        wrapper.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        wrapper.add(sp, BorderLayout.CENTER);
        content.add(wrapper, BorderLayout.CENTER);

        return content;
    }

    // ════════════════════════════════════════════════════
    //  FILE CARD
    // ════════════════════════════════════════════════════
    private JPanel createFileCard(VaultManager.VaultFile vf) {
        // Card colours based on file type
        Color[] cols = cardColors(vf.getExtension());
        Color bgColor    = cols[0];
        Color iconColor  = cols[1];
        Color iconBg     = cols[2];

        JPanel card = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getClientProperty("hov") != null ? new Color(240,238,255) : CARD_BG);
                g2.fillRoundRect(0,0,getWidth(),getHeight(),18,18);
                g2.setColor(bgColor);
                g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,18,18);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(192, 230));

        // ── TOP: 3-dots ──
        JPanel topRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        topRow.setOpaque(false);
        JButton dots = new JButton("•••");
        dots.setFont(new Font("Segoe UI", Font.BOLD, 12));
        dots.setForeground(TEXT_MID);
        dots.setContentAreaFilled(false); dots.setBorderPainted(false); dots.setFocusPainted(false);
        dots.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        dots.setPreferredSize(new Dimension(32, 22));
        dots.addActionListener(e -> showDotMenu(dots, vf));
        topRow.add(dots);
        card.add(topRow, BorderLayout.NORTH);

        // ── CENTER: colored icon ──
        JPanel iconPanel = new JPanel(new GridBagLayout());
        iconPanel.setOpaque(false);
        JLabel iconBox = new JLabel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(iconBg);
                g2.fillRoundRect(0,0,getWidth(),getHeight(),16,16);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        iconBox.setPreferredSize(new Dimension(80, 80));
        iconBox.setHorizontalAlignment(SwingConstants.CENTER);
        iconBox.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 36));
        iconBox.setText(vf.getTypeIcon());
        iconBox.setForeground(iconColor);
        iconBox.setOpaque(false);
        iconPanel.add(iconBox);
        card.add(iconPanel, BorderLayout.CENTER);

        // ── BOTTOM: name + meta ──
        JPanel bottomPanel = new JPanel();
        bottomPanel.setOpaque(false);
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(8, 12, 14, 12));

        String dispName = vf.originalName.length() > 22 ? vf.originalName.substring(0,20)+"..." : vf.originalName;
        JLabel nameLabel = new JLabel(dispName);
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        nameLabel.setForeground(TEXT_DARK);
        nameLabel.setAlignmentX(LEFT_ALIGNMENT);

        JLabel sizeLabel = new JLabel(vf.getFormattedSize());
        sizeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        sizeLabel.setForeground(TEXT_MID);
        sizeLabel.setAlignmentX(LEFT_ALIGNMENT);

        // type badge + date row
        JPanel metaRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        metaRow.setOpaque(false);
        JLabel typeBadge = new JLabel(getTypeName(vf.getExtension()));
        typeBadge.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        typeBadge.setForeground(iconColor);
        typeBadge.setBackground(iconBg);
        typeBadge.setOpaque(true);
        typeBadge.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        JLabel dateLabel = new JLabel("  " + new SimpleDateFormat("MMM d, h:mm a").format(new Date()));
        dateLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        dateLabel.setForeground(TEXT_LITE);
        metaRow.add(typeBadge);
        metaRow.add(dateLabel);

        bottomPanel.add(nameLabel);
        bottomPanel.add(Box.createVerticalStrut(3));
        bottomPanel.add(sizeLabel);
        bottomPanel.add(Box.createVerticalStrut(6));
        bottomPanel.add(metaRow);
        card.add(bottomPanel, BorderLayout.SOUTH);

        // hover + click
        MouseAdapter ma = new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { card.putClientProperty("hov",1); card.repaint(); card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); }
            public void mouseExited(MouseEvent e)  { card.putClientProperty("hov",null); card.repaint(); }
            public void mouseClicked(MouseEvent e) {
                if (e.getSource()==dots) return;
                new FileViewer(vaultManager, VaultDashboard.this).open(vf);
            }
        };
        card.addMouseListener(ma);
        iconPanel.addMouseListener(ma);

        return card;
    }

    private void showDotMenu(JButton dots, VaultManager.VaultFile vf) {
        JPopupMenu menu = new JPopupMenu();
        menu.setBackground(Color.WHITE);
        menu.setBorder(new LineBorder(BORDER_C, 1));

        JMenuItem open = new JMenuItem("  Open");
        styleMenuItem(open, TEXT_DARK);
        open.addActionListener(e -> new FileViewer(vaultManager, this).open(vf));
        menu.add(open);
        menu.addSeparator();

        JMenuItem export = new JMenuItem("  Export to PC");
        styleMenuItem(export, TEXT_DARK);
        export.addActionListener(e -> exportFile(vf));
        menu.add(export);

        JMenuItem delete = new JMenuItem("  Delete from Vault");
        styleMenuItem(delete, new Color(239, 68, 68));
        delete.addActionListener(e -> deleteFile(vf));
        menu.add(delete);

        menu.show(dots, 0, dots.getHeight());
    }

    // ════════════════════════════════════════════════════
    //  REFRESH GRID
    // ════════════════════════════════════════════════════
    private void refreshGrid() {
        filesPanel.removeAll();
        List<VaultManager.VaultFile> files = vaultManager.listFiles();
        long totalBytes = 0;
        int count = 0;

        for (VaultManager.VaultFile vf : files) {
            if (!matchFilter(vf)) continue;
            count++;
            totalBytes += vf.size;
            filesPanel.add(createFileCard(vf));
        }

        if (count == 0) {
            JPanel empty = new JPanel(new GridBagLayout());
            empty.setBackground(BG);
            empty.setPreferredSize(new Dimension(700, 380));
            JLabel el = new JLabel("<html><center><span style='font-size:40px'>🔒</span><br><br><b style='font-size:17px;color:#1e1e32'>Vault is Empty</b><br><br><span style='color:#a0a0be;font-size:13px'>Click Add Files to secure your files</span></center></html>");
            empty.add(el);
            filesPanel.add(empty);
        }

        // update file count
        usedLabel.setText(count + " file" + (count != 1 ? "s" : ""));
        storageArc.setPercent(0);

        filesPanel.revalidate();
        filesPanel.repaint();
    }

    private boolean matchFilter(VaultManager.VaultFile vf) {
        String e = vf.getExtension();
        switch (currentFilter) {
            case "IMG": return "jpg,jpeg,png,gif,bmp,webp".contains(e) && !e.isEmpty();
            case "VID": return "mp4,avi,mkv,mov,wmv,flv".contains(e) && !e.isEmpty();
            case "AUD": return "mp3,wav,aac,flac,ogg".contains(e) && !e.isEmpty();
            case "DOC": return "pdf,doc,docx,txt,xls,xlsx,ppt,pptx".contains(e) && !e.isEmpty();
            default: return true;
        }
    }

    // ════════════════════════════════════════════════════
    //  ACTIONS
    // ════════════════════════════════════════════════════
    private void addFiles() {
        JFileChooser fc = new JFileChooser();
        fc.setMultiSelectionEnabled(true);
        fc.setDialogTitle("Select files to add to Vault");
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            int ok = 0;
            for (File f : fc.getSelectedFiles()) {
                try { vaultManager.addFile(f); ok++; }
                catch (Exception ex) { JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage()); }
            }
            toast(ok + " file(s) added & secured ✓");
            refreshGrid();
        }
    }

    private void addFolder() {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setDialogTitle("Select folder");
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File[] files = fc.getSelectedFile().listFiles();
            int ok = 0;
            if (files != null) for (File f : files) if (f.isFile()) {
                try { vaultManager.addFile(f); ok++; } catch (Exception ignored) {}
            }
            toast(ok + " file(s) added ✓");
            refreshGrid();
        }
    }

    private void exportFile(VaultManager.VaultFile vf) {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setDialogTitle("Choose folder to export: " + vf.originalName);
        File desktop = new File(System.getProperty("user.home"), "Desktop");
        if (desktop.exists()) fc.setCurrentDirectory(desktop);

        int result = fc.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File destFolder = fc.getSelectedFile();
            try {
                destFolder.mkdirs();
                File outFile = new File(destFolder, vf.originalName);
                vaultManager.exportFile(vf.storedName, destFolder);

                if (outFile.exists()) {
                    // Auto-delete from vault after export
                    vaultManager.deleteFile(vf.storedName);
                    refreshGrid();
                    toast("✓ Exported & removed from vault");
                    if (Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().open(destFolder);
                    }
                } else {
                    JOptionPane.showMessageDialog(this,
                        "Export failed — file was not created.",
                        "Export Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                    "Export error: " + ex.getMessage(),
                    "Export Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void deleteFile(VaultManager.VaultFile vf) {
        int c = JOptionPane.showConfirmDialog(this,
            "Permanently delete '" + vf.originalName + "'?\nThis cannot be undone.",
            "Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (c == JOptionPane.YES_OPTION) {
            try { vaultManager.deleteFile(vf.storedName); toast("Deleted ✓"); refreshGrid(); }
            catch (Exception ex) { JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage()); }
        }
    }

    private void changePassword() {
        JPasswordField op = new JPasswordField(), np = new JPasswordField(), cp = new JPasswordField();
        JPanel p = new JPanel(new GridLayout(3,2,8,8));
        p.add(new JLabel("Old Password:")); p.add(op);
        p.add(new JLabel("New Password:")); p.add(np);
        p.add(new JLabel("Confirm New:"));  p.add(cp);
        if (JOptionPane.showConfirmDialog(this, p, "Change Password", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            try {
                String o = new String(op.getPassword()), n = new String(np.getPassword()), co = new String(cp.getPassword());
                if (!n.equals(co)) { JOptionPane.showMessageDialog(this, "Passwords don't match!"); return; }
                passwordManager.changePassword(o, n);
                JOptionPane.showMessageDialog(this, "Password changed! Please re-login.");
                dispose(); new LoginScreen();
            } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage()); }
        }
    }

    private void toast(String msg) {
        JWindow w = new JWindow(this);
        JLabel l = new JLabel("  " + msg + "  ", SwingConstants.CENTER);
        l.setFont(new Font("Segoe UI", Font.BOLD, 13));
        l.setForeground(Color.WHITE);
        l.setBackground(new Color(34,197,94));
        l.setOpaque(true);
        l.setBorder(BorderFactory.createEmptyBorder(10,20,10,20));
        w.getContentPane().add(l);
        w.pack();
        w.setLocation(getX()+getWidth()/2-w.getWidth()/2, getY()+getHeight()-70);
        w.setVisible(true);
        javax.swing.Timer t2 = new javax.swing.Timer(2500, e -> w.dispose());
        t2.setRepeats(false); t2.start();
    }

    // ════════════════════════════════════════════════════
    //  NAV BUTTON
    // ════════════════════════════════════════════════════
    private JPanel makeNavBtn(String key, String icon, String label) {
        JPanel btn = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (currentFilter.equals(key)) {
                    g2.setColor(new Color(238, 236, 255));
                    g2.fillRoundRect(0,0,getWidth(),getHeight(),10,10);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setOpaque(false);
        btn.setMaximumSize(new Dimension(178, 38));
        btn.setAlignmentX(LEFT_ALIGNMENT);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel ic = new JLabel(icon);
        ic.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 15));
        ic.setForeground(currentFilter.equals(key) ? ACCENT : TEXT_MID);

        JLabel lb = new JLabel(label);
        lb.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lb.setForeground(currentFilter.equals(key) ? ACCENT : TEXT_DARK);

        btn.add(ic); btn.add(lb);

        btn.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                currentFilter = key;
                // rebuild sidebar to refresh active state
                JPanel sidebar = (JPanel) getContentPane().getComponent(0);
                sidebar.repaint();
                refreshGrid();
            }
            public void mouseEntered(MouseEvent e) { if (!currentFilter.equals(key)) btn.setBackground(new Color(248,246,255)); btn.setOpaque(true); btn.repaint(); }
            public void mouseExited(MouseEvent e)  { btn.setOpaque(false); btn.repaint(); }
        });

        return btn;
    }

    // ════════════════════════════════════════════════════
    //  HELPERS
    // ════════════════════════════════════════════════════
    private Color[] cardColors(String ext) {
        if ("jpg,jpeg,png,gif,bmp,webp".contains(ext) && !ext.isEmpty())
            return new Color[]{new Color(255,237,213), new Color(234,88,12), new Color(255,247,237)};
        if ("mp4,avi,mkv,mov,wmv,flv".contains(ext) && !ext.isEmpty())
            return new Color[]{new Color(219,234,254), new Color(37,99,235), new Color(239,246,255)};
        if ("mp3,wav,aac,flac,ogg".contains(ext) && !ext.isEmpty())
            return new Color[]{new Color(252,231,243), new Color(219,39,119), new Color(253,242,248)};
        if ("pdf,doc,docx,txt,rtf".contains(ext) && !ext.isEmpty())
            return new Color[]{new Color(220,214,254), ACCENT, new Color(237,233,254)};
        if ("xls,xlsx,csv".contains(ext) && !ext.isEmpty())
            return new Color[]{new Color(209,250,229), new Color(22,163,74), new Color(240,253,244)};
        if ("zip,rar,7z,tar".contains(ext) && !ext.isEmpty())
            return new Color[]{new Color(254,249,195), new Color(202,138,4), new Color(254,252,232)};
        return new Color[]{BORDER_C, TEXT_MID, new Color(248,248,255)};
    }

    private String getTypeName(String ext) {
        if ("jpg,jpeg,png,gif,bmp,webp".contains(ext) && !ext.isEmpty()) return "Image";
        if ("mp4,avi,mkv,mov,wmv,flv".contains(ext) && !ext.isEmpty())  return "Video";
        if ("mp3,wav,aac,flac,ogg".contains(ext) && !ext.isEmpty())     return "Audio";
        if ("pdf,doc,docx,txt,rtf".contains(ext) && !ext.isEmpty())     return "Document";
        if ("xls,xlsx,csv".contains(ext) && !ext.isEmpty())             return "Spreadsheet";
        if ("zip,rar,7z,tar".contains(ext) && !ext.isEmpty())           return "Archive";
        return "File";
    }

    private String formatSize(long b) {
        if (b < 1024) return b + " B";
        if (b < 1024*1024) return String.format("%.1f KB", b/1024.0);
        if (b < 1024L*1024*1024) return String.format("%.1f MB", b/(1024.0*1024));
        return String.format("%.2f GB", b/(1024.0*1024*1024));
    }

    private JButton makeSolidBtn(String text, Color bg, Color fg) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? bg.darker() : bg);
                g2.fillRoundRect(0,0,getWidth(),getHeight(),10,10);
                g2.dispose(); super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setForeground(fg);
        btn.setContentAreaFilled(false); btn.setBorderPainted(false); btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(148, 38));
        btn.setMaximumSize(new Dimension(178, 38));
        return btn;
    }

    private JButton makeOutlineBtn(String text, Color col) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? new Color(238,236,255) : Color.WHITE);
                g2.fillRoundRect(0,0,getWidth(),getHeight(),10,10);
                g2.setColor(col);
                g2.drawRoundRect(0,0,getWidth()-1,getHeight()-1,10,10);
                g2.dispose(); super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setForeground(col);
        btn.setContentAreaFilled(false); btn.setBorderPainted(false); btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(148, 38));
        btn.setMaximumSize(new Dimension(178, 38));
        return btn;
    }

    private void styleMenuItem(JMenuItem item, Color fg) {
        item.setBackground(Color.WHITE);
        item.setForeground(fg);
        item.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        item.setBorder(BorderFactory.createEmptyBorder(8,16,8,16));
        item.setOpaque(true);
    }

    // ════════════════════════════════════════════════════
    //  STORAGE ARC WIDGET
    // ════════════════════════════════════════════════════
    static class StorageArc extends JComponent {
        private double percent = 0;
        private JProgressBar progBar;
        private JLabel pctLabel;

        void setPercent(double p) {
            this.percent = p;
            if (progBar != null) progBar.setValue((int) Math.min(p, 100));
            if (pctLabel != null) {
                if (p < 0.01 && p > 0) pctLabel.setText("< 0.01%");
                else pctLabel.setText(String.format("%.2f%%", p));
            }
            repaint();
        }
        void setProgressBar(JProgressBar b) { this.progBar = b; }
        void setPercentLabel(JLabel l)      { this.pctLabel = l; }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int s = Math.min(getWidth(), getHeight()) - 4;
            int x = (getWidth()-s)/2, y = (getHeight()-s)/2;
            // background ring
            g2.setStroke(new BasicStroke(6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(220, 218, 240));
            g2.drawArc(x, y, s, s, 0, 360);
            // filled arc — minimum 8 degrees so it's always visible
            if (percent > 0) {
                g2.setColor(ACCENT);
                int angle = Math.max(8, (int)(360 * Math.min(percent, 100) / 100.0));
                g2.drawArc(x, y, s, s, 90, -angle);
            }
            g2.dispose();
        }
    }
}