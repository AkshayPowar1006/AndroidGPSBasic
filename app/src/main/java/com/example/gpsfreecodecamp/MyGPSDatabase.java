package com.example.gpsfreecodecamp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import java.util.ArrayList;

public class MyGPSDatabase extends SQLiteOpenHelper {

    public static final  String DB_NAME = "gps_log.db";
    public static final String TABLE_NAME = "LogData";
    public static final String KEY_ID = "id";
    public static final String TIME = "time";
    public static final String LAT = "lat";
    public static final String LON = "lon";

    public MyGPSDatabase(@Nullable Context context) {
        super(context, DB_NAME, null, 1);
        SQLiteDatabase db = this.getWritableDatabase();
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table "+ TABLE_NAME+ " (id INTEGER PRIMARY KEY AUTOINCREMENT, time TEXT, lat TEXT, lon TEXT)");

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("drop table if exists " + TABLE_NAME);

    }

    public boolean insertPositionsData(String time, String lat, String lon){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(TIME,time);
        values.put(LAT, lat);
        values.put(LON,lon);


        long result = db.insert(TABLE_NAME, null, values);
        return result != -1;

    }

    public ArrayList<Position> getPositionData(){
        SQLiteDatabase db = this.getReadableDatabase();
        ArrayList<Position> arrayList = new ArrayList<>();
        Cursor cursor = db.rawQuery("select * from "+TABLE_NAME, null);
        while (cursor.moveToNext()){
            int id = cursor.getInt(0);
            String time = cursor.getString(1);
            String lat = cursor.getString(2);
            String lon = cursor.getString(3);

            Position position = new Position(id,time,lat,lon);
            arrayList.add(position);
        }
        return arrayList;

    }
}
