package com.kiboi.fluffyqr;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import com.budiyev.android.codescanner.CodeScanner;
import com.budiyev.android.codescanner.CodeScannerView;
import com.budiyev.android.codescanner.DecodeCallback;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.CSVWriterBuilder;
import com.opencsv.ICSVWriter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Environment;
import android.text.InputType;
import android.util.Log;
import android.view.ActionMode;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

//    public final String SCANNER_ACTIVITY ="qrscanner";
    public final int RCODE_SCANNER_ACTIVITY = 0;
    public final int ACTIVITY_CHOOSE_FILE1 = 1;

    private static DatabaseReference mDatabase;
//    static String DBNAME = "fluffy";
//    static String DBNAME = "fluffy2";
    static String DBNAME = "hackathon2020";
    static Query myQuery = null;
    private final String TAG = "FluffyQR";

    private RecyclerView recyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager layoutManager;
    ArrayList<Person> myDataset;
    HashMap<String, Person> mHashPerson;


    ValueEventListener eventListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            myDataset.clear();
            mHashPerson.clear();
            for(DataSnapshot ds : dataSnapshot.getChildren()) {
                try {
                    Log.d(TAG,ds.getKey());
                    Log.d(TAG,ds.child("firstname").getValue(String.class));
                    Person pData = ds.getValue(Person.class);
                    Log.d(TAG, "KEY::" + ds.getKey());
                    if(pData.date.trim().length()>0) {
                        myDataset.add(pData);
                    }
                    else
                    {
                        Log.d(TAG,"date has laman oh no ["+pData.date.trim().length()+"]");
                    }
                    mHashPerson.put(ds.getKey(), pData);

                    String msg = ds.child("firstname").getValue(String.class);
                    Log.d(TAG, "DATA FIRSTNAME:>>> " + pData.firstname+" "+pData.lastname);
                    Log.d(TAG, "HASH GET:>>> " + mHashPerson.get(ds.getKey()));
                    Log.d(TAG,"DATASET"+myDataset.toString());
                }catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
            mAdapter = new MyAdapter(MainActivity.this,myDataset);
            recyclerView.setAdapter(mAdapter);
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
            Log.d(TAG,"DATABASE ERROR: "+databaseError.getDetails());
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setTitle(DBNAME);

        myDataset = new ArrayList<Person>();
        mHashPerson = new HashMap<String,Person>();


        //load firebase
        FirebaseApp.initializeApp(this);
        mDatabase = FirebaseDatabase.getInstance().getReference();
        myQuery = mDatabase.child(DBNAME);
        //LAYOUT
        myQuery.addValueEventListener(eventListener);

        Log.d(TAG,"REPO:: "+myQuery.toString());

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
                Intent intent = new Intent(getApplicationContext(),QRScannerActivity.class);
                startActivityForResult(intent,RCODE_SCANNER_ACTIVITY);
            }
        });


        //check camera permission
        String[] permissions = {Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED ||
            ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED ||
                ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED)
        {
            ActivityCompat.requestPermissions(this, permissions, 11);
        }



        recyclerView = (RecyclerView)findViewById(R.id.my_recycler_view);
        recyclerView.setHasFixedSize(true);

        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        Log.d(TAG,"DATASET"+myDataset.toString());
        mAdapter = new MyAdapter(this,myDataset);
        recyclerView.setAdapter(mAdapter);


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 11) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permissions granted", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show();
            }
            Log.d(TAG,"Permissions accepted");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            Toast.makeText(this, "Attendance: "+myDataset.size()+"/"+mHashPerson.size(), Toast.LENGTH_LONG).show();
//        }

        switch(id)
        {
            case R.id.action_settings:
                Toast.makeText(this, "Attendance: "+myDataset.size()+"/"+mHashPerson.size(), Toast.LENGTH_LONG).show();
                break;
            case R.id.upload_csv:
                uploadCSV();
                break;
            case R.id.download_csv:
                downloadCSV();
                break;
            case R.id.reset_attendance:
                break;
            case R.id.change_db:
                changeDB();
                break;
            case R.id.delete_db:
                clearDB();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void downloadCSV()
    {
        CSVWriter writer = null;
        try
        {
            String filename = DBNAME+"_"+getTime()+".csv";
            File path = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator);
            File file = new File(path, filename);

            Log.d(TAG,"FILEPATH: "+file.getAbsolutePath());
            FileWriter out = new FileWriter(new File(path, filename));

            FileOutputStream stream = new FileOutputStream(file);

            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(stream);
            outputStreamWriter.write("this is a sample");

            writer = new CSVWriter(out);
            Iterator it = mHashPerson.entrySet().iterator();


            Log.d(TAG,"START EXPORT:: "+mHashPerson.size());
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry)it.next();
                Person pData = (Person)pair.getValue();
                String[] entries = {pData.firstname,pData.lastname,pData.date};
                Log.d(TAG,"EXPORT:"+entries[0]);
                writer.writeNext(entries);

            }

            writer.flush();
            writer.close();

            Toast.makeText(this,"Done exporting CSV ["+filename+"]. "+mHashPerson.size()+" records exported.",Toast.LENGTH_LONG).show();
        }
        catch (Exception e)
        {
            //error
            e.printStackTrace();
        }
    }

    public String getTime() {
        return new SimpleDateFormat("dd-MM-yy_HH:mm:ss").format(new Date());
    }

    /**
     * file picker intent
     */
    private void uploadCSV()
    {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/*");
        startActivityForResult(Intent.createChooser(intent, "Open CSV"), ACTIVITY_CHOOSE_FILE1);
    }

    /**
     * change the current db
     */
    private void changeDB()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Change Database");
        builder.setMessage("Current database is "+DBNAME);

        // Set up the input
        final EditText input = new EditText(this);

        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                DBNAME = input.getText().toString();
                getSupportActionBar().setTitle(DBNAME);
                myQuery = mDatabase.child(DBNAME);
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG,"REQUEST CODE:"+requestCode);
        if(data!=null) {
            switch(requestCode)
            {
                case RCODE_SCANNER_ACTIVITY:
                    if (resultCode == RESULT_OK) {
                        Log.d(TAG, "REQ CODE:: " + requestCode);
                        Log.d(TAG, "DATA FROM QR: " + data.getStringExtra("name"));
                        handleQRCode(data.getStringExtra("name"));
                    }
                    break;
                case ACTIVITY_CHOOSE_FILE1:
                    if (resultCode == RESULT_OK){
                        processCSV(new File(data.getData().getPath()),data.getData());
                    }
                    break;
            }
        }
    }

    private void processCSV(File csvFile, Uri uriF)
    {
        Log.d(TAG,"CSV FILE  READ: "+Environment.getExternalStorageDirectory() + "/"+csvFile.getName());
        Log.d(TAG,"CAN READ: "+csvFile.canRead());
        final int KEY = 0;
        final int FIRSTNAME = 1;
        final int LASTNAME = 2;
        final int SHAKEY = 4;
        try {
            DatabaseReference fluffyRef = mDatabase.child(DBNAME);
            InputStream istream = getContentResolver().openInputStream(uriF);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(istream));

            CSVReader reader = new CSVReader(bufferedReader);
            reader.skip(1);
            String[] nextLine;

            int records = 0;
            while ((nextLine = reader.readNext()) != null) {
                // nextLine[] is an array of values from the line
                Log.d(TAG,"Line: "+nextLine[FIRSTNAME]);
                String nkey = nextLine[0];
                Person newPerson = new Person();
                newPerson.firstname = nextLine[FIRSTNAME];
                newPerson.lastname = nextLine[LASTNAME];
                String hashKey = nextLine[SHAKEY];
                fluffyRef.child(hashKey).setValue(newPerson);
                records++;
            }

            Toast.makeText(this,"Done importing CSV. "+records+" records read.",Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void clearDB()
    {
        DatabaseReference fluffyRef = mDatabase.child(DBNAME);
        fluffyRef.setValue(null);
    }

    /**
     * handle the qr code
     * @param qrData
     */
    void handleQRCode(String qrData)
    {
        String key = qrData;

//        if(DBNAME.equalsIgnoreCase("fluffy2"))
//        {
//            key = qrData.replace(" ","_");
//            key = key.replace(".","_");
//        }

//        Log.d(TAG,"FIRSTNAME: "+names[0]);
//        Log.d(TAG,"FIRSTNAME: "+names[1]);

        if(mHashPerson.containsKey(key))
        {
            Log.d(TAG,"HAS KEY");
            Person mRegPerson = mHashPerson.get(key);

            Log.d(TAG,"FIRSTNAME: "+mRegPerson.firstname);
            Log.d(TAG,"LASTNME: "+mRegPerson.lastname);
            Log.d(TAG,"DATE: "+mRegPerson.date);


            if(mRegPerson.date.trim().length()>0) {
                new AlertDialog.Builder(this)
                        .setTitle("Already Registered [" + mRegPerson.firstname + " " + mRegPerson.lastname + "]")
                        .setMessage("Person already registered: " + mRegPerson.date)
                        .setNegativeButton(android.R.string.no, null)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            }
            else
            {
                showDialog(this, "Register: " + mRegPerson.firstname + " " + mRegPerson.lastname, mRegPerson,key);
            }
        }
        else
        {
            new AlertDialog.Builder(this)
                    .setTitle("Not found")
                    .setMessage("User not found in list of participants. Please scan another code.")
                    .setNegativeButton(android.R.string.no, null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }
    }


    //DIALOG
    public void showDialog(Activity activity, String msg, Person data,String key){
        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);
        dialog.setContentView(R.layout.dialog_c);

        final Person personData = data;
        final String hashKey = key;

        TextView text = (TextView) dialog.findViewById(R.id.text_dialog);
        text.setText(msg);

        Button dialogButton = (Button) dialog.findViewById(R.id.btn_dialog);
        Button dialogCancel = (Button) dialog.findViewById(R.id.btn_cancel);

        dialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String qrName = personData.firstname+"_"+personData.lastname;


                try {
//                    String hashQR=key;
//                    if(DBNAME.equalsIgnoreCase("fluffy")) {
//                        MessageDigest digest = MessageDigest.getInstance("SHA-256");
//                        byte[] hash = digest.digest(qrName.getBytes(StandardCharsets.UTF_8));
//
//
//                        StringBuffer hexString = new StringBuffer();
//
//                        for (int i = 0; i < hash.length; i++) {
//                            String hex = Integer.toHexString(0xff & hash[i]);
//                            if (hex.length() == 1) hexString.append('0');
//                            hexString.append(hex);
//                        }
//
//                        hashQR = hexString.toString();
//                    }
//                    else
//                    {
//                        hashQR = qrName.replace(" ","_");
//                        hashQR = hashQR.replace(".","_");
//                    }

                    DatabaseReference fluffyRef = mDatabase.child(DBNAME);
                    String timeStamp = new SimpleDateFormat("MM/dd/yyyy HH:mm").format(new Date());
                    personData.date = timeStamp;
                    fluffyRef.child(hashKey).setValue(personData);
                }catch(Exception e)
                {

                }
                dialog.dismiss();
            }
        });

        dialogCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();

    }
}
