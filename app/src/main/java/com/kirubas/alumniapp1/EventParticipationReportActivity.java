package com.kirubas.alumniapp1;

import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import android.database.Cursor;

public class EventParticipationReportActivity extends AppCompatActivity {

    ListView lvReport;
    ArrayAdapter<String> adapter;
    ArrayList<String> reportList = new ArrayList<>();

    int eventId;
    int institutionId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_participation_report);

        lvReport = findViewById(R.id.lvReport);

        eventId = getIntent().getIntExtra("event_id", -1);
        institutionId = getIntent().getIntExtra("institution_id", -1);

        if (eventId == -1 || institutionId == -1) {
            Toast.makeText(this, "Invalid event or institution ID", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        loadReport();
    }

    private void loadReport() {
        reportList.clear();

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Query for status counts
        String queryStatus = "SELECT status, COUNT(*) AS count FROM eventParticipation " +
                "WHERE eventId = ? GROUP BY status";
        Cursor cStatus = db.rawQuery(queryStatus, new String[]{String.valueOf(eventId)});
        while (cStatus.moveToNext()) {
            String status = cStatus.getString(cStatus.getColumnIndexOrThrow("status"));
            int count = cStatus.getInt(cStatus.getColumnIndexOrThrow("count"));
            reportList.add(status + ": " + count);
        }
        cStatus.close();

        // Query for not responded count
        String queryNotResponded = "SELECT COUNT(*) as noResponse FROM alumni a " +
                "INNER JOIN alumniInstitution ai ON a.id = ai.alumniId AND ai.approved = 1 " +
                "WHERE ai.institutionId = ? AND a.id NOT IN (SELECT alumniId FROM eventParticipation WHERE eventId = ?)";
        Cursor cNR = db.rawQuery(queryNotResponded, new String[]{String.valueOf(institutionId), String.valueOf(eventId)});
        if (cNR.moveToFirst()) {
            int noResponse = cNR.getInt(cNR.getColumnIndexOrThrow("noResponse"));
            reportList.add("Not Responded: " + noResponse);
        }
        cNR.close();
        db.close();

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, reportList);
        lvReport.setAdapter(adapter);
    }
}
