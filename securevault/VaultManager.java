package securevault;

import javax.crypto.SecretKey;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class VaultManager {

    private final File vaultFile;
    private final SecretKey secretKey;
    private List<VaultEntry> entries;

    private static class VaultEntry {
        String id;
        String originalName;
        long originalSize;
        byte[] encryptedData;

        VaultEntry(String id, String originalName, long originalSize, byte[] encryptedData) {
            this.id = id;
            this.originalName = originalName;
            this.originalSize = originalSize;
            this.encryptedData = encryptedData;
        }
    }

    public VaultManager(String vaultFilePath, String password) throws Exception {
        this.vaultFile = new File(vaultFilePath);
        this.secretKey = CryptoUtil.generateKeyFromPassword(password);
        this.entries = new ArrayList<>();
        if (vaultFile.exists()) loadVault();
    }

    public static String getVaultFilePath() {
        try {
            File jarLocation = new File(VaultManager.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            File dir = jarLocation.isDirectory() ? jarLocation : jarLocation.getParentFile();
            return dir.getAbsolutePath() + File.separator + "data.svault";
        } catch (Exception e) {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                String appData = System.getenv("APPDATA");
                if (appData != null) {
                    File dir = new File(appData + File.separator + ".svhidden");
                    dir.mkdirs();
                    try { Runtime.getRuntime().exec("attrib +H +S \"" + dir.getAbsolutePath() + "\"").waitFor(); } catch (Exception ignored) {}
                    return dir.getAbsolutePath() + File.separator + "data.svault";
                }
            }
            return System.getProperty("user.home") + File.separator + "data.svault";
        }
    }

    public void addFile(File sourceFile) throws Exception {
        byte[] rawData = Files.readAllBytes(sourceFile.toPath());
        byte[] encrypted = CryptoUtil.encrypt(rawData, secretKey);
        entries.add(new VaultEntry(UUID.randomUUID().toString(), sourceFile.getName(), sourceFile.length(), encrypted));
        saveVault();
        sourceFile.delete();
    }

    public void exportFile(String id, File destFolder) throws Exception {
        VaultEntry entry = findById(id);
        if (entry == null) throw new Exception("File not found");
        byte[] decrypted = CryptoUtil.decrypt(entry.encryptedData, secretKey);
        Files.write(new File(destFolder, entry.originalName).toPath(), decrypted);
    }

    public void deleteFile(String id) throws Exception {
        entries.removeIf(e -> e.id.equals(id));
        saveVault();
    }

    public List<VaultFile> listFiles() {
        List<VaultFile> result = new ArrayList<>();
        for (VaultEntry e : entries) result.add(new VaultFile(e.id, e.originalName, e.originalSize));
        return result;
    }

    private void saveVault() throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        dos.writeInt(entries.size());
        for (VaultEntry e : entries) {
            writeStr(dos, e.originalName);
            writeStr(dos, e.id);
            dos.writeLong(e.originalSize);
            dos.writeInt(e.encryptedData.length);
            dos.write(e.encryptedData);
        }
        dos.flush();
        Files.write(vaultFile.toPath(), CryptoUtil.encrypt(bos.toByteArray(), secretKey));
    }

    private void loadVault() throws Exception {
        byte[] plain;
        try { plain = CryptoUtil.decrypt(Files.readAllBytes(vaultFile.toPath()), secretKey); }
        catch (Exception e) { throw new Exception("Wrong password or corrupted vault!"); }
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(plain));
        int count = dis.readInt();
        entries = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String name = readStr(dis), id = readStr(dis);
            long size = dis.readLong();
            byte[] data = new byte[dis.readInt()];
            dis.readFully(data);
            entries.add(new VaultEntry(id, name, size, data));
        }
    }

    private void writeStr(DataOutputStream dos, String s) throws IOException { byte[] b = s.getBytes("UTF-8"); dos.writeInt(b.length); dos.write(b); }
    private String readStr(DataInputStream dis) throws IOException { byte[] b = new byte[dis.readInt()]; dis.readFully(b); return new String(b, "UTF-8"); }
    private VaultEntry findById(String id) { for (VaultEntry e : entries) if (e.id.equals(id)) return e; return null; }

    public static class VaultFile {
        public final String storedName, originalName;
        public final long size;

        public VaultFile(String storedName, String originalName, long size) {
            this.storedName = storedName; this.originalName = originalName; this.size = size;
        }

        public String getExtension() { int d = originalName.lastIndexOf('.'); return d >= 0 ? originalName.substring(d+1).toLowerCase() : ""; }

        public String getFormattedSize() {
            if (size < 1024) return size + " B";
            if (size < 1024*1024) return String.format("%.1f KB", size/1024.0);
            if (size < 1024L*1024*1024) return String.format("%.1f MB", size/(1024.0*1024));
            return String.format("%.2f GB", size/(1024.0*1024*1024));
        }

        public String getTypeIcon() {
            switch (getExtension()) {
                case "jpg": case "jpeg": case "png": case "gif": case "bmp": case "webp": return "🖼";
                case "mp4": case "avi": case "mkv": case "mov": case "wmv": case "flv":  return "🎬";
                case "mp3": case "wav": case "aac": case "flac": case "ogg":             return "🎵";
                case "pdf": return "📄";
                case "doc": case "docx": case "txt": case "rtf": return "📝";
                case "xls": case "xlsx": case "csv": return "📊";
                case "zip": case "rar": case "7z": case "tar": return "📦";
                default: return "📁";
            }
        }
    }
}
