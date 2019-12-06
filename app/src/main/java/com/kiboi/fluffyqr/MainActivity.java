package com.kiboi.fluffyqr;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import com.google.zxing.Result;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.ActionMode;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

//    public final String SCANNER_ACTIVITY ="qrscanner";
    public final int RCODE_SCANNER_ACTIVITY = 0;

    private DatabaseReference mDatabase;
    static String DBNAME = "fluffy";
    private final String TAG = "FQR";

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
                    mHashPerson.put(ds.getKey(), pData);
                    String msg = ds.child("firstname").getValue(String.class);
                    Log.d(TAG, "DATA FIRSTNAME:>>> " + pData.firstname);
                    Log.d(TAG, "HASH GET:>>> " + mHashPerson.get(ds.getKey()));
                }catch (Exception e)
                {
                    e.printStackTrace();
                }

            }
            mAdapter = new MyAdapter(MainActivity.this,myDataset);
            recyclerView.setAdapter(mAdapter);
//            progressBar.setVisibility(View.GONE);
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

        myDataset = new ArrayList<Person>();
        mHashPerson = new HashMap<String,Person>();


        //load firebase
        FirebaseApp.initializeApp(this);
        mDatabase = FirebaseDatabase.getInstance().getReference();
        Query myQuery = mDatabase.child(DBNAME);

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
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA}, 11);
        }

        //LAYOUT
        myQuery.addValueEventListener(eventListener);

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
                Toast.makeText(this, "camera permission granted", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show();
            }
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
        if (id == R.id.action_settings) {
            Toast.makeText(this, "Attendance: "+myDataset.size()+"/"+mHashPerson.size(), Toast.LENGTH_LONG).show();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(data!=null) {
            if (requestCode == RCODE_SCANNER_ACTIVITY) {
                if (resultCode == RESULT_OK) {
                    Log.d(TAG, "REQ CODE:: " + requestCode);
                    Log.d(TAG, "DATA FROM QR: " + data.getStringExtra("name"));
                    handleQRCode(data.getStringExtra("name"));
                }
            }
        }
    }

    /**
     * handle the qr code
     * @param qrData
     */
    void handleQRCode(String qrData)
    {
        String[] names = qrData.split("_");
//        Log.d(TAG,"FIRSTNAME: "+names[0]);
//        Log.d(TAG,"FIRSTNAME: "+names[1]);

        if(mHashPerson.containsKey(qrData))
        {
            Log.d(TAG,"HAS KEY");
            Person mRegPerson = mHashPerson.get(qrData);

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
                showDialog(this, "Register: " + mRegPerson.firstname + " " + mRegPerson.lastname, mRegPerson);
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

    public void showDialog(Activity activity, String msg, Person data){
        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(true);
        dialog.setContentView(R.layout.dialog_c);

        final Person personData = data;

        TextView text = (TextView) dialog.findViewById(R.id.text_dialog);
        text.setText(msg);

        Button dialogButton = (Button) dialog.findViewById(R.id.btn_dialog);
        Button dialogCancel = (Button) dialog.findViewById(R.id.btn_cancel);

        dialogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String qrName = personData.firstname+"_"+personData.lastname;

                try {
                    MessageDigest digest = MessageDigest.getInstance("SHA-256");
                    byte[] hash = digest.digest(qrName.getBytes(StandardCharsets.UTF_8));


                    StringBuffer hexString = new StringBuffer();

                    for (int i = 0; i < hash.length; i++) {
                        String hex = Integer.toHexString(0xff & hash[i]);
                        if(hex.length() == 1) hexString.append('0');
                        hexString.append(hex);
                    }

                    String hashQR = hexString.toString();

                    DatabaseReference fluffyRef = mDatabase.child(DBNAME);
                    String timeStamp = new SimpleDateFormat("MM/dd/yyyy HH:mm").format(new Date());
                    personData.date = timeStamp;
                    fluffyRef.child(hashQR).setValue(personData);
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
