package com.example.clubeventportal;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "ClubManager.db";
    private static final int DATABASE_VERSION = 5; // BUMP VERSION
    private static final String TABLE_EVENTS = "events";

    // Columns
    private static final String COL_ID = "id";
    private static final String COL_TITLE = "title";
    private static final String COL_DESC = "description";
    private static final String COL_DATE = "date";
    private static final String COL_TIME = "time";
    private static final String COL_VENUE = "venue";
    private static final String COL_LIMIT = "reg_limit";
    private static final String COL_FEE = "fee";
    private static final String COL_DURATION = "duration"; // New
    private static final String COL_IMAGE = "image_url";
    private static final String COL_CLUB_NAME = "club_name";
    private static final String COL_CLUB_ID = "club_id";
    private static final String COL_BUDGET = "budget";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_EVENTS + " ("
                + COL_ID + " TEXT PRIMARY KEY,"
                + COL_TITLE + " TEXT,"
                + COL_DESC + " TEXT,"
                + COL_DATE + " TEXT,"
                + COL_TIME + " TEXT,"
                + COL_VENUE + " TEXT,"
                + COL_LIMIT + " INTEGER,"
                + COL_FEE + " REAL,"
                + COL_DURATION + " INTEGER," // New
                + COL_IMAGE + " TEXT,"
                + COL_CLUB_NAME + " TEXT,"
                + COL_CLUB_ID + " TEXT,"
                + COL_BUDGET + " REAL)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_EVENTS);
        onCreate(db);
    }

    public void syncEvent(ClubEvent event) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_ID, event.getEventId());
        values.put(COL_TITLE, event.getTitle());
        values.put(COL_DESC, event.getDescription());
        values.put(COL_DATE, event.getDate());
        values.put(COL_TIME, event.getTime());
        values.put(COL_VENUE, event.getVenue());
        values.put(COL_LIMIT, event.getRegistrationLimit());
        values.put(COL_FEE, event.getEntryFee());
        values.put(COL_DURATION, event.getAttendanceDuration()); // Sync
        values.put(COL_IMAGE, event.getPosterUrl());
        values.put(COL_CLUB_NAME, event.getClubName());
        values.put(COL_CLUB_ID, event.getClubId());
        values.put(COL_BUDGET, event.getBudget());

        db.replace(TABLE_EVENTS, null, values);
        db.close();
    }

    public ArrayList<ClubEvent> getAllEvents() {
        ArrayList<ClubEvent> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_EVENTS, null);

        if (cursor.moveToFirst()) {
            do {
                list.add(new ClubEvent(
                        cursor.getString(0), // id
                        cursor.getString(1), // title
                        cursor.getString(2), // desc
                        cursor.getString(3), // date
                        cursor.getString(4), // time
                        cursor.getString(5), // venue
                        cursor.getInt(6),    // limit
                        cursor.getDouble(7), // fee
                        cursor.getInt(8),    // duration (New index)
                        cursor.getString(9), // poster
                        cursor.getString(10), // clubName
                        cursor.getString(11),// clubId
                        cursor.getDouble(12) // budget
                ));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }
}