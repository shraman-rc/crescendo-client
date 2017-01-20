package com.example.firstapp;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.lang.Integer.parseInt;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    //public final static String EXTRA_MESSAGE = "com.example.myfirstapp.MESSAGE";
    public final static String GROUPID_EXTRA = "com.example.myfirstapp.GROUPID";
    private static int group_id;

    // yisha start
    public static final String EXTRAS_DEVICE_NAME = "HMSoft";
    public static final String EXTRAS_DEVICE_ADDRESS = "5C:F8:21:F9:47:DA";
    private TextView isSerial;
    private TextView mConnectionState;
    private TextView mDataField;
    private String mDeviceName;
    private String mDeviceAddress;
    //  private ExpandableListView mGattServicesList;
    private BluetoothLeService mBluetoothLeService;
    private boolean mConnected = false;
    private BluetoothGattCharacteristic characteristicTX;
    private BluetoothGattCharacteristic characteristicRX;


    public final static UUID HM_RX_TX =
            UUID.fromString(SampleGattAttributes.HM_RX_TX);

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";
    // yisha end

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Display firebase token
        Log.d(TAG, "Firebase token: " + MyFirebaseInstanceIDService.token);

        // yisha start
//        final Intent intent = getIntent();
        mDeviceName = EXTRAS_DEVICE_NAME;
        mDeviceAddress = EXTRAS_DEVICE_ADDRESS;

        // Sets up UI references.
        mConnectionState = (TextView) findViewById(R.id.text_view_status);

        mDataField = (TextView) findViewById(R.id.text_view_data);

        final Button buttonOn = (Button) findViewById(R.id.OnLED);
        buttonOn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                changeLED("M");
            }
        });

//        getActionBar().setTitle(mDeviceName);
//        getActionBar().setDisplayHomeAsUpEnabled(true);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
        // yisha end
    }

    /** Called when the user clicks the Register Group button */
    public void createGroup(View view) {
        //Intent intent = new Intent(this, RegisterGroupActivity.class);
        //startActivity(intent);
        requestRegister();
    }

    /** Called when the user clicks the Join Group button */
    public void joinGroup(View view) {
        EditText editText = (EditText) findViewById(R.id.edit_groupid);
        int gid = parseInt(editText.getText().toString());
        requestJoin(gid);
        //Intent intent = new Intent(this, JoinGroupActivity.class);
        //intent.putExtra(GROUPID_EXTRA, gid);
        //startActivity(intent);
    }

    /** Called when the user clicks the Register Group button */
    public void alert(View view) {
        //Intent intent = new Intent(this, AlertActivity.class);
        //startActivity(intent);
        requestAlert();
    }

    private void requestRegister() {
        // Configure URL
        String protocol = getString(R.string.app_server_protocol);
        String host = getString(R.string.app_server_host);
        int port = this.getResources().getInteger(R.integer.app_server_port);
        String path = getString(R.string.app_server_path_reg_group);
        String url = "";
        try {
            url = new URL(protocol, host, port, path).toString();
        } catch (MalformedURLException e) {
            Log.e("Reg Request URL", "Malformed URL Error");
        }
        Log.d("Reg Request URL", url);

        // Configure POST params
        final String fcm_token = MyFirebaseInstanceIDService.token;
        Map<String, String> params = new HashMap<String, String>();
        params.put(getString(R.string.param_name_fcm_token), fcm_token);

        // Configure callback
        final TextView textView = (TextView) findViewById(R.id.text_view_groupid);
        Response.Listener<String> rl = new Response.Listener<String>() {
            @Override
            public void onResponse(String new_gid) {
                Log.d("Reg Request Response", new_gid);
                textView.setText(new_gid);
                group_id = parseInt(new_gid);
            }
        };

        // Send request
        sendPostRequest(url, params, rl);
    }

    private void requestJoin(final int gid) {
        // Configure URL
        String protocol = getString(R.string.app_server_protocol);
        String host = getString(R.string.app_server_host);
        int port = this.getResources().getInteger(R.integer.app_server_port);
        String path = getString(R.string.app_server_path_join_group);
        String url = "";
        try {
            url = new URL(protocol, host, port, path).toString();
        } catch (MalformedURLException e) {
            Log.e("Join Request URL", "Malformed URL Error");
        }
        Log.d("Join Request URL", url);

        // Configure POST params
        final String fcm_token = MyFirebaseInstanceIDService.token;
        final String gid_str = Integer.toString(gid);
        Map<String, String> params = new HashMap<String, String>();
        params.put(getString(R.string.param_name_fcm_token), fcm_token);
        params.put(getString(R.string.param_name_groupid), gid_str);

        // Configure callback
        final TextView textView = (TextView) findViewById(R.id.text_view_groupid);
        Response.Listener<String> rl = new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d("Join Request Response: ", response);
                textView.setText(gid_str);
                group_id = gid;
            }
        };

        // Send request
        sendPostRequest(url, params, rl);
    }

     private void requestAlert() {
        // Configure URL
        String protocol = getString(R.string.app_server_protocol);
        String host = getString(R.string.app_server_host);
        int port = this.getResources().getInteger(R.integer.app_server_port);
        String path = getString(R.string.app_server_path_alert);
        String url = "";
        try {
            url = new URL(protocol, host, port, path).toString();
        } catch (MalformedURLException e) {
            Log.e("Alert Request URL", "Malformed URL Error");
        }
        Log.d("Alert Request URL", url);

        // Configure POST params
        final String loc = Integer.toString(determineLocation());
        final String gid_str = Integer.toString(group_id);
        Map<String, String> params = new HashMap<String, String>();
        params.put(getString(R.string.param_name_location), loc);
        params.put(getString(R.string.param_name_groupid), gid_str);

        // Configure callback
        final TextView textView = (TextView) findViewById(R.id.text_view_status);
        Response.Listener<String> rl = new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d("Alert Response: ", response);
                textView.setText("Group alerted!");
            }
        };

        // Send request
        sendPostRequest(url, params, rl);
    }

    void sendPostRequest(final String url, final Map<String, String> params, Response.Listener<String> rl) {
        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest sr = new StringRequest(Request.Method.POST, url, rl, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError err) {
                Log.e("Reg Request Error", err.toString());
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                return params;
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String,String> params = new HashMap<String, String>();
                params.put("Content-Type","application/x-www-form-urlencoded");
                return params;
            }
        };

        queue.add(sr);
    }

    int determineLocation() {
        // TODO
        return 555;
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    // yisha start
    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d("DeviceLog" , "BroadcastReceiver mGattUpdateReceiver");
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                clearUI();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                displayData(intent.getStringExtra(mBluetoothLeService.EXTRA_DATA));
            }
        }
    };

    private void clearUI() {
        mDataField.setText(R.string.no_data);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeService.connect(mDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectionState.setText(resourceId);
            }
        });
    }

    private void displayData(String data) {

        if (data != null) {
            mDataField.setText(data);
        }
    }


    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();


        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(
                    LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));

            // If the service exists for HM 10 Serial, say so.
            if(SampleGattAttributes.lookup(uuid, unknownServiceString) == "HM 10 Serial") { isSerial.setText("Yes, serial :-)"); } else {  isSerial.setText("No, serial :-("); }
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            // get characteristic when UUID matches RX/TX UUID
            characteristicTX = gattService.getCharacteristic(BluetoothLeService.UUID_HM_RX_TX);
            characteristicRX = gattService.getCharacteristic(BluetoothLeService.UUID_HM_RX_TX);

        }

//        if(mConnected) {
//            mBluetoothLeService.setCharacteristicNotification(characteristicRX, true);
//        }

    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    private void changeLED(String state) {
        String str = state;
        Log.d(TAG, "Sending result=" + str);
        final byte[] tx = str.getBytes();
        if(mConnected) {
            characteristicTX.setValue(tx);
            mBluetoothLeService.writeCharacteristic(characteristicTX);
            mBluetoothLeService.setCharacteristicNotification(characteristicRX, true);
        }
    }
    // yisha end
}
