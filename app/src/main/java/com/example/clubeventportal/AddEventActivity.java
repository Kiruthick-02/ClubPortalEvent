package com.example.clubeventportal;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.yalantis.ucrop.UCrop;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class AddEventActivity extends AppCompatActivity {

    private ImageView imgPreview;
    private Uri finalImageUri; // The result after cropping

    // UI Fields
    private EditText etTitle, etDesc, etBudget, etVenue, etRegLimit, etEntryFee, etDuration;
    private TextView tvDate, tvTime;
    private Button btnSave;

    // Logic Variables
    private boolean isEditMode = false;
    private String eventIdToEdit = null;
    private String oldImageBase64 = null;

    private String myClubName = "";
    private String myClubId = "";

    // 1. Image Picker
    private final ActivityResultLauncher<String> selectImage = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    startCrop(uri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_event);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Create Event");
        }

        // Fetch Club Details (Only needed for new events, but good to have)
        fetchMyClubDetails();

        // Initialize Views
        imgPreview = findViewById(R.id.imgPreview);
        etTitle = findViewById(R.id.etEventTitle);
        etDesc = findViewById(R.id.etEventDesc);
        etBudget = findViewById(R.id.etEventBudget);

        etVenue = findViewById(R.id.etVenue);
        etRegLimit = findViewById(R.id.etRegLimit);
        etEntryFee = findViewById(R.id.etEntryFee);
        etDuration = findViewById(R.id.etDuration); // New Field

        tvDate = findViewById(R.id.tvDate);
        tvTime = findViewById(R.id.tvTime);
        btnSave = findViewById(R.id.btnSave);
        Button btnSelect = findViewById(R.id.btnSelectImage);

        // Listeners
        tvDate.setOnClickListener(v -> showDatePicker());
        tvTime.setOnClickListener(v -> showTimePicker());
        btnSelect.setOnClickListener(v -> selectImage.launch("image/*"));
        btnSave.setOnClickListener(v -> processAndUpload());

        // Check for Edit Mode
        if (getIntent().hasExtra("eventId")) {
            setupEditMode();
        }
    }

    // --- CROP LOGIC ---
    private void startCrop(Uri sourceUri) {
        String destFileName = "cropped_" + System.currentTimeMillis() + ".jpg";
        Uri destUri = Uri.fromFile(new File(getCacheDir(), destFileName));

        UCrop.Options options = new UCrop.Options();
        options.setToolbarColor(ContextCompat.getColor(this, R.color.purple_500));
        options.setStatusBarColor(ContextCompat.getColor(this, R.color.purple_700));
        options.setCompressionFormat(Bitmap.CompressFormat.JPEG);
        options.setCompressionQuality(90);

        // FIX: Allow user to move crop frame freely and start with full image
        options.setFreeStyleCropEnabled(true);

        UCrop.of(sourceUri, destUri)
                .useSourceImageAspectRatio() // <--- FIX: Uses original image ratio (No forced 4:3)
                .withMaxResultSize(1920, 1080) // High Res
                .withOptions(options)
                .start(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            finalImageUri = UCrop.getOutput(data);
            imgPreview.setImageURI(finalImageUri);
        } else if (resultCode == UCrop.RESULT_ERROR) {
            Toast.makeText(this, "Crop Failed", Toast.LENGTH_SHORT).show();
        }
    }

    // --- EDIT MODE SETUP ---
    private void setupEditMode() {
        isEditMode = true;
        eventIdToEdit = getIntent().getStringExtra("eventId");

        etTitle.setText(getIntent().getStringExtra("title"));
        etDesc.setText(getIntent().getStringExtra("desc"));
        etBudget.setText(String.valueOf(getIntent().getDoubleExtra("budget", 0)));

        etVenue.setText(getIntent().getStringExtra("venue"));
        tvDate.setText(getIntent().getStringExtra("date"));
        tvTime.setText(getIntent().getStringExtra("time"));

        etRegLimit.setText(String.valueOf(getIntent().getIntExtra("limit", 0)));
        etEntryFee.setText(String.valueOf(getIntent().getDoubleExtra("fee", 0)));
        etDuration.setText(String.valueOf(getIntent().getIntExtra("duration", 60))); // Default 60

        oldImageBase64 = getIntent().getStringExtra("image");
        btnSave.setText("Update Event");
        if(getSupportActionBar() != null) getSupportActionBar().setTitle("Edit Event");

        if(oldImageBase64 != null) {
            try {
                byte[] imageBytes = Base64.decode(oldImageBase64, Base64.DEFAULT);
                Glide.with(this).load(imageBytes).into(imgPreview);
            } catch (Exception e) {}
        }
    }

    // --- UPLOAD LOGIC ---
    private void processAndUpload() {
        if (!isEditMode && myClubId.isEmpty()) {
            Toast.makeText(this, "Loading club details... please wait", Toast.LENGTH_SHORT).show();
            return;
        }

        String base64Image = oldImageBase64;

        // If a new image was picked and cropped, process it
        if (finalImageUri != null) {
            base64Image = encodeImage(finalImageUri);
        }

        if (base64Image == null && oldImageBase64 == null) {
            Toast.makeText(this, "Poster Image is required", Toast.LENGTH_SHORT).show();
            return;
        }

        saveToFirestore(base64Image);
    }

    private String encodeImage(Uri uri) {
        try {
            Bitmap bitmap;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(getContentResolver(), uri));
            } else {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            }
            // Resize for Firestore Limit (approx 1MB max doc size)
            Bitmap resized = Bitmap.createScaledBitmap(bitmap, 800, 600, true);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            resized.compress(Bitmap.CompressFormat.JPEG, 50, baos);
            byte[] bytes = baos.toByteArray();
            return Base64.encodeToString(bytes, Base64.DEFAULT);
        } catch (IOException e) { return null; }
    }

    private void saveToFirestore(String base64Image) {
        String title = etTitle.getText().toString().trim();
        String desc = etDesc.getText().toString().trim();
        String venue = etVenue.getText().toString().trim();
        String date = tvDate.getText().toString();
        String time = tvTime.getText().toString();

        if (title.isEmpty() || date.equals("Select Date") || time.equals("Select Time")) {
            Toast.makeText(this, "Title, Date, and Time are required", Toast.LENGTH_SHORT).show();
            return;
        }

        // Parse Numbers Safely
        double budget = parseDoubleSafely(etBudget.getText().toString());
        double fee = parseDoubleSafely(etEntryFee.getText().toString());
        int limit = parseIntSafely(etRegLimit.getText().toString());
        int duration = parseIntSafely(etDuration.getText().toString());
        if(duration == 0) duration = 60; // Default

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (isEditMode) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("title", title); updates.put("description", desc);
            updates.put("venue", venue); updates.put("date", date);
            updates.put("time", time); updates.put("registrationLimit", limit);
            updates.put("entryFee", fee); updates.put("budget", budget);
            updates.put("attendanceDuration", duration); // Save Duration
            updates.put("posterUrl", base64Image);

            db.collection("events").document(eventIdToEdit).update(updates)
                    .addOnSuccessListener(a -> finish())
                    .addOnFailureListener(e -> Toast.makeText(this, "Update Failed", Toast.LENGTH_SHORT).show());
        } else {
            String id = UUID.randomUUID().toString();
            // Create Event with ALL fields
            ClubEvent event = new ClubEvent(id, title, desc, date, time, venue, limit, fee, duration, base64Image, myClubName, myClubId, budget);

            db.collection("events").document(id).set(event)
                    .addOnSuccessListener(a -> {
                        Toast.makeText(this, "Event Published", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    // --- HELPERS ---
    private void fetchMyClubDetails() {
        String uid = FirebaseAuth.getInstance().getUid();
        FirebaseFirestore.getInstance().collection("clubs")
                .whereEqualTo("adminId", uid).get()
                .addOnSuccessListener(snapshots -> {
                    if (!snapshots.isEmpty()) {
                        Club club = snapshots.getDocuments().get(0).toObject(Club.class);
                        if (club != null) {
                            myClubName = club.getClubName();
                            myClubId = club.getClubId();
                        }
                    }
                });
    }

    private void showDatePicker() {
        Calendar c = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(this, (v, y, m, d) -> {
            Calendar cal = Calendar.getInstance(); cal.set(y, m, d);
            tvDate.setText(new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(cal.getTime()));
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        dialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        dialog.show();
    }

    private void showTimePicker() {
        Calendar c = Calendar.getInstance();
        new TimePickerDialog(this, (v, h, m) -> {
            String amPm = (h >= 12) ? "PM" : "AM";
            int hr = (h > 12) ? h - 12 : h; if(hr==0) hr=12;
            tvTime.setText(String.format(Locale.getDefault(), "%02d:%02d %s", hr, m, amPm));
        }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false).show();
    }

    private double parseDoubleSafely(String s) { try { return Double.parseDouble(s); } catch(Exception e) { return 0.0; } }
    private int parseIntSafely(String s) { try { return Integer.parseInt(s); } catch(Exception e) { return 0; } }

    @Override public boolean onSupportNavigateUp() { finish(); return true; }
}