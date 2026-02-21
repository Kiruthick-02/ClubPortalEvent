package com.example.clubeventportal;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.UUID;

public class TaskManagerActivity extends AppCompatActivity {

    private String eventId;
    private EditText etTask;
    private Button btnAdd;
    private RecyclerView recyclerView;
    private TaskAdapter adapter;
    private ArrayList<Task> taskList;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_manager);

        eventId = getIntent().getStringExtra("eventId");
        db = FirebaseFirestore.getInstance();

        etTask = findViewById(R.id.etTaskDesc);
        btnAdd = findViewById(R.id.btnAddTask);
        recyclerView = findViewById(R.id.recyclerTasks);

        taskList = new ArrayList<>();
        adapter = new TaskAdapter(taskList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        btnAdd.setOnClickListener(v -> {
            String desc = etTask.getText().toString();
            if(!desc.isEmpty()){
                String id = UUID.randomUUID().toString();
                Task task = new Task(id, desc, false);
                db.collection("events").document(eventId).collection("tasks").document(id).set(task);
                etTask.setText("");
            }
        });

        loadTasks();
    }

    private void loadTasks() {
        db.collection("events").document(eventId).collection("tasks").addSnapshotListener((value, error) -> {
            if(value == null) return;
            taskList.clear();
            for(DocumentSnapshot doc : value.getDocuments()){
                taskList.add(doc.toObject(Task.class));
            }
            adapter.notifyDataSetChanged();
        });
    }

    class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.ViewHolder> {
        ArrayList<Task> list;
        public TaskAdapter(ArrayList<Task> list) { this.list = list; }

        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task, parent, false));
        }

        @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Task t = list.get(position);
            holder.cbTask.setText(t.getDescription());
            holder.cbTask.setChecked(t.isCompleted());
            holder.cbTask.setOnCheckedChangeListener((buttonView, isChecked) -> {
                db.collection("events").document(eventId).collection("tasks").document(t.getId()).update("completed", isChecked);
            });
        }

        @Override public int getItemCount() { return list.size(); }
        class ViewHolder extends RecyclerView.ViewHolder { CheckBox cbTask; public ViewHolder(View v) { super(v); cbTask = v.findViewById(R.id.cbTask); } }
    }
}