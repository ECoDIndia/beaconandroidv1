package com.ibm.bluelist;

/**
 * Created by root on 25/7/15.
 */

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class PostTrackingNotifier extends Activity {

    public static ListView list;
    public static ArrayList<String> web;
    public static ArrayList<Integer> imageId;
    public static ArrayList<String> address;
    public static ArrayList<String> url;
    public static ArrayList<String> meetTime;
    public static String CLASS_NAME="PostTrackingNotifier";
    public static ProgressDialog pDialog;
    public static CustomList adapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.posttrackingnotifier);
        web = new ArrayList<String>();
        imageId = new ArrayList<Integer>();
        address = new ArrayList<String>();
        url = new ArrayList<String>();
        meetTime = new ArrayList<String>();

        String id = getIntent().getExtras().getString("response");
        JSONObject responseJson;
        String message = "";

        try{
            responseJson = new JSONObject(id);
            message = responseJson.getString("message");
            JSONArray meetups = responseJson.getJSONArray("meetups");
            System.out.println(meetups.length());
            web.clear();
            imageId.clear();
            for (int i = 0; i < meetups.length(); i++) {
                JSONObject meet = meetups.getJSONObject(i);
                web.add(meet.getString("title"));
                if(meet.getBoolean("venueAvailable")==true)
                {
                    //if(meet.getString("address"))
                    address.add(meet.getString("address"));
                    meetTime.add(meet.getString("meetTime"));
                    url.add(meet.getString("url"));
                }
                imageId.add(R.drawable.logo);
            }
        }
        catch(JSONException ex)
        {
            ex.printStackTrace();
        }
        Log.d("New Intent", id);
        adapter = new CustomList(PostTrackingNotifier.this, web, imageId);

        list=(ListView)findViewById(R.id.list);
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                Toast.makeText(PostTrackingNotifier.this, "You Clicked at " + web.get(position), Toast.LENGTH_SHORT).show();
                final Dialog dialog = new Dialog(PostTrackingNotifier.this);
                dialog.setContentView(R.layout.customdialog);
                dialog.setTitle("Meetup Details");
                TextView text = (TextView) dialog.findViewById(R.id.text);
                text.setText(web.get(position));
                TextView venue = (TextView) dialog.findViewById(R.id.address);
                venue.setText(address.get(position));
                TextView time = (TextView) dialog.findViewById(R.id.meetTime);
                time.setText(meetTime.get(position));
                TextView browse = (TextView) dialog.findViewById(R.id.url);
                browse.setText(url.get(position));
                ImageView image = (ImageView) dialog.findViewById(R.id.image);
                image.setImageResource(imageId.get(position));
                dialog.show();
            }
        });
        new AlertDialog.Builder(PostTrackingNotifier.this)
                .setTitle("IBM Beacons Messenger")
                .setMessage(message)
                .setPositiveButton("Let's see", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // continue with delete
                        dialog.dismiss();

                        Toast.makeText(getApplicationContext(),
                                "Press back button twice to Exit",
                                Toast.LENGTH_LONG).show();

                    }
                })
                .setNegativeButton("Not Interested", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                        dialog.dismiss();
                        Toast.makeText(getApplicationContext(),
                                "Press back button twice to Exit",
                                Toast.LENGTH_LONG).show();
                        finish();
                    }
                })
                .show();
    }


    @Override
    public void onNewIntent(Intent intent)
    {
        System.out.println("CAME here");
        Bundle extras = intent.getExtras();
        if(extras != null){
            if(extras.containsKey("ID"))
            {
                String msg = extras.getString("NotificationMessage");
                System.out.println("msg");
            }
        }
    }


}