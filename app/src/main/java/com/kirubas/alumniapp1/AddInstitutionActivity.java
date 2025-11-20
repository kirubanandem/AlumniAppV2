package com.kirubas.alumniapp1;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class AddInstitutionActivity extends AppCompatActivity {

    EditText etName, etAddress, etAuthToken;
    ImageView ivLogo;
    Button btnSave, btnSelectLogo;
    ListView lvInstitutions;
    byte[] logoBytes = null;
    int editingInstitutionId = -1;

    ArrayList<String> instNames = new ArrayList<>();
    ArrayList<Integer> instIds = new ArrayList<>();

    private ActivityResultLauncher<Intent> getImageLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_institution);

        etName = findViewById(R.id.etInstitutionName);
        etAddress = findViewById(R.id.etInstitutionAddress);
        etAuthToken = findViewById(R.id.etInstitutionToken);
        ivLogo = findViewById(R.id.ivInstitutionLogo);
        btnSave = findViewById(R.id.btnSaveInstitution);
        btnSelectLogo = findViewById(R.id.btnSelectLogo);
        lvInstitutions = findViewById(R.id.lvInstitutions);

        getImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                            ivLogo.setImageBitmap(bitmap);
                            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                            logoBytes = outputStream.toByteArray();
                        } catch (IOException e) {
                            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        btnSelectLogo.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            getImageLauncher.launch(intent);
        });

        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String address = etAddress.getText().toString().trim();
            String token = etAuthToken.getText().toString().trim();

            if (name.isEmpty() || token.isEmpty()) {
                Toast.makeText(this, "Institution name and auth token required!", Toast.LENGTH_SHORT).show();
                return;
            }
            ContentValues values = new ContentValues();
            values.put("name", name);
            values.put("address", address);
            values.put("logo", logoBytes);
            values.put("authToken", token);

            DatabaseHelper dbHelper = new DatabaseHelper(this);
            try {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                long id;
                if (editingInstitutionId != -1) {
                    // Update existing institution
                    id = db.update("institution", values, "id=?", new String[]{String.valueOf(editingInstitutionId)});
                    editingInstitutionId = -1;
                } else {
                    // Insert new
                    id = db.insert("institution", null, values);
                }
                db.close();

                if (id == -1) {
                    Toast.makeText(this, "Failed to save institution", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Institution saved", Toast.LENGTH_SHORT).show();
                    etName.setText(""); etAddress.setText(""); etAuthToken.setText(""); ivLogo.setImageResource(R.drawable.ic_default_profile); logoBytes = null;
                    loadInstitutionsList();
                }
            } catch (Exception e) {
                Toast.makeText(this, "DB error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        lvInstitutions.setOnItemClickListener((parent, view, position, id) -> {
            int instId = instIds.get(position);
            Intent i = new Intent(AddInstitutionActivity.this, AddAlumniActivity.class);
            i.putExtra("default_institution_id", instId);
            startActivity(i);
        });

        lvInstitutions.setOnItemLongClickListener((parent, view, position, id) -> {
            int instId = instIds.get(position);
            String instName = instNames.get(position);
            new AlertDialog.Builder(AddInstitutionActivity.this)
                    .setTitle("Edit Institution")
                    .setMessage("Edit \"" + instName + "\" details?")
                    .setPositiveButton("Yes", (dialog, which) -> loadForEdit(instId))
                    .setNegativeButton("No", null)
                    .show();
            return true;
        });

        loadInstitutionsList();
    }

    private void loadInstitutionsList() {
        instNames.clear();
        instIds.clear();
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query("institution", null, null, null, null, null, "name ASC");
        while (c.moveToNext()) {
            instNames.add(c.getString(c.getColumnIndexOrThrow("name")));
            instIds.add(c.getInt(c.getColumnIndexOrThrow("id")));
        }
        c.close();
        db.close();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, instNames);
        lvInstitutions.setAdapter(adapter);
    }

    private void loadForEdit(int instId) {
        editingInstitutionId = instId;
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query("institution", null, "id=?", new String[]{String.valueOf(instId)}, null, null, null);
        if (cursor.moveToFirst()) {
            etName.setText(cursor.getString(cursor.getColumnIndexOrThrow("name")));
            etAddress.setText(cursor.getString(cursor.getColumnIndexOrThrow("address")));
            etAuthToken.setText(cursor.getString(cursor.getColumnIndexOrThrow("authToken")));
            byte[] img = cursor.getBlob(cursor.getColumnIndexOrThrow("logo"));
            if (img != null && img.length > 0) {
                Bitmap bmp = BitmapFactory.decodeByteArray(img, 0, img.length);
                ivLogo.setImageBitmap(bmp);
                logoBytes = img;
            } else {
                ivLogo.setImageResource(R.drawable.ic_default_profile);
                logoBytes = null;
            }
        }
        cursor.close();
        db.close();
    }
}
