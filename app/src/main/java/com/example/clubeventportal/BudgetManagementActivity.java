package com.example.clubeventportal;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BudgetManagementActivity extends AppCompatActivity {

    private String eventId;
    private double allocatedBudget;

    private TextView tvBudget, tvIncome, tvExpense, tvBalance;
    private PieChart pieChart;
    private RecyclerView recyclerView;
    private TransactionAdapter adapter;
    private ArrayList<Transaction> transactionList;
    private FirebaseFirestore db;
    private FloatingActionButton fabAdd;

    // Scan Logic Variables
    private EditText etAmountRef; // Reference to the dialog's amount field
    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bundle extras = result.getData().getExtras();
                    Bitmap imageBitmap = (Bitmap) extras.get("data");
                    processBillImage(imageBitmap);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_budget_management);

        if (getIntent() != null) {
            eventId = getIntent().getStringExtra("eventId");
            allocatedBudget = getIntent().getDoubleExtra("budget", 0.0);
        }

        if (eventId == null) {
            Toast.makeText(this, "Error: Event ID missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();

        // UI Init
        tvBudget = findViewById(R.id.tvTotalBudget);
        tvIncome = findViewById(R.id.tvTotalIncome);
        tvExpense = findViewById(R.id.tvTotalExpense);
        tvBalance = findViewById(R.id.tvBalance);
        pieChart = findViewById(R.id.pieChart);
        fabAdd = findViewById(R.id.fabAddTransaction);

        tvBudget.setText("Allocated: $" + allocatedBudget);

        recyclerView = findViewById(R.id.recyclerTransactions);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        transactionList = new ArrayList<>();
        adapter = new TransactionAdapter(transactionList, this);
        recyclerView.setAdapter(adapter);

        fabAdd.setOnClickListener(v -> showAddTransactionDialog());

        fetchTransactions();
    }

    private void showAddTransactionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_transaction, null);
        builder.setView(view);

        Spinner spType = view.findViewById(R.id.spTransType);
        EditText etCategory = view.findViewById(R.id.etCategory);
        etAmountRef = view.findViewById(R.id.etAmount); // Save global reference
        ImageButton btnScan = view.findViewById(R.id.btnScanBill);
        Button btnSave = view.findViewById(R.id.btnSaveTrans);

        String[] types = {"EXPENSE", "INCOME"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, types);
        spType.setAdapter(adapter);

        // --- SCAN BILL LOGIC ---
        btnScan.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 100);
            } else {
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                cameraLauncher.launch(takePictureIntent);
            }
        });

        AlertDialog dialog = builder.create();

        btnSave.setOnClickListener(v -> {
            String type = spType.getSelectedItem().toString();
            String cat = etCategory.getText().toString();
            String amtStr = etAmountRef.getText().toString();

            if (cat.isEmpty() || amtStr.isEmpty()) {
                Toast.makeText(this, "Fill details or scan bill", Toast.LENGTH_SHORT).show();
                return;
            }

            double amount = Double.parseDouble(amtStr);
            String id = UUID.randomUUID().toString();
            String uid = FirebaseAuth.getInstance().getUid();

            Transaction t = new Transaction(id, type, cat, amount, "APPROVED", uid);

            db.collection("events").document(eventId).collection("transactions").document(id).set(t)
                    .addOnSuccessListener(a -> {
                        Toast.makeText(this, "Transaction Added", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    });
        });

        dialog.show();
    }

    // --- ML KIT LOGIC ---
    private void processBillImage(Bitmap bitmap) {
        if (bitmap == null) return;
        Toast.makeText(this, "Scanning Receipt...", Toast.LENGTH_SHORT).show();

        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        InputImage image = InputImage.fromBitmap(bitmap, 0);

        recognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    String rawText = visionText.getText();
                    double detectedAmount = extractTotalAmount(rawText);

                    if (detectedAmount > 0) {
                        etAmountRef.setText(String.valueOf(detectedAmount));
                        Toast.makeText(this, "Amount Detected: " + detectedAmount, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Could not detect total amount. Please enter manually.", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Scan Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // Smart logic to find the "Total" price in a messy receipt
    private double extractTotalAmount(String text) {
        // Regex to find prices (e.g., 10.00, 1,200.50)
        // Matches numbers with optional commas and required decimal points
        Pattern p = Pattern.compile("(\\d{1,3}(,\\d{3})*|\\d+)(\\.\\d{2})");
        Matcher m = p.matcher(text);

        List<Double> prices = new ArrayList<>();
        while (m.find()) {
            try {
                String val = m.group().replace(",", ""); // Remove commas
                prices.add(Double.parseDouble(val));
            } catch (Exception e) { /* Ignore parsing errors */ }
        }

        if (prices.isEmpty()) return 0.0;

        // HEURISTIC: In most receipts, the "Total" is the largest number found.
        return Collections.max(prices);
    }

    // --- EXISTING FETCH & CHART LOGIC (Unchanged) ---
    private void fetchTransactions() {
        db.collection("events").document(eventId).collection("transactions")
                .addSnapshotListener((value, error) -> {
                    if (value == null) return;
                    transactionList.clear();
                    double totalIncome = 0;
                    double totalExpense = 0;
                    for (DocumentSnapshot doc : value.getDocuments()) {
                        Transaction t = doc.toObject(Transaction.class);
                        if (t != null) {
                            transactionList.add(t);
                            if ("APPROVED".equals(t.getStatus())) {
                                if ("INCOME".equals(t.getType())) totalIncome += t.getAmount();
                                else totalExpense += t.getAmount();
                            }
                        }
                    }
                    adapter.notifyDataSetChanged();
                    updateUI(totalIncome, totalExpense);
                });
    }

    private void updateUI(double income, double expense) {
        tvIncome.setText("Inc: $" + income);
        tvExpense.setText("Exp: $" + expense);
        double balance = (allocatedBudget + income) - expense;
        tvBalance.setText("Bal: $" + balance);
        if (balance < 0) tvBalance.setTextColor(Color.RED);
        else tvBalance.setTextColor(Color.parseColor("#4CAF50"));

        try { if (pieChart != null) updateChart(income, expense, balance); } catch (Exception e) {}
    }

    private void updateChart(double income, double expense, double balance) {
        List<PieEntry> entries = new ArrayList<>();
        double totalAvailable = allocatedBudget + income;
        double remaining = totalAvailable - expense;
        if(remaining < 0) remaining = 0;

        if (expense == 0 && remaining == 0) {
            pieChart.clear();
            return;
        }

        entries.add(new PieEntry((float) expense, "Expenses"));
        entries.add(new PieEntry((float) remaining, "Remaining"));

        PieDataSet dataSet = new PieDataSet(entries, "Budget");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextSize(12f);
        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        pieChart.getDescription().setEnabled(false);
        pieChart.setCenterText("Overview");
        pieChart.invalidate();
    }
}