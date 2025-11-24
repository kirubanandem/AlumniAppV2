package com.kirubas.alumniapp1;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import java.util.ArrayList;

public class InstitutionAdminAssignmentActivity extends AppCompatActivity {

    int institutionId;
    Spinner spnAdminUsers;
    Button btnAssignAdmin;
    ListView lvAssignedAdmins;

    ArrayList<String> adminNames = new ArrayList<>();
    ArrayList<Integer> adminIds = new ArrayList<>();

    ArrayList<String> assignedAdminNames = new ArrayList<>();
    ArrayList<Integer> assignedAdminIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_institution_admin_assignment);

        institutionId = getIntent().getIntExtra("institution_id", -1);
        if (institutionId == -1) {
            Toast.makeText(this, "Invalid institution", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        spnAdminUsers = findViewById(R.id.spnAdminUsers);
        btnAssignAdmin = findViewById(R.id.btnAssignAdmin);
        lvAssignedAdmins = findViewById(R.id.lvAssignedAdmins);

        loadAvailableAdminUsers();
        loadAssignedAdmins();

        btnAssignAdmin.setOnClickListener(v -> assignSelectedAdmin());
    }

    private void loadAvailableAdminUsers() {
        adminNames.clear();
        adminIds.clear();
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor c = db.query("adminUser", null, null, null, null, null, "username ASC");
        while (c.moveToNext()) {
            int id = c.getInt(c.getColumnIndexOrThrow("id"));
            String uname = c.getString(c.getColumnIndexOrThrow("username"));
            adminIds.add(id);
            adminNames.add(uname);
        }
        c.close();
        db.close();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, adminNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spnAdminUsers.setAdapter(adapter);
    }

    private void loadAssignedAdmins() {
        assignedAdminNames.clear();
        assignedAdminIds.clear();

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor c = db.rawQuery(
                "SELECT au.id, au.username FROM adminUser au " +
                        "INNER JOIN adminInstitution ai ON au.id = ai.adminId " +
                        "WHERE ai.institutionId = ?",
                new String[]{String.valueOf(institutionId)});

        while (c.moveToNext()) {
            assignedAdminIds.add(c.getInt(c.getColumnIndexOrThrow("id")));
            assignedAdminNames.add(c.getString(c.getColumnIndexOrThrow("username")));
        }
        c.close();
        db.close();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, assignedAdminNames);
        lvAssignedAdmins.setAdapter(adapter);
    }

    private void assignSelectedAdmin() {
        int pos = spnAdminUsers.getSelectedItemPosition();
        if (pos == AdapterView.INVALID_POSITION) {
            Toast.makeText(this, "Select an admin user to assign", Toast.LENGTH_SHORT).show();
            return;
        }
        int adminId = adminIds.get(pos);

        if (assignedAdminIds.contains(adminId)) {
            Toast.makeText(this, "Admin user already assigned to this institution", Toast.LENGTH_SHORT).show();
            return;
        }

        ContentValues cv = new ContentValues();
        cv.put("adminId", adminId);
        cv.put("institutionId", institutionId);

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long result = db.insert("adminInstitution", null, cv);
        db.close();

        if (result == -1) {
            Toast.makeText(this, "Failed to assign admin user", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Admin user assigned successfully", Toast.LENGTH_SHORT).show();
            loadAssignedAdmins();
        }
    }
}
