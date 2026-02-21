package com.example.clubeventportal;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button; // <--- ADDED IMPORT
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat; // Added for theme colors
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class AdminAttendanceActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private RegAdapter adapter;
    private ArrayList<Registration> list;
    private FirebaseFirestore db;

    // Data
    private String eventId, eventDateString, eventTimeString;
    private int durationMins;

    // UI
    private TextView tvCount, tvStatus;
    private ExtendedFloatingActionButton fabScan;

    // Logic Flags
    private boolean isEventActive = false;
    private boolean isEventCompleted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_attendance);

        Intent intent = getIntent();
        eventId = intent.getStringExtra("eventId");
        eventDateString = intent.getStringExtra("eventDate");
        eventTimeString = intent.getStringExtra("eventTime");
        durationMins = intent.getIntExtra("duration", 60);

        if (eventId == null) {
            Toast.makeText(this, "Error: No Event ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvCount = findViewById(R.id.tvCount);
        tvStatus = findViewById(R.id.tvStatus);
        fabScan = findViewById(R.id.fabScan);
        recyclerView = findViewById(R.id.recyclerRegistrations);
        Button btnExport = findViewById(R.id.btnExport); // <--- FIXED: Linked Button

        db = FirebaseFirestore.getInstance();
        list = new ArrayList<>();
        adapter = new RegAdapter(list);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        checkTimeAndLockScanner();
        fetchRegistrations();

        // --- SCAN LOGIC ---
        fabScan.setOnClickListener(v -> {
            if (isEventActive) {
                IntentIntegrator integrator = new IntentIntegrator(this);
                integrator.setOrientationLocked(true);
                integrator.setPrompt("Scan Student QR");
                integrator.setBeepEnabled(true);
                integrator.initiateScan();
            } else if (isEventCompleted) {
                Toast.makeText(this, "Event Completed. Attendance Closed.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Event hasn't started yet!", Toast.LENGTH_SHORT).show();
            }
        });

        // --- EXPORT LOGIC (FIXED) ---
        btnExport.setOnClickListener(v -> {
            if (list.isEmpty()) {
                Toast.makeText(this, "No data to export", Toast.LENGTH_SHORT).show();
                return;
            }
            StringBuilder csv = new StringBuilder();
            csv.append("Name,Register No,Department,Phone,Email,Status\n");

            for(Registration r : list) {
                String status = r.isAttended() ? "Present" : "Absent";
                csv.append(r.getName()).append(",")
                        .append(r.getRegNo()).append(",")
                        .append(r.getDept()).append(",")
                        .append(r.getPhone()).append(",")
                        .append(r.getEmail()).append(",")
                        .append(status).append("\n");
            }

            // Call the Utility class created earlier
            CsvExportUtils.exportData(this, "Attendance_" + eventDateString, csv.toString());
        });
    }

    private void checkTimeAndLockScanner() {
        if (eventDateString == null || eventTimeString == null) {
            isEventActive = true;
            tvStatus.setText("Status: Open (Date info missing)");
            return;
        }

        String fullDateTime = eventDateString + " " + eventTimeString;
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy hh:mm a", Locale.getDefault());

        try {
            Date start = sdf.parse(fullDateTime);
            if (start != null) {
                long startTime = start.getTime();
                long endTime = startTime + (durationMins * 60 * 1000L);
                long now = System.currentTimeMillis();

                if (now < startTime) {
                    isEventActive = false;
                    isEventCompleted = false;
                    fabScan.setBackgroundColor(Color.GRAY);
                    fabScan.setText("Locked 🔒");
                    tvStatus.setText("Status: Upcoming (Starts " + eventTimeString + ")");
                    tvStatus.setTextColor(Color.GRAY);
                } else if (now >= startTime && now <= endTime) {
                    isEventActive = true;
                    isEventCompleted = false;
                    fabScan.setBackgroundColor(Color.parseColor("#03DAC5"));
                    fabScan.setText("Scan QR 📷");
                    tvStatus.setText("Status: Attendance Open 🟢");
                    tvStatus.setTextColor(Color.parseColor("#4CAF50"));
                } else {
                    isEventActive = false;
                    isEventCompleted = true;
                    fabScan.setBackgroundColor(Color.RED);
                    fabScan.setText("Completed 🏁");
                    tvStatus.setText("Status: Time Limit Reached 🔴");
                    tvStatus.setTextColor(Color.RED);
                }
            }
        } catch (Exception e) {
            isEventActive = true;
            tvStatus.setText("Status: Open (Date Check Skipped)");
        }
    }

    private void fetchRegistrations() {
        db.collection("events").document(eventId).collection("registrations")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        tvCount.setText("Error loading data");
                        return;
                    }
                    if (value != null) {
                        list.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            list.add(doc.toObject(Registration.class));
                        }
                        tvCount.setText("Total Registered: " + list.size());
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() != null) {
                markAttendance(result.getContents());
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void markAttendance(String qrContent) {
        String[] parts = qrContent.split("\\|");
        if (parts.length != 2) {
            Toast.makeText(this, "Invalid QR Format", Toast.LENGTH_SHORT).show();
            return;
        }

        String scannedEventId = parts[0];
        String scannedUserId = parts[1];

        if (!scannedEventId.equals(eventId)) {
            Toast.makeText(this, "Wrong Event Ticket!", Toast.LENGTH_LONG).show();
            return;
        }

        db.collection("events").document(eventId)
                .collection("registrations").document(scannedUserId)
                .update("attended", true)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Attendance Marked ✅", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Student Not Registered!", Toast.LENGTH_SHORT).show());
    }

    class RegAdapter extends RecyclerView.Adapter<RegAdapter.ViewHolder> {
        ArrayList<Registration> list;
        public RegAdapter(ArrayList<Registration> list) { this.list = list; }

        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ViewHolder(view);
        }

        @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Registration r = list.get(position);
            holder.text1.setText(r.getName() + " (" + r.getRegNo() + ")");
            // Fix: Use Dynamic Colors via ContextCompat
            holder.text1.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.text_primary));

            if (r.isAttended()) {
                holder.text2.setText("PRESENT ✅");
                holder.text2.setTextColor(Color.parseColor("#4CAF50")); // Green
            } else {
                holder.text2.setText("NOT PRESENT ❌");
                holder.text2.setTextColor(Color.RED);
            }
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
}