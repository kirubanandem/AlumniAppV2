package com.kirubas.alumniapp1;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class ControlPanelActivity extends AppCompatActivity {

    Button btnManageAlumniUDFFields, btnManageAdminAssignments, btnManageAlumniApproval, btnManagePolls, btnManageEvents;

    String userType;
    int userId = -1;          // for admin
    int institutionId = -1;   // for IAAM

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control_panel);

        btnManageAlumniUDFFields = findViewById(R.id.btnManageAlumniUDFFields);
        btnManageAdminAssignments = findViewById(R.id.btnManageAdminAssignments);
        btnManageAlumniApproval = findViewById(R.id.btnManageAlumniApproval);
        btnManagePolls = findViewById(R.id.btnManagePolls);
        btnManageEvents = findViewById(R.id.btnManageEvents);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            userType = extras.getString("user_type", "");
            if (userType.equals("admin")) {
                userId = extras.getInt("user_id", -1);
            } else if (userType.equals("iaam")) {
                institutionId = extras.getInt("institution_id", -1);
            }
        }
        btnManageAlumniUDFFields.setOnClickListener(v -> {
            Intent intent = new Intent(this, UdfFieldListActivity.class);
            if (userType.equals("admin")) {
                intent.putExtra("user_id", userId);
            } else if (userType.equals("iaam")) {
                intent.putExtra("institution_id", institutionId);
            }
            intent.putExtra("user_type", userType);
            startActivity(intent);
        });

        btnManageAdminAssignments.setOnClickListener(v -> {
            Intent intent = new Intent(this, InstitutionAdminAssignmentActivity.class);
            if (userType.equals("admin")) {
                intent.putExtra("user_id", userId);
            } else if (userType.equals("iaam")) {
                intent.putExtra("institution_id", institutionId);
            }
            intent.putExtra("user_type", userType);
            startActivity(intent);
        });

        btnManageAlumniApproval.setOnClickListener(v -> {
            Intent intent = new Intent(this, AlumniApprovalActivity.class);
            if (userType.equals("admin")) {
                intent.putExtra("user_id", userId);
            } else if (userType.equals("iaam")) {
                intent.putExtra("institution_id", institutionId);
            }
            intent.putExtra("user_type", userType);
            startActivity(intent);
        });

        btnManagePolls.setOnClickListener(v -> {
            Intent intent = new Intent(this, PollManagementActivity.class);
            if (userType.equals("admin")) {
                intent.putExtra("user_id", userId);
            } else if (userType.equals("iaam")) {
                intent.putExtra("institution_id", institutionId);
            }
            intent.putExtra("user_type", userType);
            startActivity(intent);
        });

        btnManageEvents.setOnClickListener(v -> {
            Intent intent = new Intent(this, EventManagementActivity.class);
            if (userType.equals("admin")) {
                intent.putExtra("user_id", userId);
            } else if (userType.equals("iaam")) {
                intent.putExtra("institution_id", institutionId);
            }
            intent.putExtra("user_type", userType);
            startActivity(intent);
        });
    }
}
