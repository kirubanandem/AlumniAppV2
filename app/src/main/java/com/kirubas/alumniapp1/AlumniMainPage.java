package com.kirubas.alumniapp1;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.documentfile.provider.DocumentFile;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;

public class AlumniMainPage extends AppCompatActivity {
    private static final int REQUEST_CODE_PICK_DIR = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alumni_main_page);

        Button btnAddAlumni = findViewById(R.id.btnAddAlumni);
        btnAddAlumni.setOnClickListener(v -> {
            Intent i = new Intent(this, AddAlumniActivity.class);
            startActivity(i);
        });

        Button btnViewAlumni = findViewById(R.id.btnViewAlumni);
        btnViewAlumni.setOnClickListener(v -> {
            Intent i = new Intent(this, ViewAlumniActivity.class);
            startActivity(i);
        });
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
        // No need to bind btnBackupDB or btnShareDB manually if using android:onClick!
    }

    // Called from XML button with android:onClick
    public void requestUserToPickFolder(View view) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        startActivityForResult(intent, REQUEST_CODE_PICK_DIR);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_PICK_DIR && resultCode == RESULT_OK && data != null) {
            Uri treeUri = data.getData();
            final int takeFlags = data.getFlags()
                    & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            getContentResolver().takePersistableUriPermission(treeUri, takeFlags);
            copyDatabaseToFolder(treeUri, "AlumniDB.db");
        }
    }

    void copyDatabaseToFolder(Uri folderUri, String fileName) {
        try {
            String dbPath = getDatabasePath("AlumniDB.db").getAbsolutePath();
            try (FileInputStream fis = new FileInputStream(dbPath)) {
                DocumentFile docFile = DocumentFile.fromTreeUri(this, folderUri);
                DocumentFile destFile = docFile.createFile("application/x-sqlite3", fileName);
                Uri destUri = destFile.getUri();

                try (OutputStream out = getContentResolver().openOutputStream(destUri)) {
                    byte[] buffer = new byte[8192];
                    int length;
                    while ((length = fis.read(buffer)) > 0) {
                        out.write(buffer, 0, length);
                    }
                    out.flush();
                    Toast.makeText(this, "Database copied successfully.", Toast.LENGTH_LONG).show();

                    // Now, share the copied file using destUri (not the internal DB file!)
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("application/x-sqlite3");
                    shareIntent.putExtra(Intent.EXTRA_STREAM, destUri);
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(shareIntent, "Share Alumni DB via..."));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to copy/share database: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
