package com.kirubas.alumniapp1;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import androidx.appcompat.app.AlertDialog;

public class AlumniPollsActivity extends AppCompatActivity {

    ListView lvPolls;
    ArrayList<Integer> pollIds = new ArrayList<>();
    ArrayList<String> pollQuestions = new ArrayList<>();
    ArrayAdapter<String> pollsAdapter;

    int alumniId;
    int institutionId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alumni_polls);

        lvPolls = findViewById(R.id.lvPolls);

        alumniId = getIntent().getIntExtra("alumni_id", -1);
        institutionId = getIntent().getIntExtra("institution_id", -1);
        if (alumniId == -1 || institutionId == -1) {
            Toast.makeText(this, "Missing alumni or institution ID", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        pollIds.clear();
        pollQuestions.clear();

        loadPolls();

        pollsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, pollQuestions);
        lvPolls.setAdapter(pollsAdapter);

        lvPolls.setOnItemClickListener((parent, view, position, id) -> {
            int selectedPollId = pollIds.get(position);
            // Show voting dialog for this poll
            showPollVoteDialog(selectedPollId);
        });
    }

    private void loadPolls() {
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor c = db.query("institutionPolls",
                new String[]{"id", "question", "status"},
                "institutionId=?",
                new String[]{String.valueOf(institutionId)},
                null, null, "createdAt DESC");

        while (c.moveToNext()) {
            int id = c.getInt(c.getColumnIndexOrThrow("id"));
            String question = c.getString(c.getColumnIndexOrThrow("question"));
            String status = c.getString(c.getColumnIndexOrThrow("status"));
            pollIds.add(id);
            String display = question + (status.equalsIgnoreCase("completed") ? " (Completed)" : "");
            pollQuestions.add(display);
        }
        c.close();
        db.close();
    }

    private void showPollVoteDialog(int pollId) {
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cOptions = db.query("pollOptions", new String[]{"id", "optionText"},
                "pollId=?", new String[]{String.valueOf(pollId)},
                null, null, null);
        ArrayList<String> options = new ArrayList<>();
        ArrayList<Integer> optionIds = new ArrayList<>();
        while (cOptions.moveToNext()) {
            optionIds.add(cOptions.getInt(cOptions.getColumnIndexOrThrow("id")));
            options.add(cOptions.getString(cOptions.getColumnIndexOrThrow("optionText")));
        }
        cOptions.close();

        // Check if poll completed
        Cursor cStatus = db.query("institutionPolls", new String[]{"status"}, "id=?", new String[]{String.valueOf(pollId)}, null, null, null);
        String pollStatus = "";
        if (cStatus.moveToFirst()) {
            pollStatus = cStatus.getString(cStatus.getColumnIndexOrThrow("status"));
        }
        cStatus.close();
        db.close();

        if (pollStatus.equalsIgnoreCase("completed")) {
            Toast.makeText(this, "Poll is completed and no longer accepts responses.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if user already answered
        db = dbHelper.getReadableDatabase();
        Cursor cAnswer = db.query("pollAnswers", new String[]{"pollOptionId"},
                "pollId=? AND alumniId=?", new String[]{String.valueOf(pollId), String.valueOf(alumniId)},
                null, null, null);

        int checkedIndex = -1;
        if (cAnswer.moveToFirst()) {
            int answeredOptionId = cAnswer.getInt(cAnswer.getColumnIndexOrThrow("pollOptionId"));
            for (int i = 0; i < optionIds.size(); i++) {
                if (optionIds.get(i) == answeredOptionId) {
                    checkedIndex = i;
                    break;
                }
            }
        }
        cAnswer.close();
        db.close();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Vote");

        final int[] selected = {checkedIndex};
        builder.setSingleChoiceItems(options.toArray(new String[0]), checkedIndex, (dialog, which) -> selected[0] = which);

        builder.setPositiveButton("Submit", (dialog, which) -> {
            if (selected[0] == -1) {
                Toast.makeText(this, "Please select an option.", Toast.LENGTH_SHORT).show();
                return;
            }
            submitVote(pollId, optionIds.get(selected[0]));
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void submitVote(int pollId, int pollOptionId) {
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        // Delete any previous answer for this user and poll
        db.delete("pollAnswers", "pollId=? AND alumniId=?", new String[]{String.valueOf(pollId), String.valueOf(alumniId)});

        ContentValues cv = new ContentValues();
        cv.put("pollId", pollId);
        cv.put("pollOptionId", pollOptionId);
        cv.put("alumniId", alumniId);

        long res = db.insert("pollAnswers", null, cv);
        db.close();

        if (res == -1) {
            Toast.makeText(this, "Failed to submit vote.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Vote submitted successfully!", Toast.LENGTH_SHORT).show();
        }
    }
}
