package com.example.clubeventportal;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class NotificationUtils {

    public static final String CHANNEL_ID_FCM = "fcm_channel";
    public static final String CHANNEL_ID_REMINDER = "reminder_channel";

    // Create Channels for Android O+
    public static void createNotificationChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);

            // Channel 1: General Updates (FCM)
            NotificationChannel fcmChannel = new NotificationChannel(
                    CHANNEL_ID_FCM,
                    "Club Updates",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            fcmChannel.setDescription("Notifications for new events and updates");

            // Channel 2: Reminders (WorkManager) - High Importance
            NotificationChannel reminderChannel = new NotificationChannel(
                    CHANNEL_ID_REMINDER,
                    "Event Reminders",
                    NotificationManager.IMPORTANCE_HIGH
            );
            reminderChannel.setDescription("Reminders for registered events");

            if (manager != null) {
                manager.createNotificationChannel(fcmChannel);
                manager.createNotificationChannel(reminderChannel);
            }
        }
    }

    // Schedule a Local Reminder
    public static void scheduleEventReminder(Context context, String eventTitle, String eventDateStr, String eventTimeStr) {
        try {
            // Parse Event Date & Time
            String fullDateTime = eventDateStr + " " + eventTimeStr;
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy hh:mm a", Locale.getDefault());
            Date eventDate = sdf.parse(fullDateTime);

            if (eventDate == null) return;

            long eventMillis = eventDate.getTime();
            long nowMillis = System.currentTimeMillis();

            // Schedule for 2 hours before event
            long reminderMillis = eventMillis - (2 * 60 * 60 * 1000);
            long delay = reminderMillis - nowMillis;

            if (delay > 0) {
                // Prepare Data to pass to Worker
                Data inputData = new Data.Builder()
                        .putString("title", "Upcoming Event: " + eventTitle)
                        .putString("message", "Don't forget! The event starts in 2 hours.")
                        .build();

                // Create Work Request
                OneTimeWorkRequest reminderRequest = new OneTimeWorkRequest.Builder(ReminderWorker.class)
                        .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                        .setInputData(inputData)
                        .addTag("reminder_" + eventTitle)
                        .build();

                // Enqueue
                WorkManager.getInstance(context).enqueue(reminderRequest);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}