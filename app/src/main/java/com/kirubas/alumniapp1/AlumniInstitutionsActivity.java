package com.kirubas.alumniapp1;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;

public class AlumniInstitutionsActivity extends AppCompatActivity {

    ListView lvInstitutions;
    ArrayList<String> joinedInstitutions = new ArrayList<>();
    ArrayList<Integer> joinedInstitutionIds = new ArrayList<>();
    int alumniId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alumni_institutions);

        lvInstitutions = findViewById(R.id.lvInstitutions);
        alumniId = getIntent().getIntExtra("alumni_id", -1);
        if (alumniId == -1) {
            Toast.makeText(this, "Invalid alumni ID", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        loadJoinedInstitutions();

        lvInstitutions.setOnItemClickListener((parent, view, position, id) -> {
            int institutionId = joinedInstitutionIds.get(position);
            Intent intent = new Intent(this, InstitutionLandingActivity.class);
            intent.putExtra("institution_id", institutionId);
            intent.putExtra("alumni_id", alumniId);
            startActivity(intent);
        });

        Button addNewInstitute=findViewById(R.id.addNewInstitute);
        addNewInstitute.setOnClickListener(v -> {
            Intent i = new Intent(this, AlumniInstitutionRequestActivity.class);
            startActivity(i);
        });
    }

    private void loadJoinedInstitutions() {
        joinedInstitutions.clear();
        joinedInstitutionIds.clear();

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT institution.id, institution.name FROM institution " +
                        "INNER JOIN alumniInstitution ON institution.id = alumniInstitution.institutionId " +
                        "WHERE alumniInstitution.alumniId = ? AND alumniInstitution.approved = 1",
                new String[]{String.valueOf(alumniId)});
        while (c.moveToNext()) {
            joinedInstitutionIds.add(c.getInt(c.getColumnIndexOrThrow("id")));
            joinedInstitutions.add(c.getString(c.getColumnIndexOrThrow("name")));
        }
        c.close();
        db.close();

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, joinedInstitutions);
        lvInstitutions.setAdapter(adapter);
    }
}
