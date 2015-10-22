/*
 * Copyright 2014 IBM Corp. All Rights Reserved
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0 
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ibm.bluelist;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.ActionMode;

import com.ibm.mobile.services.cloudcode.IBMCloudCode;
import com.ibm.mobile.services.core.http.IBMHttpResponse;
import com.sensoro.beacon.kit.Beacon;
import com.sensoro.beacon.kit.BeaconManagerListener;
import com.sensoro.cloud.SensoroManager;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import bolts.Continuation;
import bolts.Task;

public class MainActivity extends Activity {

	MessengerApplication blApplication;
	ActionMode mActionMode = null;
	int listItemPosition;
	public static final String CLASS_NAME = "MainActivity";

    public Intent myIntent;
    private final static int REQUEST_ENABLE_BT = 1;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SensoroManager sensoroManager = SensoroManager.getInstance(getApplicationContext());
	    blApplication = (MessengerApplication) getApplication();
        if (getIntent().getBooleanExtra("EXIT", false)) {
            finish();
        }
        myIntent = new Intent(getApplicationContext(), PostTrackingNotifier.class);
        myIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        if (sensoroManager.isBluetoothEnabled()) {
            /**
             * Enable cloud service (upload sensor data, including battery status, UMM, etc.)。Without setup, it keeps in closed status as default.
             **/
            sensoroManager.setCloudServiceEnable(true);
            /**
             * Enable SDK service
             **/
            try {
                sensoroManager.startService();
            } catch (Exception e) {
                e.printStackTrace(); // Fetch abnormal info
            }
        }
        else
        {
            Intent bluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(bluetoothIntent, REQUEST_ENABLE_BT);
        }
        BeaconManagerListener beaconManagerListener = new BeaconManagerListener() {
            ArrayList<String> foundSensors = new ArrayList<String>();

            @Override
            public void onUpdateBeacon(ArrayList<Beacon> beacons) {
                // Refresh sensor info
                for (Beacon beacon : beacons) {
                    if (true) {
                        if (beacon.getMovingState() == Beacon.MovingState.DISABLED) {
                            // Disable accelerometer
                            System.out.println(beacon.getSerialNumber() + "Disabled");
                            Log.d("Main", beacon.getSerialNumber() + "Disabled");


                        } else if (beacon.getMovingState() == Beacon.MovingState.STILL) {
                            // Device is at static
                            Log.d("Main", beacon.getSerialNumber() + "static");
                            Log.d("Main", beacon.getProximityUUID());
                            if (!foundSensors.contains(beacon.getProximityUUID())) {
                                //String messageDisplay = "";
                                Log.d("Main", "getting Beacon Info");
                                foundSensors.add(beacon.getProximityUUID());
                                IBMCloudCode.initializeService();
                                IBMCloudCode myCloudCodeService = IBMCloudCode.getService();
                                String id = beacon.getProximityUUID();
                                String url = "/getNotification?id="+id;
                                myCloudCodeService.get(url).continueWith(new Continuation<IBMHttpResponse, Void>() {

                                    @Override
                                    public Void then(Task<IBMHttpResponse> task) throws Exception {
                                        if (task.isCancelled()) {
                                            Log.e(CLASS_NAME, "Exception : Task" + task.isCancelled() + "was cancelled.");
                                        } else if (task.isFaulted()) {
                                            Log.e(CLASS_NAME, "Exception : " + task.getError().getMessage());
                                        } else {
                                            InputStream is = task.getResult().getInputStream();
                                            try {
                                                BufferedReader in = new BufferedReader(new InputStreamReader(is));
                                                String responseString = "";
                                                String myString = "";
                                                while ((myString = in.readLine()) != null)
                                                    responseString += myString;

                                                in.close();
                                                Log.i(CLASS_NAME, "Response Body: " + responseString);
                                                JSONObject obj = new JSONObject(responseString);
                                                final String name = obj.getString("message");
                                                final String meets = obj.getString("meetups");

                                                /*Notification*/

                                                myIntent.putExtra("response", responseString);
                                                PendingIntent pendingNotificationIntent = PendingIntent.getActivity(getApplicationContext(),5,myIntent,PendingIntent.FLAG_UPDATE_CURRENT);
                                                final NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext());
                                                builder.setSmallIcon(R.drawable.logo);
                                                builder.setContentTitle("IBM Beacons Messenger");
                                                builder.setContentText(name);
                                                builder.setContentIntent(pendingNotificationIntent);
                                                NotificationManager notificationManager = (NotificationManager) getSystemService(
                                                        Context.NOTIFICATION_SERVICE);
                                                notificationManager.notify(5, builder.build());




                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }


                                            Log.i(CLASS_NAME, "Response Status from login: " + task.getResult().getHttpResponseCode());
                                        }

                                        return null;
                                    }

                                });

                            }
                            Log.d("Main", beacon.getMajor().toString());
                            Log.d("Main", beacon.getMinor().toString());
                            //Log.d("Main",beacon.getRssi().toString());
                            System.out.println(beacon.getSerialNumber() + "static");

                        } else if (beacon.getMovingState() == Beacon.MovingState.MOVING) {
                            // Device is moving
                            Log.d("Main", beacon.getSerialNumber() + "moving");
                            Log.d("Main", beacon.getProximityUUID());
                            System.out.println(beacon.getSerialNumber() + "moving");
                        }
                    }
                }
            }

            @Override
            public void onNewBeacon(Beacon beacon) {
                // New sensor found
                System.out.println(beacon.getSerialNumber());
                Log.d("Main", beacon.getSerialNumber() + "Got New");
            }

            @Override
            public void onGoneBeacon(Beacon beacon) {
                // A sensor disappears from the range
                System.out.println(beacon.getSerialNumber());

            }
        };
        sensoroManager.setBeaconManagerListener(beaconManagerListener);
    }
}
