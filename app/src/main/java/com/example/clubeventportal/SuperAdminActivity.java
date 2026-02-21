package com.example.clubeventportal;

import android.app.AlertDialog;
import android.content.Context; // <--- Fixed Error: Added Import
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat; // <--- Added for Theme Support
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SuperAdminActivity extends AppCompatActivity {

    private RecyclerView recyclerClubs, recyclerStudents;
    private ClubAdminAdapter clubAdapter;
    private StudentAdapter studentAdapter;

    private ArrayList<User> clubAdminList;
    private ArrayList<User> studentList;

    // Map to store AdminID -> ClubName for easy lookup
    private Map<String, String> adminClubMap = new HashMap<>();

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_super_admin);

        db = FirebaseFirestore.getInstance();

        recyclerClubs = findViewById(R.id.recyclerClubs);
        recyclerStudents = findViewById(R.id.recyclerStudents);

        // Setup Club Recycler
        recyclerClubs.setLayoutManager(new LinearLayoutManager(this));
        clubAdminList = new ArrayList<>();
        clubAdapter = new ClubAdminAdapter(clubAdminList);
        recyclerClubs.setAdapter(clubAdapter);

        // Setup Student Recycler
        recyclerStudents.setLayoutManager(new LinearLayoutManager(this));
        studentList = new ArrayList<>();
        studentAdapter = new StudentAdapter(studentList);
        recyclerStudents.setAdapter(studentAdapter);

        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        loadDashboardData();
    }

    private void loadDashboardData() {
        // 1. Fetch Clubs First (to get names)
        db.collection("clubs").get().addOnSuccessListener(clubSnapshots -> {
            adminClubMap.clear();
            for (DocumentSnapshot doc : clubSnapshots) {
                Club club = doc.toObject(Club.class);
                if (club != null) {
                    adminClubMap.put(club.getAdminId(), club.getClubName());
                }
            }

            // 2. Fetch Users after we have the club names
            fetchUsers();
        });
    }

    private void fetchUsers() {
        db.collection("users").get().addOnSuccessListener(userSnapshots -> {
            clubAdminList.clear();
            studentList.clear();

            for (DocumentSnapshot doc : userSnapshots) {
                User user = doc.toObject(User.class);
                if (user != null) {
                    if ("Club Admin".equalsIgnoreCase(user.getRole())) {
                        clubAdminList.add(user);
                    } else if ("Student".equalsIgnoreCase(user.getRole())) {
                        studentList.add(user);
                    }
                }
            }
            clubAdapter.notifyDataSetChanged();
            studentAdapter.notifyDataSetChanged();
        });
    }

    // --- ADAPTER 1: CLUB ADMINS ---
    class ClubAdminAdapter extends RecyclerView.Adapter<ClubAdminAdapter.ViewHolder> {
        ArrayList<User> list;
        public ClubAdminAdapter(ArrayList<User> list) { this.list = list; }

        @NonNull @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            User user = list.get(position);
            Context context = holder.itemView.getContext();

            // Look up Club Name
            String rawName = adminClubMap.get(user.getUid());
            if (rawName == null) {
                rawName = "No Club Assigned";
            }
            String finalClubName = rawName; // Final for Lambda

            holder.text1.setText(finalClubName);
            holder.text1.setTextSize(18);

            // Fix: Use Dynamic Color for Dark Mode support
            holder.text1.setTextColor(ContextCompat.getColor(context, R.color.purple_500));

            holder.text2.setText("Admin: " + user.getName() + "\n" + user.getEmail() + " (Tap to Remove)");
            // Subtext usually handles its own color in simple_list_item_2, or we can force it:
            holder.text2.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));

            holder.itemView.setOnClickListener(v -> showRemoveAdminDialog(user, finalClubName));
        }

        @Override public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView text1, text2;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                text1 = itemView.findViewById(android.R.id.text1);
                text2 = itemView.findViewById(android.R.id.text2);
            }
        }
    }

    // --- ADAPTER 2: STUDENTS ---
    class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.ViewHolder> {
        ArrayList<User> list;
        public StudentAdapter(ArrayList<User> list) { this.list = list; }

        @NonNull @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            User user = list.get(position);
            Context context = holder.itemView.getContext(); // Context defined here

            holder.text1.setText(user.getName());

            // FIX: Use Dynamic Color instead of Color.BLACK
            holder.text1.setTextColor(ContextCompat.getColor(context, R.color.text_primary));

            holder.text2.setText(user.getEmail() + " (Tap to Remove)");
            holder.text2.setTextColor(ContextCompat.getColor(context, R.color.text_secondary));

            holder.itemView.setOnClickListener(v -> showRemoveStudentDialog(user));
        }

        @Override public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView text1, text2;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                text1 = itemView.findViewById(android.R.id.text1);
                text2 = itemView.findViewById(android.R.id.text2);
            }
        }
    }

    // --- DELETE LOGIC ---

    private void showRemoveAdminDialog(User user, String clubName) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Club & Admin")
                .setMessage("Deleting " + user.getName() + " will also delete '" + clubName + "'. Continue?")
                .setPositiveButton("Delete All", (dialog, which) -> deleteClubAdmin(user))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showRemoveStudentDialog(User user) {
        new AlertDialog.Builder(this)
                .setTitle("Remove Student")
                .setMessage("Are you sure you want to remove " + user.getName() + "?")
                .setPositiveButton("Remove", (dialog, which) -> deleteStudent(user))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteClubAdmin(User user) {
        db.collection("users").document(user.getUid()).delete();
        db.collection("clubs").whereEqualTo("adminId", user.getUid()).get()
                .addOnSuccessListener(snapshots -> {
                    for (DocumentSnapshot doc : snapshots) {
                        doc.getReference().delete();
                    }
                    Toast.makeText(this, "Club & Admin Deleted", Toast.LENGTH_SHORT).show();
                    loadDashboardData();
                });
    }

    private void deleteStudent(User user) {
        db.collection("users").document(user.getUid()).delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Student Removed", Toast.LENGTH_SHORT).show();
                    loadDashboardData();
                });
    }
}