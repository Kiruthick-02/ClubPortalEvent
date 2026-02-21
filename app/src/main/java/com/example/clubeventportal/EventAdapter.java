package com.example.clubeventportal;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText; // <--- THIS WAS MISSING
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.ViewHolder> {

    private final ArrayList<ClubEvent> list;
    private final FirebaseFirestore db;
    private final String userRole;
    private final String currentUserId;
    private final Set<String> viewedEventsSession = new HashSet<>();

    public EventAdapter(ArrayList<ClubEvent> list, String userRole, String currentUserId) {
        this.list = list;
        this.userRole = userRole;
        this.currentUserId = currentUserId;
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ClubEvent event = list.get(position);
        Context context = holder.itemView.getContext();

        // --- 1. DATA BINDING ---
        holder.title.setText(event.getTitle());
        holder.club.setText(event.getClubName());
        holder.date.setText(event.getDate());
        holder.txtVenueTime.setText(event.getVenue() + " • " + event.getTime());

        String feeText = (event.getEntryFee() == 0) ? "Free" : "$" + event.getEntryFee();
        String limitText = (event.getRegistrationLimit() == 0) ? "No Limit" : String.valueOf(event.getRegistrationLimit());
        holder.txtFeeLimit.setText("Entry: " + feeText + " • Limit: " + limitText);

        holder.tvViews.setText(event.getViews() + " Viewed");
        holder.tvInterested.setText(event.getInterested() + " Interested");
        holder.tvNotInterested.setText(event.getNotInterested() + " Not Interested");

        loadImage(holder, event.getPosterUrl());

        // --- 2. ROLE BASED VISIBILITY ---

        if (userRole != null && userRole.toLowerCase().contains("admin") && !userRole.toLowerCase().contains("super")) {
            // === CLUB ADMIN VIEW ===
            holder.adminLayout.setVisibility(View.VISIBLE);
            holder.studentLayout.setVisibility(View.GONE);

            // Button: Budget
            holder.btnBudget.setOnClickListener(v -> {
                Intent intent = new Intent(context, BudgetManagementActivity.class);
                intent.putExtra("eventId", event.getEventId());
                intent.putExtra("budget", event.getBudget());
                context.startActivity(intent);
            });

            // Button: Tasks
            holder.btnTasks.setOnClickListener(v -> {
                Intent intent = new Intent(context, TaskManagerActivity.class);
                intent.putExtra("eventId", event.getEventId());
                context.startActivity(intent);
            });

            // Button: Gallery (Admin)
            holder.btnGallery.setOnClickListener(v -> {
                Intent intent = new Intent(context, GalleryActivity.class);
                intent.putExtra("eventId", event.getEventId());
                intent.putExtra("isAdmin", true);
                context.startActivity(intent);
            });

            // Button: Attendance
            holder.btnManageAttendance.setOnClickListener(v -> {
                Intent intent = new Intent(context, AdminAttendanceActivity.class);
                intent.putExtra("eventId", event.getEventId());
                intent.putExtra("eventDate", event.getDate());
                intent.putExtra("eventTime", event.getTime());
                intent.putExtra("duration", event.getAttendanceDuration());
                context.startActivity(intent);
            });

            // Button: Edit
            holder.btnEdit.setOnClickListener(v -> {
                Intent intent = new Intent(context, AddEventActivity.class);
                intent.putExtra("eventId", event.getEventId());
                intent.putExtra("title", event.getTitle());
                intent.putExtra("desc", event.getDescription());
                intent.putExtra("club", event.getClubName());
                intent.putExtra("budget", event.getBudget());
                intent.putExtra("image", event.getPosterUrl());
                intent.putExtra("venue", event.getVenue());
                intent.putExtra("date", event.getDate());
                intent.putExtra("time", event.getTime());
                intent.putExtra("limit", event.getRegistrationLimit());
                intent.putExtra("fee", event.getEntryFee());
                intent.putExtra("duration", event.getAttendanceDuration());
                context.startActivity(intent);
            });

            // Button: Delete
            holder.btnDelete.setOnClickListener(v -> showDeleteDialog(context, event.getEventId()));

        } else if (userRole != null && userRole.equalsIgnoreCase("Student")) {
            // === STUDENT VIEW ===
            holder.adminLayout.setVisibility(View.GONE);
            holder.studentLayout.setVisibility(View.VISIBLE);

            checkRegistrationStatus(holder.btnRegister, event.getEventId(), context);

            // Register
            holder.btnRegister.setOnClickListener(v -> {
                if (holder.btnRegister.getText().toString().contains("Ticket")) {
                    Intent intent = new Intent(context, TicketActivity.class);
                    intent.putExtra("eventId", event.getEventId());
                    intent.putExtra("userId", currentUserId);
                    context.startActivity(intent);
                } else {
                    showRegistrationDialog(context, event);
                }
            });

            // Gallery (Student)
            holder.btnGalleryStudent.setOnClickListener(v -> {
                Intent intent = new Intent(context, GalleryActivity.class);
                intent.putExtra("eventId", event.getEventId());
                intent.putExtra("isAdmin", false);
                context.startActivity(intent);
            });

            // Feedback
            holder.btnFeedback.setOnClickListener(v -> {
                Intent intent = new Intent(context, FeedbackActivity.class);
                intent.putExtra("eventId", event.getEventId());
                context.startActivity(intent);
            });

            if (!viewedEventsSession.contains(event.getEventId())) {
                viewedEventsSession.add(event.getEventId());
                incrementViewCount(event.getEventId());
            }
        } else {
            // === SUPER ADMIN ===
            holder.adminLayout.setVisibility(View.GONE);
            holder.studentLayout.setVisibility(View.GONE);
        }

        // Polls
        holder.btnInterested.setOnClickListener(v -> handleVoteChange(event.getEventId(), "interested"));
        holder.btnNotInterested.setOnClickListener(v -> handleVoteChange(event.getEventId(), "notInterested"));
    }

    private void checkRegistrationStatus(Button btn, String eventId, Context context) {
        db.collection("events").document(eventId).collection("registrations").document(currentUserId)
                .get().addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        btn.setText("View Ticket 🎟️");
                        btn.setBackgroundColor(Color.parseColor("#FF9800"));
                    } else {
                        btn.setText("Register Now");
                        btn.setBackgroundColor(context.getResources().getColor(R.color.teal_200));
                    }
                });
    }

    private void showRegistrationDialog(Context context, ClubEvent event) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_register, null);
        builder.setView(view);

        // This works now because EditText is imported
        EditText etName = view.findViewById(R.id.etRegName);
        EditText etRegNo = view.findViewById(R.id.etRegNo);
        EditText etDept = view.findViewById(R.id.etRegDept);
        EditText etPhone = view.findViewById(R.id.etRegPhone);
        Button btnSubmit = view.findViewById(R.id.btnRegSubmit);

        AlertDialog dialog = builder.create();

        btnSubmit.setOnClickListener(v -> {
            if (etName.getText().toString().isEmpty() || etRegNo.getText().toString().isEmpty()) {
                Toast.makeText(context, "Fill all details", Toast.LENGTH_SHORT).show();
                return;
            }
            String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();
            Registration reg = new Registration(currentUserId, etName.getText().toString(),
                    etRegNo.getText().toString(), etDept.getText().toString(),
                    etPhone.getText().toString(), email);

            db.collection("events").document(event.getEventId())
                    .collection("registrations").document(currentUserId)
                    .set(reg)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(context, "Registered!", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();

                        // Schedule Notification
                        NotificationUtils.scheduleEventReminder(context, event.getTitle(), event.getDate(), event.getTime());

                        Intent intent = new Intent(context, TicketActivity.class);
                        intent.putExtra("eventId", event.getEventId());
                        intent.putExtra("userId", currentUserId);
                        context.startActivity(intent);
                        notifyDataSetChanged();
                    });
        });
        dialog.show();
    }

    private void handleVoteChange(String eventId, String newVoteType) {
        DocumentReference voterRef = db.collection("events").document(eventId).collection("voters").document(currentUserId);
        DocumentReference eventRef = db.collection("events").document(eventId);

        db.runTransaction(transaction -> {
            DocumentSnapshot voterSnapshot = transaction.get(voterRef);
            if (!voterSnapshot.exists()) {
                transaction.update(eventRef, newVoteType, FieldValue.increment(1));
                Map<String, Object> voteData = new HashMap<>();
                voteData.put("timestamp", System.currentTimeMillis());
                voteData.put("vote", newVoteType);
                transaction.set(voterRef, voteData);
                return "Vote Recorded";
            } else {
                String oldVoteType = voterSnapshot.getString("vote");
                if (oldVoteType != null && oldVoteType.equals(newVoteType)) return "Already selected";
                transaction.update(eventRef, oldVoteType, FieldValue.increment(-1));
                transaction.update(eventRef, newVoteType, FieldValue.increment(1));
                transaction.update(voterRef, "vote", newVoteType);
                return "Vote Changed";
            }
        }).addOnSuccessListener(result -> {
            if (result != null && !result.equals("Already selected"))
                Toast.makeText(db.getApp().getApplicationContext(), result, Toast.LENGTH_SHORT).show();
        });
    }

    private void showDeleteDialog(Context context, String eventId) {
        new AlertDialog.Builder(context)
                .setTitle("Delete Event")
                .setMessage("Are you sure? This cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    db.collection("events").document(eventId).delete()
                            .addOnSuccessListener(aVoid -> Toast.makeText(context, "Event Deleted", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null).show();
    }

    private void incrementViewCount(String eventId) {
        db.collection("events").document(eventId).update("views", FieldValue.increment(1));
    }

    private void loadImage(ViewHolder holder, String base64String) {
        if (base64String != null && !base64String.isEmpty()) {
            try {
                byte[] imageBytes = Base64.decode(base64String, Base64.DEFAULT);
                Glide.with(holder.itemView.getContext()).load(imageBytes).centerCrop().into(holder.image);
                holder.image.setOnClickListener(v -> {
                    Context context = holder.itemView.getContext();
                    Intent intent = new Intent(context, FullImageActivity.class);
                    intent.putExtra("image", base64String);
                    context.startActivity(intent);
                });
            } catch (Exception e) {
                holder.image.setImageResource(android.R.drawable.ic_menu_report_image);
            }
        }
    }

    @Override public int getItemCount() { return list.size(); }

    // --- UPDATED VIEW HOLDER ---
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, club, date, tvViews, tvInterested, tvNotInterested;
        TextView txtVenueTime, txtFeeLimit;

        Button btnInterested, btnNotInterested;
        Button btnRegister, btnManageAttendance;
        Button btnGalleryStudent, btnFeedback; // Student Buttons

        ImageButton btnEdit, btnDelete, btnBudget;
        ImageButton btnTasks, btnGallery; // Admin Buttons

        ImageView image;
        LinearLayout studentLayout, adminLayout;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.txtTitle);
            club = itemView.findViewById(R.id.txtClubName);
            date = itemView.findViewById(R.id.txtDate);
            image = itemView.findViewById(R.id.imgPoster);
            txtVenueTime = itemView.findViewById(R.id.txtVenueTime);
            txtFeeLimit = itemView.findViewById(R.id.txtFeeLimit);

            tvViews = itemView.findViewById(R.id.tvViews);
            tvInterested = itemView.findViewById(R.id.tvInterestedCount);
            tvNotInterested = itemView.findViewById(R.id.tvNotInterestedCount);

            studentLayout = itemView.findViewById(R.id.layoutStudentControls);
            adminLayout = itemView.findViewById(R.id.layoutAdminControls);

            // Polls
            btnInterested = itemView.findViewById(R.id.btnInterested);
            btnNotInterested = itemView.findViewById(R.id.btnNotInterested);

            // Student Buttons
            btnRegister = itemView.findViewById(R.id.btnRegister);
            btnGalleryStudent = itemView.findViewById(R.id.btnGalleryStudent);
            btnFeedback = itemView.findViewById(R.id.btnFeedback);

            // Admin Buttons
            btnManageAttendance = itemView.findViewById(R.id.btnManageAttendance);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            btnBudget = itemView.findViewById(R.id.btnBudget);
            btnTasks = itemView.findViewById(R.id.btnTasks);
            btnGallery = itemView.findViewById(R.id.btnGallery);
        }
    }
}