package com.sparkapp.spark;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends Activity {

    public static final UUID uuid = UUID.fromString("b042294f-84f1-43ca-b7f5-a84631b084f7");
    private static final int DISCOVERABLE_ACTIVITY_RESULT = 42;
    public static String channel_id = null;

    BluetoothAdapter adapter;
    List<BluetoothDevice> devices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageButton connectButton = (ImageButton)findViewById(R.id.connect);
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //new ChannelDialog().show(getFragmentManager(), "Channel");
                startDiscovery();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void startDiscovery() {
        adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            Log.e("BLUETOOTH", "NO BLUETOOTH?!?!?!");
        }
        Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivity(enableBluetoothIntent);

        devices = new ArrayList<BluetoothDevice>();

        BroadcastReceiver foundReciever = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction()))
                    devices.add((BluetoothDevice) intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE));
            }
        };
        IntentFilter foundFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(foundReciever, foundFilter);

        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivity(discoverableIntent);

        BroadcastReceiver finishedReciever = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                Log.d("DONE", "Done with recieving");
                adapter.cancelDiscovery();
                startConnection();
            }
        };
        IntentFilter finishedFilter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(finishedReciever, finishedFilter);

        adapter.startDiscovery();
    }

    public void startConnection() {
        List<BluetoothSocket> sockets = new ArrayList<BluetoothSocket>();
        BluetoothServerSocket serverSocket = null;
        try {
            serverSocket = adapter.listenUsingRfcommWithServiceRecord(adapter.getName(), uuid);
        } catch (IOException e) {
            Log.e("BLUETOOTH", "ERROR: " + e.toString(), e);
        }

        Log.d("BLUETOOTH", "Searching for connections");
        boolean done = false;
        while (!done) {
            try {
                BluetoothSocket socket = serverSocket.accept(10000);
                sockets.add(socket);
            } catch (IOException e) {
                if (e.getMessage().equals("Try again"))
                    done = true;
                else
                    Log.e("BLUETOOTH", "ERROR: " + e.getMessage(), e);
            }
        }

        for (BluetoothSocket s : sockets) {
            try {
                new Thread(new InputHandler(s.getInputStream())).start();
                s.getOutputStream().write(42);
            } catch (IOException e) {
                Log.e("BLUETOOTH", "Failed to get input stream: " + e);
            }
        }

        for (BluetoothDevice device : devices) {
            Log.d("DEVICE", device.getName() + " " + device.getAddress());
            try {
                BluetoothSocket socket = device.createRfcommSocketToServiceRecord(uuid);
                socket.connect();
                new Thread(new InputHandler(socket.getInputStream())).start();
                socket.getOutputStream().write(42);
            } catch (IOException e) {
                Log.e("BLUETOOTH", "ERROR: " + e.toString(), e);
            }
        }
    }
}
