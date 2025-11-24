package com.kirubas.alumniapp1;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.*;

public class DatabaseRestoreActivity extends AppCompatActivity {

    File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
    File backupDir = new File(downloadsDir, "AlumniApp/DB_Backup");
    String backupPath = backupDir.getAbsolutePath();

    private static final String BACKUP_DB_PATH = "/storage/emulated/0/Download/AlumniApp/DB_Backup/AlumniDB.db";
    private static final String APP_DB_NAME = "AlumniDB.db";

        @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        checkAndOfferRestore();
    }

    private void checkAndOfferRestore() {
        File backupDbFile = new File(BACKUP_DB_PATH);
        if (backupDbFile.exists()) {
            new AlertDialog.Builder(this)
                    .setTitle("Database Backup Found")
                    .setMessage("A backup database was found. Do you want to restore from backup?")
                    .setPositiveButton("Restore", (dialog, which) -> {
                        boolean success = restoreDatabase(backupDbFile);
                        if (success) {
                            Toast.makeText(this, "Database restored successfully.", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this, "Failed to restore database.", Toast.LENGTH_LONG).show();
                        }
                        finish(); // Close after operation
                    })
                    .setNegativeButton("Skip", (dialog, which) -> {
                        Toast.makeText(this, "Proceeding without restoring backup.", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .setCancelable(false)
                    .show();
        } else {
            // No backup found, proceed normally
            Toast.makeText(this, "No backup database found. Continuing fresh setup.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private boolean restoreDatabase(File backupFile) {
        try {
            // Path of the app's main database
            File appDbFile = getApplicationContext().getDatabasePath(APP_DB_NAME);

            if (!appDbFile.getParentFile().exists()) {
                appDbFile.getParentFile().mkdirs();
            }

            try (InputStream in = new FileInputStream(backupFile);
                 OutputStream out = new FileOutputStream(appDbFile)) {

                byte[] buffer = new byte[1024];
                int length;
                while ((length = in.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                }
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
