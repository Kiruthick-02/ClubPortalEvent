package com.example.clubeventportal;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.UUID;

public class CreateClubActivity extends AppCompatActivity {

    private EditText etClubName, etDesc;
    private Button btnCreate;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_club);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        etClubName = findViewById(R.id.etClubName);
        etDesc = findViewById(R.id.etClubDesc);
        btnCreate = findViewById(R.id.btnCreateClub);

        btnCreate.setOnClickListener(v -> createClub());
    }

    private void createClub() {
        String name = etClubName.getText().toString().trim();
        String desc = etDesc.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(this, "Club Name is required", Toast.LENGTH_SHORT).show();
            return;
        }

        btnCreate.setEnabled(false);
        String clubId = UUID.randomUUID().toString();
        String adminId = auth.getUid();

        // FIX: Passed 'null' as the 5th argument for profileImage
        // The admin can add the image later in "Edit Club Profile"
        Club club = new Club(clubId, name, desc, adminId, null);

        db.collection("clubs").document(clubId).set(club)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Club Created Successfully!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnCreate.setEnabled(true);
                });
    }
}