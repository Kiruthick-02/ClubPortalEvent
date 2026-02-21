package com.example.clubeventportal;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {

    private ArrayList<Transaction> list;
    private Context context;

    public TransactionAdapter(ArrayList<Transaction> list, Context context) {
        this.list = list;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Transaction t = list.get(position);

        holder.tvCategory.setText(t.getCategory());
        holder.tvStatus.setText(t.getStatus());

        String date = new SimpleDateFormat("dd MMM", Locale.getDefault()).format(new Date(t.getTimestamp()));
        holder.tvDate.setText(date);

        if ("INCOME".equals(t.getType())) {
            holder.tvAmount.setText("+ $" + t.getAmount());
            holder.tvAmount.setTextColor(Color.parseColor("#4CAF50")); // Green
        } else {
            holder.tvAmount.setText("- $" + t.getAmount());
            holder.tvAmount.setTextColor(Color.RED);
        }

        // Long click to Delete or Change Status (Admin Power)
        holder.itemView.setOnLongClickListener(v -> {
            showOptionsDialog(t);
            return true;
        });
    }

    private void showOptionsDialog(Transaction t) {
        // Find Event ID from context (Workaround for cleaner code, usually passed in)
        if (context instanceof BudgetManagementActivity) {
            String eventId = ((BudgetManagementActivity) context).getIntent().getStringExtra("eventId");

            new AlertDialog.Builder(context)
                    .setTitle("Transaction Action")
                    .setItems(new String[]{"Delete", "Toggle Approval"}, (dialog, which) -> {
                        if (which == 0) {
                            FirebaseFirestore.getInstance().collection("events").document(eventId)
                                    .collection("transactions").document(t.getId()).delete();
                        } else {
                            String newStatus = t.getStatus().equals("APPROVED") ? "PENDING" : "APPROVED";
                            FirebaseFirestore.getInstance().collection("events").document(eventId)
                                    .collection("transactions").document(t.getId()).update("status", newStatus);
                        }
                    }).show();
        }
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategory, tvAmount, tvDate, tvStatus;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategory = itemView.findViewById(R.id.tvTransCategory);
            tvAmount = itemView.findViewById(R.id.tvTransAmount);
            tvDate = itemView.findViewById(R.id.tvTransDate);
            tvStatus = itemView.findViewById(R.id.tvTransStatus);
        }
    }
}