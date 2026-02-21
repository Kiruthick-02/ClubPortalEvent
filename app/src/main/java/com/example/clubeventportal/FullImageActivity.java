package com.example.clubeventportal;

import android.os.Bundle;
import android.util.Base64;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.github.chrisbanes.photoview.PhotoView;

public class FullImageActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_image);

        PhotoView photoView = findViewById(R.id.photo_view);
        ImageButton btnClose = findViewById(R.id.btnClose);

        // Get Base64 string passed from Adapter
        String imageString = getIntent().getStringExtra("image");

        if (imageString != null) {
            try {
                byte[] imageBytes = Base64.decode(imageString, Base64.DEFAULT);
                Glide.with(this).load(imageBytes).into(photoView);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        btnClose.setOnClickListener(v -> finish());
    }
}