package com.example.clubeventportal;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileWriter;

public class CsvExportUtils {

    public static void exportData(Context context, String fileName, String content) {
        try {
            // Create file in Cache Directory (No permissions needed for cache)
            File file = new File(context.getCacheDir(), fileName + ".csv");
            FileWriter writer = new FileWriter(file);
            writer.append(content);
            writer.flush();
            writer.close();

            // Share File
            Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/csv");
            intent.putExtra(Intent.EXTRA_SUBJECT, "Export: " + fileName);
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(Intent.createChooser(intent, "Share Report"));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}