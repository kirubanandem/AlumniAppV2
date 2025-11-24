package com.kirubas.alumniapp1;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;

public class UdfFieldListActivity extends AppCompatActivity {

    Spinner spnInstitutes;
    EditText etFieldName, etFieldType;
    Button btnAddField;
    ListView lvFields;

    ArrayList<String> instituteNames = new ArrayList<>();
    ArrayList<Integer> instituteIds = new ArrayList<>();
    ArrayList<String> fieldDisplay = new ArrayList<>();
    ArrayList<Integer> fieldIds = new ArrayList<>();
    int selectedInstituteId = -1;

    String userType;  // "admin" or "iaam"
    int userId = -1;  // adminId or institutionId depending on userType

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_udf_field_list);

        spnInstitutes = findViewById(R.id.spnInstitutes);
        etFieldName = findViewById(R.id.etFieldName);
        etFieldType = findViewById(R.id.etFieldType);
        btnAddField = findViewById(R.id.btnAddField);
        lvFields = findViewById(R.id.lvFields);

        userType = getIntent().getStringExtra("user_type");
        userId = getIntent().getIntExtra("user_id", -1);
        if (userType == null) userType = "";
        if (userId == -1) {
            Toast.makeText(this, "Invalid user info", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        loadInstitutes();

        spnInstitutes.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int pos, long id) {
                selectedInstituteId = instituteIds.get(pos);
                loadFields();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        btnAddField.setOnClickListener(v -> {
            if (selectedInstituteId == -1) {
                Toast.makeText(this, "Select an institute", Toast.LENGTH_SHORT).show();
                return;
            }
            String fieldName = etFieldName.getText().toString().trim();
            String fieldType = etFieldType.getText().toString().trim();
            if (fieldName.isEmpty() || fieldType.isEmpty()) {
                Toast.makeText(this, "Both field name and type required", Toast.LENGTH_SHORT).show();
                return;
            }

            // TODO: Optional - add permission validation depending on userType and institution

            ContentValues cv = new ContentValues();
            cv.put("fieldName", fieldName);
            cv.put("fieldType", fieldType);
            cv.put("institutionId", selectedInstituteId);

            DatabaseHelper dbHelper = new DatabaseHelper(this);
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            long id = db.insert("userDefinedFields", null, cv);
            db.close();
            if (id == -1) {
                Toast.makeText(this, "Failed to save field (maybe duplicate name)", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
                etFieldName.setText("");
                etFieldType.setText("");
                loadFields();
            }
        });

        lvFields.setOnItemLongClickListener((parent, view, position, id) -> {
            final int fieldId = fieldIds.get(position);
            new AlertDialog.Builder(UdfFieldListActivity.this)
                    .setTitle("Delete Field")
                    .setMessage("Delete this user-defined field?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        DatabaseHelper dbHelper = new DatabaseHelper(this);
                        SQLiteDatabase db = dbHelper.getWritableDatabase();
                        db.delete("userDefinedFields", "id=?", new String[]{String.valueOf(fieldId)});
                        db.close();
                        Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();
                        loadFields();
                    })
                    .setNegativeButton("No", null)
                    .show();
            return true;
        });
    }

    private void loadInstitutes() {
        instituteNames.clear();
        instituteIds.clear();

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor;
        if ("admin".equals(userType)) {
            // Load institutions linked to admin via adminInstitution mapping
            String query = "SELECT institution.id, institution.name FROM institution " +
                    "INNER JOIN adminInstitution ON institution.id = adminInstitution.institutionId " +
                    "WHERE adminInstitution.adminId = ?";
            cursor = db.rawQuery(query, new String[]{String.valueOf(userId)});
        } else if ("iaam".equals(userType)) {
            // IAAM user manages exactly one institution = userId
            cursor = db.query("institution", null, "id=?",
                    new String[]{String.valueOf(userId)}, null, null, null);
        } else {
            cursor = db.query("institution", new String[]{"id", "name"}, null, null, null, null, "name ASC");
        }

        while (cursor.moveToNext()) {
            instituteIds.add(cursor.getInt(cursor.getColumnIndexOrThrow("id")));
            instituteNames.add(cursor.getString(cursor.getColumnIndexOrThrow("name")));
        }
        cursor.close();
        db.close();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, instituteNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnInstitutes.setAdapter(adapter);

        if (!instituteIds.isEmpty()) {
            selectedInstituteId = instituteIds.get(0);
            spnInstitutes.setSelection(0);
        } else {
            selectedInstituteId = -1;
        }
    }

    private void loadFields() {
        fieldDisplay.clear();
        fieldIds.clear();

        if (selectedInstituteId == -1) {
            lvFields.setAdapter(null);
            return;
        }

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor c = db.query("userDefinedFields", null, "institutionId=?", new String[]{String.valueOf(selectedInstituteId)}, null, null, "fieldName ASC");
        while (c.moveToNext()) {
            String disp = c.getString(c.getColumnIndexOrThrow("fieldName")) + " (" + c.getString(c.getColumnIndexOrThrow("fieldType")) + ")";
            fieldDisplay.add(disp);
            fieldIds.add(c.getInt(c.getColumnIndexOrThrow("id")));
        }
        c.close();
        db.close();

        lvFields.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, fieldDisplay));
    }
}
