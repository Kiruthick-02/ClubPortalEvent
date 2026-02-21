package com.example.clubeventportal;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class FeedbackActivity extends AppCompatActivity {

    private EditText etFeedback;
    private TextView tvResult;
    private Button btnSubmit;
    private String eventId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);

        eventId = getIntent().getStringExtra("eventId");
        etFeedback = findViewById(R.id.etFeedback);
        tvResult = findViewById(R.id.tvSentimentResult);
        btnSubmit = findViewById(R.id.btnSubmitFeedback);

        btnSubmit.setOnClickListener(v -> analyzeAndSubmit());
    }

    private void analyzeAndSubmit() {
        String text = etFeedback.getText().toString().trim();
        if (text.isEmpty()) return;

        // 1. AI Analysis
        String sentiment = SentimentAnalyzer.analyze(text);

        // UI Feedback
        String emoji = sentiment.equals("POSITIVE") ? "😊" : (sentiment.equals("NEGATIVE") ? "😞" : "😐");
        tvResult.setText("AI Analysis: " + sentiment + " " + emoji);
        tvResult.setVisibility(View.VISIBLE);

        // 2. Save to Firestore
        Map<String, Object> feedback = new HashMap<>();
        feedback.put("userId", FirebaseAuth.getInstance().getUid());
        feedback.put("text", text);
        feedback.put("sentiment", sentiment);
        feedback.put("timestamp", System.currentTimeMillis());

        FirebaseFirestore.getInstance().collection("events").document(eventId)
                .collection("feedback").add(feedback)
                .addOnSuccessListener(a -> Toast.makeText(this, "Feedback Sent!", Toast.LENGTH_SHORT).show());
    }
}