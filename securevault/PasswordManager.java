package securevault;

import java.io.*;
import java.nio.file.*;

public class PasswordManager {
    private final File passFile;

    public PasswordManager(String vaultFilePath) { this.passFile = new File(vaultFilePath + ".key"); }

    public boolean isPasswordSet() { return passFile.exists(); }

    public void setPassword(String password) throws Exception {
        Files.write(passFile.toPath(), CryptoUtil.hashPassword(password).getBytes("UTF-8"));
    }

    public boolean verifyPassword(String password) throws Exception {
        if (!isPasswordSet()) return false;
        return new String(Files.readAllBytes(passFile.toPath()), "UTF-8").trim().equals(CryptoUtil.hashPassword(password));
    }

    public void changePassword(String oldPassword, String newPassword) throws Exception {
        if (!verifyPassword(oldPassword)) throw new Exception("Old password is incorrect!");
        setPassword(newPassword);
    }
}
