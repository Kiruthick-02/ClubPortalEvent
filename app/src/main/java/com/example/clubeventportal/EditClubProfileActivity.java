package com.example.clubeventportal;

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
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

public class EditClubProfileActivity extends AppCompatActivity {

    private ImageView imgProfile;
    private EditText etName, etDesc;
    private Button btnSave;
    private Uri imageUri;
    private String clubId;
    private String existingImageBase64;

    private final ActivityResultLauncher<String> selectImage = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    imageUri = uri;
                    imgProfile.setImageURI(uri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_club_profile);

        imgProfile = findViewById(R.id.imgClubProfileEdit);
        etName = findViewById(R.id.etClubNameEdit);
        etDesc = findViewById(R.id.etClubDescEdit);
        btnSave = findViewById(R.id.btnSaveProfile);

        imgProfile.setOnClickListener(v -> selectImage.launch("image/*"));
        btnSave.setOnClickListener(v -> saveProfile());

        loadCurrentDetails();
    }

    private void loadCurrentDetails() {
        String uid = FirebaseAuth.getInstance().getUid();
        FirebaseFirestore.getInstance().collection("clubs")
                .whereEqualTo("adminId", uid).get()
                .addOnSuccessListener(snapshots -> {
                    if (!snapshots.isEmpty()) {
                        Club club = snapshots.getDocuments().get(0).toObject(Club.class);
                        if (club != null) {
                            clubId = club.getClubId();
                            etName.setText(club.getClubName());
                            etDesc.setText(club.getDescription());
                            existingImageBase64 = club.getProfileImage();

                            if (existingImageBase64 != null) {
                                byte[] bytes = Base64.decode(existingImageBase64, Base64.DEFAULT);
                                Glide.with(this).load(bytes).centerCrop().into(imgProfile);
                            }
                        }
                    }
                });
    }

    private void saveProfile() {
        if (clubId == null) return;

        btnSave.setEnabled(false);
        String finalImage = existingImageBase64;

        if (imageUri != null) {
            finalImage = encodeImage(imageUri);
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("clubName", etName.getText().toString());
        updates.put("description", etDesc.getText().toString());
        updates.put("profileImage", finalImage);

        FirebaseFirestore.getInstance().collection("clubs").document(clubId)
                .update(updates)
                .addOnSuccessListener(a -> {
                    Toast.makeText(this, "Profile Updated", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private String encodeImage(Uri uri) {
        try {
            Bitmap bitmap;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(getContentResolver(), uri));
            } else {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            }
            Bitmap resized = Bitmap.createScaledBitmap(bitmap, 400, 400, true);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            resized.compress(Bitmap.CompressFormat.JPEG, 60, baos);
            return Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
        } catch (Exception e) { return null; }
    }
}