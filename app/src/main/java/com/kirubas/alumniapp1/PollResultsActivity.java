package com.kirubas.alumniapp1;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;

public class PollResultsActivity extends AppCompatActivity {

    ListView lvResults;
    ArrayAdapter<String> adapter;
    ArrayList<String> resultsList = new ArrayList<>();

    int pollId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_poll_results);

        lvResults = findViewById(R.id.lvResults);
        pollId = getIntent().getIntExtra("poll_id", -1);

        if (pollId == -1) {
            Toast.makeText(this, "Invalid Poll ID", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        loadPollResults();
    }

    private void loadPollResults() {
        resultsList.clear();

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Query to get options and their vote counts
        String query = "SELECT po.optionText, COUNT(pa.id) AS voteCount " +
                "FROM pollOptions po LEFT JOIN pollAnswers pa ON po.id = pa.pollOptionId " +
                "AND pa.pollId = ? " +
                "WHERE po.pollId = ? " +
                "GROUP BY po.id, po.optionText " +
                "ORDER BY voteCount DESC";

        Cursor cursor = db.rawQuery(query, new String[] { String.valueOf(pollId), String.valueOf(pollId) });

        while (cursor.moveToNext()) {
            String optionText = cursor.getString(cursor.getColumnIndexOrThrow("optionText"));
            int voteCount = cursor.getInt(cursor.getColumnIndexOrThrow("voteCount"));

            resultsList.add(optionText + ": " + voteCount + " votes");
        }
        cursor.close();
        db.close();

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, resultsList);
        lvResults.setAdapter(adapter);
    }
}
