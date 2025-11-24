package com.kirubas.alumniapp1;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;

public class PollManagementActivity extends AppCompatActivity {

    ListView lvPolls;
    Button btnAddPoll;
    ArrayList<Integer> pollIds = new ArrayList<>();
    ArrayList<String> pollQuestions = new ArrayList<>();
    ArrayAdapter<String> adapter;

    String userType;
    int userId = -1;
    int institutionId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_poll_management);

        lvPolls = findViewById(R.id.lvPolls);
        btnAddPoll = findViewById(R.id.btnAddPoll);

        userType = getIntent().getStringExtra("user_type");
        if ("admin".equals(userType)) {
            userId = getIntent().getIntExtra("user_id", -1);
        } else if ("iaam".equals(userType)) {
            institutionId = getIntent().getIntExtra("institution_id", -1);
        }

        loadPolls();

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, pollQuestions);
        lvPolls.setAdapter(adapter);

        lvPolls.setOnItemClickListener((parent, view, position, id) -> {
            int pollId = pollIds.get(position);
            showPollOptionsDialog(pollId);
        });

        btnAddPoll.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddInstitutionPollActivity.class);
            if ("admin".equals(userType)) {
                intent.putExtra("creator_type", "adminUser");
                intent.putExtra("creator_id", userId);
                // Provide institutionId if needed to AddInstitutionPollActivity based on your logic
            } else if ("iaam".equals(userType)) {
                intent.putExtra("creator_type", "IAAM");
                intent.putExtra("creator_id", institutionId);
                intent.putExtra("institution_id", institutionId);
            }
            startActivity(intent);
        });
    }

    private void loadPolls() {
        pollIds.clear();
        pollQuestions.clear();

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String selection = null;
        String[] selectionArgs = null;

        if ("iaam".equals(userType)) {
            selection = "institutionId=?";
            selectionArgs = new String[]{String.valueOf(institutionId)};
        }

        Cursor c = db.query("institutionPolls", new String[]{"id", "question", "status"},
                selection, selectionArgs, null, null, "createdAt DESC");
        while (c.moveToNext()) {
            int id = c.getInt(c.getColumnIndexOrThrow("id"));
            String question = c.getString(c.getColumnIndexOrThrow("question"));
            String status = c.getString(c.getColumnIndexOrThrow("status"));

            pollIds.add(id);
            pollQuestions.add(question + (status.equalsIgnoreCase("completed") ? " (Completed)" : ""));
        }
        c.close();
        db.close();
    }

    private void showPollOptionsDialog(int pollId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select an action");

        String[] options = {"View Results", "Edit Poll", "Delete Poll"};
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0:
                    // View Results Activity
                    Intent resultIntent = new Intent(this, PollResultsActivity.class);
                    resultIntent.putExtra("poll_id", pollId);
                    startActivity(resultIntent);
                    break;
                case 1:
                    Toast.makeText(this, "Edit Poll feature to be implemented", Toast.LENGTH_SHORT).show();
                    // Redirect to edit poll activity if implemented
                    break;
                case 2:
                    confirmDeletePoll(pollId);
                    break;
            }
        });
        builder.show();
    }

    private void confirmDeletePoll(int pollId) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Poll")
                .setMessage("Are you sure you want to delete this poll?")
                .setPositiveButton("Yes", (dialog, which) -> deletePoll(pollId))
                .setNegativeButton("No", null)
                .show();
    }

    private void deletePoll(int pollId) {
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        int rows = db.delete("institutionPolls", "id=?", new String[]{String.valueOf(pollId)});
        db.close();

        if (rows > 0) {
            Toast.makeText(this, "Poll deleted", Toast.LENGTH_SHORT).show();
            loadPolls();
            adapter.notifyDataSetChanged();
        } else {
            Toast.makeText(this, "Failed to delete poll", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPolls();
        adapter.notifyDataSetChanged();
    }
}
