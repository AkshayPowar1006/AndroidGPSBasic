package com.example.gpsfreecodecamp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
//import android.icu.text.Transliterator;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
//import android.service.autofill.TextValueSanitizer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
//import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
//import java.nio.file.attribute.PosixFileAttributes;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public static final int defaultUpdateInterval = 30;
    public static final int fastestUpdateInterval = 5;
    public static final int PERMISSIONS_FINE_LOCATION = 99;
    TextView tv_lat, tv_lon, tv_altitude, tv_accuracy, tv_speed, tv_sensor, tv_updates, tv_address, tv_waypointCounts, tv_time;
    Switch sw_locationsupdates, sw_gps;
    Button btn_newWaypoint, btn_showWaypointList, btn_showMap, btn_logData, btn_export;
    SimpleDateFormat simpleDateFormat;
    String DateAndTime;
    Calendar calender;
    MyGPSDatabase myGPSDatabase;


    boolean updateOn = false;
    Location currentLocation;
    List<Location> savedLocations;



    //Googles API for location provider
    FusedLocationProviderClient fusedLocationProviderClient;

    // Location Request
    LocationRequest locationRequest;
    LocationCallback locationCallBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d("MSG","App is Starting");


        tv_lat = findViewById(R.id.tv_lat);
        tv_lon = findViewById(R.id.tv_lon);
        tv_altitude = findViewById(R.id.tv_altitude);
        tv_accuracy = findViewById(R.id.tv_accuracy);
        tv_speed = findViewById(R.id.tv_speed);
        tv_sensor = findViewById(R.id.tv_sensor);
        tv_updates = findViewById(R.id.tv_updates);
        tv_address = findViewById(R.id.tv_address);
        sw_locationsupdates = findViewById(R.id.sw_locationsupdates);
        sw_gps = findViewById(R.id.sw_gps);
        btn_newWaypoint = findViewById(R.id.btn_BreadCrumb);
        btn_showWaypointList = findViewById(R.id.btn_showWaypointList);
        tv_waypointCounts = findViewById(R.id.tv_countOfBreadCrumbs);
        btn_showMap = findViewById(R.id.btn_showMap);
        tv_time = findViewById(R.id.tv_time);
        btn_logData = findViewById(R.id.btnLog);
        btn_export = findViewById(R.id.btnExport);

        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000 * defaultUpdateInterval);
        locationRequest.setFastestInterval(1000 * fastestUpdateInterval);
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        calender = Calendar.getInstance();
        simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
        DateAndTime = simpleDateFormat.format(calender.getTime());

        myGPSDatabase = new MyGPSDatabase(this);

        locationCallBack = new LocationCallback() {

            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);

                //save the location
                Location location = locationResult.getLastLocation();
                updateUIValues(location);
            }
        };

        btn_newWaypoint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // get the gps location

                // add the new location to the list
                MyApplication myApplication = (MyApplication)getApplicationContext();
                savedLocations = myApplication.getMyLocations();
                savedLocations.add(currentLocation);

            }
        });

        btn_showWaypointList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, ShowSavedLocationList.class);
                startActivity(i);

            }
        });

        btn_showMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(MainActivity.this, MapsActivity.class);
                startActivity(i);

            }
        });

        sw_gps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sw_gps.isChecked()) {
                    locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                    tv_sensor.setText("Using GPS Sensors");
                } else {
                    locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
                    tv_sensor.setText("Using Towers + WIFI");
                }
            }
        });


        sw_locationsupdates.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sw_locationsupdates.isChecked()) {
                    startLocationUpdates();
                } else {
                    stopLocationUpdates();
                }
            }
        });


        updateGPS();


        btn_logData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean result = myGPSDatabase.insertPositionsData(tv_time.getText().toString(),
                                    tv_lat.getText().toString(),
                                    tv_lon.getText().toString());
                if (result){
                    Toast.makeText(MainActivity.this, "Success", Toast.LENGTH_LONG).show();
                }
                else{
                    Toast.makeText(MainActivity.this, "Fail", Toast.LENGTH_LONG).show();
                }
            }

        });


        btn_export.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(checkStoragePermission()){
                    exportCSV();

                    //exportcsvDB();
                }else{
                    requestStoragePermission();
                }

            }
        });




    }  //end of Create method

    private void exportcsvDB() {
        MyGPSDatabase myGPSDatabase = new MyGPSDatabase(getApplicationContext());
        File exportdir = new File(Environment.getExternalStorageDirectory(),"");
        if(!exportdir.exists()){
            exportdir.mkdirs();
        }
        File file = new File(exportdir, "sadda.csv");
        try{
            file.createNewFile();
            CSVWriter csvWriter = new CSVWriter(new FileWriter(file));
            SQLiteDatabase db = myGPSDatabase.getReadableDatabase();
            Cursor curCSV = db.rawQuery("SELECT * FROM LogData", null);
            csvWriter.writeNext(curCSV.getColumnNames());
            while(curCSV.moveToNext()){
                //
                String dataList[] = {curCSV.getString(0),
                        curCSV.getString(1),
                        curCSV.getString(2)};
                csvWriter.writeNext(dataList);
            }
            csvWriter.close();
            curCSV.close();
            Toast.makeText(this, exportdir.toString(), Toast.LENGTH_LONG).show();
        }catch (Exception e){
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }

    }

    private void exportCSV() {
        File folder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_ALARMS+"/Data");
        if(!folder.exists()){
            folder.mkdirs();
        }
        File file = new File(folder, "sadda.csv");

        calender = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyMMddHHmmssZZZ");
        String Datetime = sdf.format(calender.getTime());


        String csvFileName = Datetime+".csv";
        String csvFilePath = folder+"/"+csvFileName;
        Log.d("EXP", csvFilePath);
        //////////////////
         //////////////

        ArrayList<Position> dataList = new ArrayList<>();
        dataList = myGPSDatabase.getPositionData();
        try{
            file.createNewFile();
            FileWriter fw = new FileWriter(file);
            for(int i=0; i<dataList.size(); i++){
                /*fw.append("").append(String.valueOf(dataList.get(i).getId()));
                fw.append(",").append(dataList.get(i).getTime());
                fw.append(",").append(dataList.get(i).getLat());
                fw.append(",").append(dataList.get(i).getLon());
                fw.append("\n");*/
                fw.append("")
                        .append(String.valueOf(dataList.get(i).getId()))
                        .append(",")
                        .append(dataList.get(i).getTime())
                        .append(",")
                        .append(dataList.get(i).getLat())
                        .append(",")
                        .append(dataList.get(i).getLon())
                        .append("\n");
            }
            //fw.flush();
            fw.close();
            Toast.makeText(this, "Success", Toast.LENGTH_LONG).show();

        }catch(Exception e){
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();

        }
    }


    private void stopLocationUpdates() {
        tv_updates.setText("Location is tracking stopped");
        tv_time.setText("Not being tracked");
        tv_lat.setText("Not being tracked");
        tv_lon.setText("Not being tracked");
        tv_accuracy.setText("Not being tracked");
        tv_altitude.setText("Not being tracked");
        tv_speed.setText("Not being tracked");
        tv_sensor.setText("Not being tracked");
        tv_address.setText("Not being tracked");

        fusedLocationProviderClient.removeLocationUpdates(locationCallBack);

    }

    private void startLocationUpdates() {
        tv_updates.setText("Location is being tracked");
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallBack, null);
        updateGPS();


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode){
            case PERMISSIONS_FINE_LOCATION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    updateGPS();
                }
                else {
                    Toast.makeText(this, "This app requires permissions to be granted properly", Toast.LENGTH_LONG).show();
                    finish();
                }
                break;
        }
    }

    private void updateGPS(){
        //get Permissions from the user
        //get current location from fused client
        //Update the UI

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(MainActivity.this);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {

                    updateUIValues(location);
                    currentLocation = location;

                }
            });
        }
        else{
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                requestPermissions(new String[] {android.Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_FINE_LOCATION);
            }

        }
    }

    private void updateUIValues(Location location) {
        // Update the text view objects with new location

        calender = Calendar.getInstance();
        simpleDateFormat = new SimpleDateFormat("HH:mm:ss.SSS");
        DateAndTime = simpleDateFormat.format(location.getTime());

        Date ms = calender.getTime();
        Log.d("MS", String.valueOf(ms));

        Date date = new Date();
        long Millisecond1 = date.getTime();



        tv_time.setText(DateAndTime);
        tv_accuracy.setText(String.valueOf(location.getAccuracy()));
        tv_lon.setText(String.valueOf(location.getLongitude()));
        tv_lat.setText(String.valueOf(location.getLatitude()));


        if (location.hasAltitude()){
            tv_altitude.setText(String.valueOf(location.getAltitude()));
        }
        else {
            tv_altitude.setText("Not Available");
        }

        if (location.hasSpeed()){
            tv_speed.setText(String.valueOf(location.getSpeed()));
        }
        else {
            tv_speed.setText("Not Available");
        }

        Geocoder geocoder = new Geocoder(this);
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            tv_address.setText((addresses.get(0).getAddressLine(0)));
        }
        catch (Exception e){
            tv_address.setText("Unable to find Address");

        }

        MyApplication myApplication = (MyApplication)getApplicationContext();
        savedLocations = myApplication.getMyLocations();
        // show the o´number of waypoint
        tv_waypointCounts.setText(Integer.toString(savedLocations.size()));

    }

    private boolean checkStoragePermission(){
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestStoragePermission(){
        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},100);
    }



}