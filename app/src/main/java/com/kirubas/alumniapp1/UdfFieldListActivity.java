package com.kirubas.alumniapp1;

import android.content.ContentValues;
import android.database.Cursor;
import android.view.View;
import android.widget.AdapterView;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_udf_field_list);

        spnInstitutes = findViewById(R.id.spnInstitutes);
        etFieldName = findViewById(R.id.etFieldName);
        etFieldType = findViewById(R.id.etFieldType);
        btnAddField = findViewById(R.id.btnAddField);
        lvFields = findViewById(R.id.lvFields);

        loadInstitutes();

        spnInstitutes.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                selectedInstituteId = instituteIds.get(pos);
                loadFields();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
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
        Cursor c = db.query("institution", null, null, null, null, null, "name ASC");
        while (c.moveToNext()) {
            instituteNames.add(c.getString(c.getColumnIndexOrThrow("name")));
            instituteIds.add(c.getInt(c.getColumnIndexOrThrow("id")));
        }
        c.close();
        db.close();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, instituteNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnInstitutes.setAdapter(adapter);
        if (!instituteIds.isEmpty()) selectedInstituteId = instituteIds.get(0);
        else selectedInstituteId = -1;
    }

    private void loadFields() {
        fieldDisplay.clear();
        fieldIds.clear();
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query("userDefinedFields", null, "institutionId=?", new String[]{String.valueOf(selectedInstituteId)}, null, null, "fieldName ASC");
        while (c.moveToNext()) {
            fieldDisplay.add(
                    c.getString(c.getColumnIndexOrThrow("fieldName")) + " (" +
                            c.getString(c.getColumnIndexOrThrow("fieldType")) + ")"
            );
            fieldIds.add(c.getInt(c.getColumnIndexOrThrow("id")));
        }
        c.close();
        db.close();
        lvFields.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, fieldDisplay));
    }
}
