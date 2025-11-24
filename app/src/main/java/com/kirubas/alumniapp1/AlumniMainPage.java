package com.kirubas.alumniapp1;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class AlumniMainPage extends AppCompatActivity {

    private static final String BACKUP_DIR_PATH = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            + "/AlumniApp/DB_Backup";
    private static final String BACKUP_FILE_NAME = "AlumniDB.db";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alumni_main_page);

        Button btnOpenAdminUser = findViewById(R.id.btnOpenAdminUser);
        btnOpenAdminUser.setOnClickListener(v -> {
            Intent i = new Intent(this, AdminUserActivity.class);
            startActivity(i);
        });

        Button btnOpenInstitutions = findViewById(R.id.btnOpenInstitutions);
        btnOpenInstitutions.setOnClickListener(v -> {
            Intent i = new Intent(this, AddInstitutionActivity.class);
            startActivity(i);
        });

        Button btnControlPanelLogin = findViewById(R.id.btnControlPanelLogin);
        btnControlPanelLogin.setOnClickListener(v -> {
            Intent i = new Intent(this, LoginActivity.class);
            startActivity(i);
        });

        Button btnAlumniLogin = findViewById(R.id.btnAlumniLogin);
        btnAlumniLogin.setOnClickListener(v -> {
            Intent i = new Intent(this, AlumniLoginActivity.class);
            startActivity(i);
        });
        Button btnAlumniRegister = findViewById(R.id.btnAlumniRegister);
        btnAlumniRegister.setOnClickListener(v -> {
            Intent i = new Intent(this, AddAlumniActivity.class);
            startActivity(i);
        });
    }

    // Link this method to your backup button in XML using android:onClick="backupDatabase"
    public void backupDatabase(View view) {
        File backupDir = new File(BACKUP_DIR_PATH);
        if (!backupDir.exists()) {
            if (!backupDir.mkdirs()) {
                Toast.makeText(this, "Failed to create backup directory", Toast.LENGTH_LONG).show();
                return;
            }
        }

        File backupFile = new File(backupDir, BACKUP_FILE_NAME);
        String dbPath = getDatabasePath("AlumniDB.db").getAbsolutePath();

        try (FileInputStream fis = new FileInputStream(dbPath);
             OutputStream os = new FileOutputStream(backupFile)) {

            byte[] buffer = new byte[8192];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
            os.flush();

            Toast.makeText(this, "Database backed up to:\n" + backupFile.getAbsolutePath(), Toast.LENGTH_LONG).show();

            // Removed sharing intent — backup only

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Backup failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

}
