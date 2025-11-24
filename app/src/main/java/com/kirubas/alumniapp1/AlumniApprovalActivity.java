package com.kirubas.alumniapp1;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;

public class AlumniApprovalActivity extends AppCompatActivity {

    ListView lvRequests;
    ImageView ivProfilePic;
    TextView tvName, tvEmail, tvPhone, tvGender, tvDOB, tvInstitution, tvDept, tvYears, tvGradYear;
    Button btnApprove, btnReject, btnPending;

    ArrayList<Integer> alumniIds = new ArrayList<>();
    ArrayList<Integer> institutionIds = new ArrayList<>();
    ArrayList<String> requestDisplay = new ArrayList<>();

    int selectedIndex = -1;

    String userType;
    int userId = -1;           // admin user id
    int institutionIdUser = -1; // IAAM institution id

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alumni_approval);

        lvRequests = findViewById(R.id.lvRequests);
        ivProfilePic = findViewById(R.id.ivProfilePic);
        tvName = findViewById(R.id.tvName);
        tvEmail = findViewById(R.id.tvEmail);
        tvPhone = findViewById(R.id.tvPhone);
        tvGender = findViewById(R.id.tvGender);
        tvDOB = findViewById(R.id.tvDOB);
        tvInstitution = findViewById(R.id.tvInstitution);
        tvDept = findViewById(R.id.tvDept);
        tvYears = findViewById(R.id.tvYears);
        tvGradYear = findViewById(R.id.tvGradYear);

        btnApprove = findViewById(R.id.btnApprove);
        btnReject = findViewById(R.id.btnReject);
        btnPending = findViewById(R.id.btnPending);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            userType = extras.getString("user_type", "");
            if ("admin".equals(userType)) {
                userId = extras.getInt("user_id", -1);
            } else if ("iaam".equals(userType)) {
                institutionIdUser = extras.getInt("institution_id", -1);
            }
        }

        loadPendingRequests();

        lvRequests.setOnItemClickListener((parent, view, position, id) -> {
            selectedIndex = position;
            displayAlumniRequestDetails(position);
        });

        btnApprove.setOnClickListener(v -> setRequestStatus(1));
        btnReject.setOnClickListener(v -> setRequestStatus(2));
        btnPending.setOnClickListener(v -> setRequestStatus(0));
    }

    private void loadPendingRequests() {
        requestDisplay.clear();
        alumniIds.clear();
        institutionIds.clear();

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        StringBuilder sb = new StringBuilder();
        sb.append("SELECT ai.alumniId, ai.institutionId, a.firstName, a.lastName, inst.name ");
        sb.append("FROM alumniInstitution ai ");
        sb.append("JOIN alumni a ON ai.alumniId = a.id ");
        sb.append("JOIN institution inst ON ai.institutionId = inst.id ");
        sb.append("WHERE ai.approved = 0 ");

        ArrayList<String> args = new ArrayList<>();

        if ("iaam".equals(userType) && institutionIdUser != -1) {
            sb.append("AND ai.institutionId = ? ");
            args.add(String.valueOf(institutionIdUser));
        } else if ("admin".equals(userType) && userId != -1) {
            sb.append("AND ai.institutionId IN (SELECT institutionId FROM adminInstitution WHERE adminId = ?) ");
            args.add(String.valueOf(userId));
        }

        sb.append("ORDER BY inst.name ASC");

        Cursor c = db.rawQuery(sb.toString(), args.isEmpty() ? null : args.toArray(new String[0]));
        while (c.moveToNext()) {
            int alumniId = c.getInt(c.getColumnIndexOrThrow("alumniId"));
            int institutionId = c.getInt(c.getColumnIndexOrThrow("institutionId"));
            String alumName = c.getString(c.getColumnIndexOrThrow("firstName")) + " " +
                    c.getString(c.getColumnIndexOrThrow("lastName"));
            String institutionName = c.getString(c.getColumnIndexOrThrow("name"));

            alumniIds.add(alumniId);
            institutionIds.add(institutionId);
            requestDisplay.add(alumName + " - " + institutionName);
        }
        c.close();
        db.close();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, requestDisplay);
        lvRequests.setAdapter(adapter);

        clearProfileDetails();
    }

    private void displayAlumniRequestDetails(int position) {
        if (position < 0 || position >= alumniIds.size()) return;

        int alumniId = alumniIds.get(position);
        int institutionId = institutionIds.get(position);

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor ac = db.query("alumni", null, "id=?",
                new String[]{String.valueOf(alumniId)}, null, null, null);
        if (ac.moveToFirst()) {
            tvName.setText(ac.getString(ac.getColumnIndexOrThrow("firstName")) + " " +
                    ac.getString(ac.getColumnIndexOrThrow("lastName")));
            tvEmail.setText(ac.getString(ac.getColumnIndexOrThrow("email")));
            tvPhone.setText(ac.getString(ac.getColumnIndexOrThrow("phone")));
            tvGender.setText(ac.getString(ac.getColumnIndexOrThrow("gender")));
            tvDOB.setText(ac.getString(ac.getColumnIndexOrThrow("dateOfBirth")));

            byte[] imgBytes = ac.getBlob(ac.getColumnIndexOrThrow("profilePicture"));
            if (imgBytes != null && imgBytes.length > 0) {
                Bitmap bmp = BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.length);
                ivProfilePic.setImageBitmap(bmp);
            } else {
                ivProfilePic.setImageResource(R.drawable.ic_default_profile);
            }
        }
        ac.close();

        Cursor ic = db.query("institution", null, "id=?",
                new String[]{String.valueOf(institutionId)}, null, null, null);
        if (ic.moveToFirst()) {
            tvInstitution.setText(ic.getString(ic.getColumnIndexOrThrow("name")));
        }
        ic.close();

        Cursor ai = db.query("alumniInstitution", null,
                "alumniId=? AND institutionId=?",
                new String[]{String.valueOf(alumniId), String.valueOf(institutionId)},
                null, null, null);
        if (ai.moveToFirst()) {
            tvDept.setText(ai.getString(ai.getColumnIndexOrThrow("department")));
            tvYears.setText(ai.getInt(ai.getColumnIndexOrThrow("fromYear")) + " - " +
                    ai.getInt(ai.getColumnIndexOrThrow("toYear")));
            tvGradYear.setText(String.valueOf(ai.getInt(ai.getColumnIndexOrThrow("graduationYear"))));
        }
        ai.close();
        db.close();
    }

    private void clearProfileDetails() {
        tvName.setText("");
        tvEmail.setText("");
        tvPhone.setText("");
        tvGender.setText("");
        tvDOB.setText("");
        tvInstitution.setText("");
        tvDept.setText("");
        tvYears.setText("");
        tvGradYear.setText("");
        ivProfilePic.setImageResource(R.drawable.ic_default_profile);
        selectedIndex = -1;
    }

    private void setRequestStatus(int status) {
        if (selectedIndex == -1) {
            Toast.makeText(this, "Select a join request first", Toast.LENGTH_SHORT).show();
            return;
        }

        int alumniId = alumniIds.get(selectedIndex);
        int institutionId = institutionIds.get(selectedIndex);

        ContentValues cv = new ContentValues();
        cv.put("approved", status);

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rows = db.update("alumniInstitution", cv,
                "alumniId=? AND institutionId=?",
                new String[]{String.valueOf(alumniId), String.valueOf(institutionId)});
        db.close();

        if (rows > 0) {
            String msg = status == 1 ? "Approved" : (status == 2 ? "Rejected" : "Set to Pending");
            Toast.makeText(this, "Request " + msg, Toast.LENGTH_SHORT).show();
            loadPendingRequests();
        } else {
            Toast.makeText(this, "Failed to update request status", Toast.LENGTH_SHORT).show();
        }
    }
}
