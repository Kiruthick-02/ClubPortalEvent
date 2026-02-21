package com.example.clubeventportal;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;

public class ClubEventsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private EventAdapter adapter;
    private ArrayList<ClubEvent> eventList;
    private FirebaseFirestore db;
    private String clubId, clubName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_club_events);

        clubId = getIntent().getStringExtra("clubId");
        clubName = getIntent().getStringExtra("clubName");

        TextView tvHeader = findViewById(R.id.tvClubHeader);
        tvHeader.setText(clubName + " Events");

        db = FirebaseFirestore.getInstance();
        recyclerView = findViewById(R.id.recyclerClubEvents);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        eventList = new ArrayList<>();

        // Student Role passed to adapter
        adapter = new EventAdapter(eventList, "Student", FirebaseAuth.getInstance().getUid());
        recyclerView.setAdapter(adapter);

        fetchEvents();
    }

    private void fetchEvents() {
        db.collection("events")
                .whereEqualTo("clubId", clubId)
                // .orderBy("timestamp", Query.Direction.DESCENDING) // Needs Index
                .addSnapshotListener((value, error) -> {
                    if (value == null) return;
                    eventList.clear();
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        eventList.add(doc.toObject(ClubEvent.class));
                    }
                    adapter.notifyDataSetChanged();
                });
    }
}