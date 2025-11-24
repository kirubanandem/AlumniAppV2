package com.kirubas.alumniapp1;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;

public class AddInstitutionPollActivity extends AppCompatActivity {

    EditText etQuestion, etOption;
    Button btnAddOption, btnSavePoll;
    ListView lvOptions;

    ArrayList<String> options = new ArrayList<>();
    ArrayAdapter<String> optionsAdapter;

    int institutionId = -1;
    String creatorType;   // "adminUser" or "IAAM"
    int creatorId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_institution_poll);

        etQuestion = findViewById(R.id.etQuestion);
        etOption = findViewById(R.id.etOption);
        btnAddOption = findViewById(R.id.btnAddOption);
        btnSavePoll = findViewById(R.id.btnSavePoll);
        lvOptions = findViewById(R.id.lvOptions);

        institutionId = getIntent().getIntExtra("institution_id", -1);
        creatorType = getIntent().getStringExtra("creator_type");  // must be "adminUser" or "IAAM"
        creatorId = getIntent().getIntExtra("creator_id", -1);

        if (institutionId == -1 || creatorId == -1 || creatorType == null) {
            Toast.makeText(this, "Missing institution or creator information", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        optionsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, options);
        lvOptions.setAdapter(optionsAdapter);

        btnAddOption.setOnClickListener(v -> {
            String optionText = etOption.getText().toString().trim();
            if (optionText.isEmpty()) {
                Toast.makeText(this, "Enter option text", Toast.LENGTH_SHORT).show();
                return;
            }
            options.add(optionText);
            optionsAdapter.notifyDataSetChanged();
            etOption.setText("");
        });

        btnSavePoll.setOnClickListener(v -> savePoll());
    }

    private void savePoll() {
        String question = etQuestion.getText().toString().trim();
        if (question.isEmpty()) {
            Toast.makeText(this, "Enter poll question", Toast.LENGTH_SHORT).show();
            return;
        }
        if (options.isEmpty()) {
            Toast.makeText(this, "Add at least one option", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues pollCv = new ContentValues();
        pollCv.put("institutionId", institutionId);
        pollCv.put("creatorType", creatorType);
        pollCv.put("creatorId", creatorId);
        pollCv.put("question", question);
        pollCv.put("status", "new");

        long pollId = db.insert("institutionPolls", null, pollCv);
        if (pollId == -1) {
            Toast.makeText(this, "Failed to create poll", Toast.LENGTH_SHORT).show();
            db.close();
            return;
        }

        for (String option : options) {
            ContentValues optionCv = new ContentValues();
            optionCv.put("pollId", pollId);
            optionCv.put("optionText", option);
            optionCv.put("votes", 0);
            db.insert("pollOptions", null, optionCv);
        }
        db.close();
        Toast.makeText(this, "Poll created successfully", Toast.LENGTH_SHORT).show();
        finish();
    }
}
