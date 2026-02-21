package com.example.clubeventportal;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private EditText etEmail, etPassword, etName;
    private Spinner spRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Check if already logged in
        if (auth.getCurrentUser() != null) {
            routeUser(auth.getUid());
        }

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        spRole = findViewById(R.id.spRole);
        Button btnAction = findViewById(R.id.btnLogin);

        // Setup Spinner
        String[] roles = {"Student", "Club Admin", "Super Admin"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, roles);
        spRole.setAdapter(adapter);

        btnAction.setOnClickListener(v -> {
            String email = etEmail.getText().toString();
            String pass = etPassword.getText().toString();
            String name = etName.getText().toString();
            String role = spRole.getSelectedItem().toString();

            if (!email.isEmpty() && !pass.isEmpty()) {
                loginOrRegister(email, pass, name, role);
            }
        });
    }

    private void loginOrRegister(String email, String pass, String name, String role) {
        auth.signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener(authResult -> routeUser(authResult.getUser().getUid()))
                .addOnFailureListener(e -> {
                    // If login fails, try to register
                    if(name.isEmpty()) {
                        Toast.makeText(this, "Name is required for new registration", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    auth.createUserWithEmailAndPassword(email, pass)
                            .addOnSuccessListener(authResult -> {
                                User user = new User(auth.getUid(), name, email, role);
                                db.collection("users").document(auth.getUid()).set(user)
                                        .addOnSuccessListener(aVoid -> routeUser(auth.getUid()));
                            })
                            .addOnFailureListener(e1 -> Toast.makeText(this, "Error: " + e1.getMessage(), Toast.LENGTH_SHORT).show());
                });
    }

    private void routeUser(String uid) {
        db.collection("users").document(uid).get().addOnSuccessListener(snapshot -> {
            if (!snapshot.exists()) {
                // User might have been deleted by Super Admin
                Toast.makeText(this, "Account blocked or deleted.", Toast.LENGTH_LONG).show();
                auth.signOut();
                return;
            }

            User user = snapshot.toObject(User.class);
            if (user == null) return;

            if ("Super Admin".equalsIgnoreCase(user.getRole())) {
                startActivity(new Intent(this, SuperAdminActivity.class));
                finish();
            } else if ("Club Admin".equalsIgnoreCase(user.getRole())) {
                checkForClub(uid); // Check if they need to setup a club
            } else {
                // Student
                startActivity(new Intent(this, MainActivity.class));
                finish();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Network Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void checkForClub(String adminId) {
        db.collection("clubs").whereEqualTo("adminId", adminId).get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        // Admin has no club -> Go to Create Club
                        startActivity(new Intent(this, CreateClubActivity.class));
                    } else {
                        // Admin has a club -> Go to Main
                        startActivity(new Intent(this, MainActivity.class));
                    }
                    finish();
                });
    }
}