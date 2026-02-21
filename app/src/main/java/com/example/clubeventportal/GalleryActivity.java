package com.example.clubeventportal;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class GalleryActivity extends AppCompatActivity {

    private String eventId;
    private boolean isAdmin;
    private GridView gridView;
    private ArrayList<String> images;
    private GalleryAdapter adapter;
    private FirebaseFirestore db;

    private final ActivityResultLauncher<String> selectImage = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) uploadImage(uri);
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        eventId = getIntent().getStringExtra("eventId");
        isAdmin = getIntent().getBooleanExtra("isAdmin", false);
        db = FirebaseFirestore.getInstance();

        gridView = findViewById(R.id.gridGallery);
        FloatingActionButton fab = findViewById(R.id.fabAddPhoto);

        if (!isAdmin) fab.setVisibility(View.GONE);
        fab.setOnClickListener(v -> selectImage.launch("image/*"));

        images = new ArrayList<>();
        adapter = new GalleryAdapter();
        gridView.setAdapter(adapter);

        loadImages();
    }

    private void uploadImage(Uri uri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            Bitmap resized = Bitmap.createScaledBitmap(bitmap, 600, 600, true);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            resized.compress(Bitmap.CompressFormat.JPEG, 60, baos);
            String base64 = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);

            Map<String, Object> map = new HashMap<>();
            map.put("image", base64);
            db.collection("events").document(eventId).collection("gallery").add(map);
            Toast.makeText(this, "Photo Uploaded", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {}
    }

    private void loadImages() {
        db.collection("events").document(eventId).collection("gallery").addSnapshotListener((v, e) -> {
            if (v == null) return;
            images.clear();
            for (DocumentSnapshot doc : v.getDocuments()) {
                images.add(doc.getString("image"));
            }
            adapter.notifyDataSetChanged();
        });
    }

    class GalleryAdapter extends BaseAdapter {
        @Override public int getCount() { return images.size(); }
        @Override public Object getItem(int i) { return images.get(i); }
        @Override public long getItemId(int i) { return 0; }
        @Override public View getView(int i, View view, ViewGroup viewGroup) {
            ImageView iv;
            if (view == null) {
                iv = new ImageView(GalleryActivity.this);
                iv.setLayoutParams(new GridView.LayoutParams(300, 300));
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            } else iv = (ImageView) view;

            try {
                byte[] decodedString = Base64.decode(images.get(i), Base64.DEFAULT);
                Glide.with(GalleryActivity.this).load(decodedString).into(iv);
            } catch (Exception e) {}
            return iv;
        }
    }
}