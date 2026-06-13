package securevault;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.*;
import java.nio.file.*;

public class FileViewer {
    private final VaultManager vaultManager;
    private final JFrame parent;

    public FileViewer(VaultManager vm, JFrame parent) { this.vaultManager=vm; this.parent=parent; }

    public void open(VaultManager.VaultFile vf) {
        String ext = vf.getExtension();
        if ("jpg,jpeg,png,gif,bmp,webp".contains(ext) && !ext.isEmpty()) { openImageViewer(vf); return; }
        if ("txt,log,csv,xml,json,html,htm,java,py,js,css,md".contains(ext) && !ext.isEmpty()) { openTextViewer(vf); return; }
        openWithSystemApp(vf);
    }

    private void openImageViewer(VaultManager.VaultFile vf) {
        JDialog dialog = new JDialog(parent, vf.originalName, true);
        dialog.setSize(900, 650);
        dialog.setLocationRelativeTo(parent);
        dialog.setUndecorated(true);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(new Color(248,248,255));
        root.setBorder(new LineBorder(new Color(230,228,245),1));

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(Color.WHITE);
        topBar.setBorder(BorderFactory.createCompoundBorder(
            new MatteBorder(0,0,1,0,new Color(230,228,245)),
            BorderFactory.createEmptyBorder(10,16,10,16)));
        JLabel title = new JLabel("🖼  "+vf.originalName);
        title.setFont(new Font("Segoe UI",Font.BOLD,14));
        title.setForeground(new Color(30,30,50));
        topBar.add(title,BorderLayout.WEST);
        JButton closeBtn = new JButton("✕  Close");
        closeBtn.setFont(new Font("Segoe UI",Font.BOLD,12));
        closeBtn.setForeground(new Color(239,68,68));
        closeBtn.setContentAreaFilled(false); closeBtn.setBorderPainted(false); closeBtn.setFocusPainted(false);
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.addActionListener(e->dialog.dispose());
        JPanel right=new JPanel(new FlowLayout(FlowLayout.RIGHT,0,0));
        right.setOpaque(false); right.add(closeBtn);
        topBar.add(right,BorderLayout.EAST);
        root.add(topBar,BorderLayout.NORTH);

        JLabel loading = new JLabel("Loading...",SwingConstants.CENTER);
        loading.setForeground(new Color(148,163,184));
        loading.setFont(new Font("Segoe UI",Font.PLAIN,14));
        root.add(loading,BorderLayout.CENTER);

        dialog.setContentPane(root);
        dialog.setVisible(false);

        SwingWorker<ImageIcon,Void> worker = new SwingWorker<ImageIcon,Void>() {
            @Override protected ImageIcon doInBackground() throws Exception {
                byte[] data = decryptToBytes(vf);
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(data));
                if (img==null) throw new Exception("Cannot read image");
                int maxW=860,maxH=560;
                double scale=Math.min((double)maxW/img.getWidth(),(double)maxH/img.getHeight());
                if (scale<1.0) {
                    Image sc=img.getScaledInstance((int)(img.getWidth()*scale),(int)(img.getHeight()*scale),Image.SCALE_SMOOTH);
                    return new ImageIcon(sc);
                }
                return new ImageIcon(img);
            }
            @Override protected void done() {
                try {
                    ImageIcon icon=get();
                    JLabel imgLabel=new JLabel(icon,SwingConstants.CENTER);
                    imgLabel.setBackground(new Color(248,248,255)); imgLabel.setOpaque(true);
                    root.remove(loading); root.add(imgLabel,BorderLayout.CENTER);
                    root.revalidate(); root.repaint();
                } catch(Exception ex){ loading.setText("❌ Cannot display: "+ex.getMessage()); }
            }
        };
        worker.execute();
        dialog.setVisible(true);
    }

    private void openTextViewer(VaultManager.VaultFile vf) {
        JDialog dialog = new JDialog(parent,vf.originalName,true);
        dialog.setSize(820,600); dialog.setLocationRelativeTo(parent); dialog.setUndecorated(true);
        JPanel root=new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);
        root.setBorder(new LineBorder(new Color(230,228,245),1));
        JPanel topBar=new JPanel(new BorderLayout());
        topBar.setBackground(Color.WHITE);
        topBar.setBorder(BorderFactory.createCompoundBorder(new MatteBorder(0,0,1,0,new Color(230,228,245)),BorderFactory.createEmptyBorder(10,16,10,16)));
        JLabel title=new JLabel("📝  "+vf.originalName);
        title.setFont(new Font("Segoe UI",Font.BOLD,14)); title.setForeground(new Color(30,30,50));
        topBar.add(title,BorderLayout.WEST);
        JButton cb=new JButton("✕  Close");
        cb.setFont(new Font("Segoe UI",Font.BOLD,12)); cb.setForeground(new Color(239,68,68));
        cb.setContentAreaFilled(false); cb.setBorderPainted(false); cb.setFocusPainted(false);
        cb.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); cb.addActionListener(e->dialog.dispose());
        JPanel r=new JPanel(new FlowLayout(FlowLayout.RIGHT,0,0)); r.setOpaque(false); r.add(cb);
        topBar.add(r,BorderLayout.EAST); root.add(topBar,BorderLayout.NORTH);
        JTextArea area=new JTextArea();
        area.setBackground(new Color(250,250,255)); area.setForeground(new Color(30,30,50));
        area.setFont(new Font("Consolas",Font.PLAIN,13)); area.setCaretColor(new Color(108,93,211));
        area.setBorder(BorderFactory.createEmptyBorder(16,16,16,16)); area.setEditable(false);
        JScrollPane sp=new JScrollPane(area); sp.setBorder(BorderFactory.createEmptyBorder());
        root.add(sp,BorderLayout.CENTER);
        try { byte[] data=decryptToBytes(vf); area.setText(new String(data,"UTF-8")); area.setCaretPosition(0); }
        catch(Exception ex){area.setText("Error: "+ex.getMessage());}
        dialog.setContentPane(root); dialog.setVisible(true);
    }

    private void openWithSystemApp(VaultManager.VaultFile vf) {
        JDialog loading=new JDialog(parent,"Opening...",false);
        loading.setSize(320,100); loading.setLocationRelativeTo(parent); loading.setUndecorated(true);
        JPanel lp=new JPanel(new GridBagLayout());
        lp.setBackground(Color.WHITE); lp.setBorder(new LineBorder(new Color(230,228,245),1));
        JLabel ll=new JLabel("⏳  Opening "+vf.originalName+"...");
        ll.setFont(new Font("Segoe UI",Font.PLAIN,13)); ll.setForeground(new Color(108,93,211));
        lp.add(ll); loading.setContentPane(lp); loading.setVisible(true);

        SwingWorker<Void,Void> worker=new SwingWorker<Void,Void>(){
            @Override protected Void doInBackground() throws Exception {
                File tempDir=new File(System.getProperty("java.io.tmpdir"),"svault_tmp");
                tempDir.mkdirs();
                vaultManager.exportFile(vf.storedName,tempDir);
                File tempFile=new File(tempDir,vf.originalName);
                tempFile.deleteOnExit();
                if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(tempFile);
                else throw new Exception("Desktop not supported");
                return null;
            }
            @Override protected void done() {
                loading.dispose();
                try { get(); }
                catch(Exception ex){ JOptionPane.showMessageDialog(parent,"Cannot open:\n"+ex.getCause().getMessage(),"Error",JOptionPane.ERROR_MESSAGE); }
            }
        };
        worker.execute();
    }

    private byte[] decryptToBytes(VaultManager.VaultFile vf) throws Exception {
        File tempDir=new File(System.getProperty("java.io.tmpdir"),"svault_view");
        tempDir.mkdirs();
        vaultManager.exportFile(vf.storedName,tempDir);
        File exported=new File(tempDir,vf.originalName);
        byte[] data=Files.readAllBytes(exported.toPath());
        exported.delete();
        return data;
    }
}
