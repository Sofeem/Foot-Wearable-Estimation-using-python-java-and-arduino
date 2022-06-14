package com.example.hp.rbluetooth;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Environment;
import android.os.ParcelUuid;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Button;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends AppCompatActivity  implements ActivityCompat.OnRequestPermissionsResultCallback {

    TextView myLabel;
    EditText myTextbox;
    BluetoothAdapter mBluetoothAdapter;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    OutputStream mmOutputStream;
    InputStream mmInputStream;
    Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition;
    int counter;
    volatile boolean stopWorker;
    LineChart linechart;
    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
    String currentDateandTime = sdf.format(new Date());
    public String path = Environment.getExternalStorageDirectory().getAbsolutePath()+"/appData/sensorData.txt";
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    ArrayList<String> Store = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button openButton = (Button) findViewById(R.id.open);
        Button sendButton = (Button) findViewById(R.id.send);
        Button closeButton = (Button) findViewById(R.id.close);
        myLabel = (TextView) findViewById(R.id.label);

        // Storage Permissions




        //Open Button
        openButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                findBT();
                try {

                    openBT();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });


        //Close button
        closeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    closeBT();
                } catch (IOException ex) {
                }
            }
        });



    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        Log.d("check","i am in2");
        switch (requestCode) {
            case REQUEST_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {


                        Log.d("check","i am in");
                        beginListenForData();

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }





    void findBT() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            myLabel.setText("No bluetooth adapter available");
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                ParcelUuid list[] = device.getUuids();
                if (list != null) {
                    ArrayList<String> uuidStrings = new ArrayList<>(list.length);
                    for (ParcelUuid parcelUuid : list) {
                        uuidStrings.add(parcelUuid.getUuid().toString());
                        //Log.d("UUIdName",parcelUuid.getUuid().toString());
                        if (device.getName().equals("HC-05")) {


                            Log.d("DeviceName",device.getName());
                            Log.d("UUIdName",parcelUuid.getUuid().toString());

                            //Log.d("List of decives", String.valueOf( pairedDevices));
                            mmDevice = device;
                            break;
                        }
                    }

                }

                //Log.d("DeviceName",device.getName());



            }
        }

        myLabel.setText("Bluetooth Device Found");
    }

    void openBT() throws IOException {
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); //Standard SerialPortService ID

        mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
        mmSocket.connect();
        mmOutputStream = mmSocket.getOutputStream();
        mmInputStream = mmSocket.getInputStream();
        String check = "key";

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            Log.d(check, "i am in");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_EXTERNAL_STORAGE);

        }
        else{
            Log.d(check, "i am in2");
         beginListenForData();
    }


        myLabel.setText("Bluetooth Opened");
    }

    void beginListenForData() {
        final Handler handler = new Handler();
        final byte delimiter = 10; //This is the ASCII code for a newline character
        Store.add(0,"TimeinSec,Temperature,Humidity,Voltage,Force");
        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        workerThread = new Thread(new Runnable() {
            public void run() {
                while (!Thread.currentThread().isInterrupted() && !stopWorker) {
                    try {
                        int bytesAvailable = mmInputStream.available();
                        if (bytesAvailable > 0) {
                            byte[] packetBytes = new byte[bytesAvailable];
                            mmInputStream.read(packetBytes);

                            for (int i = 0; i < bytesAvailable; i++) {
                                byte b = packetBytes[i];
                                if (b == delimiter) {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;


                                    handler.post(new Runnable() {
                                        public void run() {

                                            int i = 1;
                                            myLabel.setText(data);

                                            Store.add(i,data);
                                            i++;




                                            /*linechart = (LineChart) findViewById(R.id.LineChart);
                                            ArrayList<String> xaxis = new ArrayList<>();
                                            ArrayList<Entry> yaxis = new ArrayList<>();
                                            float temp = Float.parseFloat(data);


                                            for (int k = 0; k < 100; k=k+5){
                                                yaxis.add(new Entry(temp, k));

                                                LineDataSet lineDateSet = new LineDataSet(yaxis,"Temp");
                                                lineDateSet.setColor(Color.BLUE);
                                                ArrayList<ILineDataSet> lineDataSets = new ArrayList<>();
                                                lineDataSets.add(lineDateSet);

                                                LineData lineData = new LineData(lineDataSets);

                                                linechart.setData(lineData);




                                            }*/
                                        }
                                    });
                                } else {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    } catch (IOException ex) {
                        stopWorker = true;
                    }
                }
            }
        });

        workerThread.start();
    }

    void sendData() throws IOException {
        /*String msg = myTextbox.getText().toString();
        msg += "\n";
        mmOutputStream.write(msg.getBytes());
        myLabel.setText("Data Sent");*/

    }

    void closeBT() throws IOException {
        stopWorker = true;
        String [] data = new String[Store.size()];
        for(int i = 0; i<data.length;i++){
            data[i] = Store.get(i);


        }
        File file = new File(path);

        if(file.exists()){
            String check = "key";
            Log.d(check, "i am in3");
            Save(file,data);

        }
        else{
            String check = "key";
            Log.d(check, "i am in not");
        }
   //     mmOutputStream.close();
        mmInputStream.close();
        mmSocket.close();
        myLabel.setText("Bluetooth Closed");
    }
    /*public static void requestStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }*/


    public static void Save(File file, String[] data)
    {
        FileOutputStream fos = null;
        String check = "key";
        Log.d(check, "i am in3");
        try
        {
            fos = new FileOutputStream(file);
        }
        catch (FileNotFoundException e) {e.printStackTrace();}
        try
        {
            try
            {
                for (int i = 0; i<data.length; i++)
                {

                    fos.write(data[i].getBytes());
                    if (i < data.length-1)
                    {
                        Log.d(check, "i am in4");
                        Log.d("Data",data[i]);
                        fos.write("\n".getBytes());
                    }
                }
            }
            catch (IOException e) {e.printStackTrace();}
        }
        finally
        {
            try
            {
                fos.close();
            }
            catch (IOException e) {e.printStackTrace();}
        }
    }

    }



