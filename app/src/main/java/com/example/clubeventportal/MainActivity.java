package com.example.clubeventportal;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerEvents, recyclerClubs;
    private EventAdapter eventAdapter;
    private ClubListAdapter clubAdapter;

    private ArrayList<ClubEvent> eventList;
    private ArrayList<Club> clubList;

    private FirebaseFirestore db;
    private FloatingActionButton fabAdd;
    private FirebaseAuth auth;
    private String currentUserRole = "Student";
    private String myClubId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Initialize Notification Channels (WorkManager/FCM)
        NotificationUtils.createNotificationChannels(this);

        // 2. Request Notification Permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        recyclerEvents = findViewById(R.id.recyclerView); // For Admins (Events)
        recyclerClubs = findViewById(R.id.recyclerClubs); // For Students (Directory)
        fabAdd = findViewById(R.id.fabAdd);

        recyclerEvents.setLayoutManager(new LinearLayoutManager(this));
        recyclerClubs.setLayoutManager(new LinearLayoutManager(this));

        eventList = new ArrayList<>();
        clubList = new ArrayList<>();

        checkUserRoleAndSetup();

        fabAdd.setOnClickListener(v -> startActivity(new Intent(this, AddEventActivity.class)));
    }

    private void checkUserRoleAndSetup() {
        if (auth.getCurrentUser() == null) return;
        String uid = auth.getUid();

        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    User user = documentSnapshot.toObject(User.class);
                    if (user != null && user.getRole() != null) {
                        currentUserRole = user.getRole();

                        // Refresh menu to show/hide "Edit Profile" based on role
                        invalidateOptionsMenu();

                        if (currentUserRole.equalsIgnoreCase("Club Admin")) {
                            // === CLUB ADMIN VIEW ===
                            fabAdd.setVisibility(View.VISIBLE);
                            recyclerClubs.setVisibility(View.GONE);
                            recyclerEvents.setVisibility(View.VISIBLE);

                            // Subscribe to Admin Notifications
                            FirebaseMessaging.getInstance().subscribeToTopic("admins");

                            fetchMyClubIdAndEvents(uid);

                        } else if (currentUserRole.equalsIgnoreCase("Student")) {
                            // === STUDENT VIEW ===
                            fabAdd.setVisibility(View.GONE);
                            recyclerEvents.setVisibility(View.GONE);
                            recyclerClubs.setVisibility(View.VISIBLE);

                            // Subscribe to Student Notifications
                            FirebaseMessaging.getInstance().subscribeToTopic("students");

                            getSupportActionBar().setTitle("Clubs Directory");
                            fetchClubList();
                        }
                    }
                });
    }

    // --- STUDENT LOGIC: List of Clubs ---
    private void fetchClubList() {
        clubAdapter = new ClubListAdapter(this, clubList);
        recyclerClubs.setAdapter(clubAdapter);

        db.collection("clubs").get().addOnSuccessListener(snapshots -> {
            clubList.clear();
            for(DocumentSnapshot doc : snapshots) {
                clubList.add(doc.toObject(Club.class));
            }
            clubAdapter.notifyDataSetChanged();
        });
    }

    // --- ADMIN LOGIC: List of THEIR Events ---
    private void fetchMyClubIdAndEvents(String adminUid) {
        db.collection("clubs").whereEqualTo("adminId", adminUid).get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        myClubId = querySnapshot.getDocuments().get(0).getString("clubId");
                    }
                    // Setup Adapter with Role
                    eventAdapter = new EventAdapter(eventList, currentUserRole, auth.getUid());
                    recyclerEvents.setAdapter(eventAdapter);
                    fetchAdminEvents(myClubId);
                });
    }

    private void fetchAdminEvents(String filterByClubId) {
        if (filterByClubId == null) return;
        db.collection("events").whereEqualTo("clubId", filterByClubId)
                .addSnapshotListener((value, error) -> {
                    if (value == null) return;
                    eventList.clear();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        eventList.add(doc.toObject(ClubEvent.class));
                    }
                    eventAdapter.notifyDataSetChanged();
                });
    }

    // --- MENU HANDLING ---
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // Show "Edit Profile" only for Admins
        MenuItem editProfile = menu.findItem(R.id.action_edit_profile);
        if (editProfile != null) {
            editProfile.setVisible(currentUserRole.equalsIgnoreCase("Club Admin"));
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            // Unsubscribe before logout to stop getting notifications
            FirebaseMessaging.getInstance().unsubscribeFromTopic("students");
            FirebaseMessaging.getInstance().unsubscribeFromTopic("admins");

            auth.signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return true;
        } else if (item.getItemId() == R.id.action_edit_profile) {
            startActivity(new Intent(this, EditClubProfileActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}