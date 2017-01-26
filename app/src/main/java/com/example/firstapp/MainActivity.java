package com.example.firstapp;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
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
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.lang.Integer.parseInt;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int VOLLEY_TIMEOUT_MS = 10000;

    public static final String BLE_OUT_MOTOR = "M";
    public static final String BLE_OUT_LED = "1";
    public static final String BLE_OUT_NAV_UP = "U9";
    public static final String BLE_OUT_NAV_DOWN = "D9";
    public static final String BLE_OUT_NAV_FORWARD = "F";
    public static final String BLE_OUT_NAV_BACK = "B";
    public static final String BLE_OUT_NAV_LEFT = "L";
    public static final String BLE_OUT_NAV_RIGHT = "R";

    private static final String BLE_IN_FIRST_BUTTON = "0";
    private static final String BLE_IN_SECOND_BUTTON = "1";

    private static int mgid;
    private static String mloc;
    private static long mlast_loc_update_time;

    public static final String DEVICE_ADDRESS_PURPLE = "5C:F8:21:F9:91:56";
    public static final String DEVICE_ADDRESS_GRAY = "04:A3:16:08:B0:B5";
    public static final String mDeviceName = "HMSoft";
    public static final String mDeviceAddress = DEVICE_ADDRESS_GRAY;

    private static TextView mConnectionState;
    private static TextView mDataField;
    private static BluetoothLeService mBluetoothLeService;
    private static boolean mConnected = false;
    private static BluetoothGattCharacteristic characteristicTX;
    private static BluetoothGattCharacteristic characteristicRX;

    public final static UUID HM_RX_TX =
            UUID.fromString(SampleGattAttributes.HM_RX_TX);

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // TODO Block until location actually updated
        update_location();

        // Display firebase token
        Log.d(TAG, "Firebase token: " + MyFirebaseInstanceIDService.token);

        // Sets up UI references.
        mConnectionState = (TextView) findViewById(R.id.text_view_status);
        mDataField = (TextView) findViewById(R.id.text_view_data);

        final Button buttonOn = (Button) findViewById(R.id.OnLED);
        buttonOn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.d("Bluetooth Service", "button click");
                //changeLED(BLE_OUT_MOTOR);
                changeLED(BLE_OUT_NAV_FORWARD);
            }
        });


        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    /** Called when the user clicks the Register Group button */
    public void createGroup(View view) {
        requestRegister();
    }

    /** Called when the user clicks the Join Group button */
    public void joinGroup(View view) {
        EditText editText = (EditText) findViewById(R.id.edit_groupid);
        int gid = parseInt(editText.getText().toString());
        requestJoin(gid);
    }

    /** Called when the user clicks the Alert button */
    public void alert(View view) {
        requestAlert();
    }

    private void requestRegister() {
        // Configure URL
        String url = buildURL(
                R.string.app_server_protocol,
                R.string.app_server_host,
                R.integer.app_server_port,
                R.string.app_server_path_reg_group);

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
                mgid = parseInt(new_gid);
            }
        };

        // Send request
        sendPostRequest(url, params, rl);
    }

    private void requestJoin(final int gid) {
        // Configure URL
        String url = buildURL(
                R.string.app_server_protocol,
                R.string.app_server_host,
                R.integer.app_server_port,
                R.string.app_server_path_join_group);

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
                mgid = gid;
            }
        };

        // Send request
        sendPostRequest(url, params, rl);
    }

     private void requestAlert() {
        // Configure URL
         String url = buildURL(
                 R.string.app_server_protocol,
                 R.string.app_server_host,
                 R.integer.app_server_port,
                 R.string.app_server_path_alert);

        // Configure POST params
        final String gid_str = Integer.toString(mgid);
        Map<String, String> params = new HashMap<String, String>();
        params.put(getString(R.string.param_name_location), mloc);
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

    /*******************/
    /**** WEB STUFF ****/
    /*******************/

    String buildURL(int res_id_protocol, int res_id_host, int res_id_port, int res_id_path) {
        String protocol = getString(res_id_protocol);
        String host = getString(res_id_host);
        int port = this.getResources().getInteger(res_id_port);
        String path = getString(res_id_path);
        String url = "";
        try {
            url = new URL(protocol, host, port, path).toString();
        } catch (MalformedURLException e) {
            Log.e("buildURL Error", "Malformed URL Error");
        }
        Log.d("Built URL", url);
        return url;
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

        sr.setRetryPolicy(new DefaultRetryPolicy(
                VOLLEY_TIMEOUT_MS,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        queue.add(sr);
    }

    /**********************/
    /**** LOCALIZATION ****/
    /**********************/

     public void update_location() {
        JSONObject obj = startTracking();
        if(obj != null) {
            JSONObject fingerprint = getFingerprint(obj);
            sendData(fingerprint);
        } else {
            Log.d("Localization", "startTracking() returned null");
        }
    }

    private JSONObject startTracking() {
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        wifiManager.startScan();
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();

        JSONObject obj = new JSONObject();
        try {
            JSONObject activity = new JSONObject();
            activity.put("BSSID", wifiInfo.getBSSID());
            activity.put("HiddenSSID", wifiInfo.getHiddenSSID());
            activity.put("SSID", wifiInfo.getSSID());
            activity.put("MacAddress", wifiInfo.getMacAddress());
            activity.put("IpAddress", wifiInfo.getIpAddress());
            activity.put("NetworkId", wifiInfo.getNetworkId());
            activity.put("RSSI", wifiInfo.getRssi());
            activity.put("LinkSpeed", wifiInfo.getLinkSpeed());
            obj.put("activity", activity);

            JSONArray available = new JSONArray();

            for (ScanResult scanResult : wifiManager.getScanResults()) {
                JSONObject ap = new JSONObject();
                ap.put("BSSID", scanResult.BSSID);
                ap.put("SSID", scanResult.SSID);
                ap.put("frequency", scanResult.frequency);
                ap.put("level", scanResult.level);
                ap.put("capabilities", scanResult.capabilities);
                available.put(ap);
            }
            Log.d("wifid", "scan result\n");
            obj.put("available", available);

            return obj;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    private JSONObject getFingerprint(JSONObject obj) {
        // Create fingerprints for tracking Post request
        JSONObject result = new JSONObject();
        JSONArray network_data;
        JSONObject tmp_json;
        JSONArray result_fp = new JSONArray();

        // TODO Change
        String group = getString(R.string.loc_default_group);
        String username = getString(R.string.loc_default_user);

        try {
            network_data = obj.getJSONArray("available");
            for(int i = 0; i < network_data.length(); i++) {
                tmp_json = network_data.getJSONObject(i);

                JSONObject ap = new JSONObject();
                ap.put("mac", tmp_json.getString("BSSID"));
                ap.put("rssi", tmp_json.getInt("level"));

                result_fp.put(ap);
            }

            result.put("group", group);
            result.put("username", username);
            result.put("location", "tracking");
            result.put("time", (new Date()).getTime());
            result.put("wifi-fingerprint", result_fp);

            return result;

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    // TODO: Combine with sendPostRequest
    private void sendData(JSONObject obj) {
        String url = buildURL(
                R.string.loc_server_protocol,
                R.string.loc_server_host,
                R.integer.loc_server_port,
                R.string.loc_server_path_track);
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);

        Log.d("mainlog", "Track obj: " + obj);

        // Request a string response from the provided URL.
        final TextView textView = (TextView) findViewById(R.id.text_view_location);
        final String loc_key = getString(R.string.json_response_loc_key);
        JsonObjectRequest jsonObjReq = new JsonObjectRequest(
                Request.Method.POST, url, obj,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d("Localization", response.toString());
                        mlast_loc_update_time = (new Date()).getTime();
                        try {
                            mloc = response.getString(loc_key);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        textView.setText(mloc);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("Localization", "Error: " + error.getMessage());
                    }
                }) {
            // Passing some request headers
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Content-Type", "application/json; charset=utf-8");
                return headers;
            }
        };
        queue.add(jsonObjReq);
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
            Log.d("Bluetooth Service", "onServiceConnected connected!");
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
                Log.d("Bluetooth Service","onReceive ACTION_GATT_CONNECTED");
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                clearUI();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
                Log.d("Bluetooth Service","onReceive ACTION_GATT_DISCOVERED");
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                displayData(intent.getStringExtra(mBluetoothLeService.EXTRA_DATA));
                Log.d("Bluetooth Service","onReceive ACTION_DATA_AVAILABLE");
            } else {
                Log.d("Bluetooth Service","WAT?");
            }

        }
    };

    private void clearUI() {
        mDataField.setText(R.string.no_data);
        Log.d("Bluetooth Service","clearUI cleared UI!");
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
            requestAlert();
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
            //if(SampleGattAttributes.lookup(uuid, unknownServiceString) == "HM 10 Serial") {
            //    isSerial.setText("Yes, serial :-)");
            //} else {
            //    isSerial.setText("No, serial :-(");
            //}
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            // get characteristic when UUID matches RX/TX UUID
            characteristicTX = gattService.getCharacteristic(BluetoothLeService.UUID_HM_RX_TX);
            characteristicRX = gattService.getCharacteristic(BluetoothLeService.UUID_HM_RX_TX);
            Log.d("Bluetooth Service", "displayGattServices find RX/TX");
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    public static void changeLED(String state) {
        String str = state;
        Log.d("Bluetooth Service", "Sending result=" + str);
        final byte[] tx = str.getBytes();
        if(mConnected) {
            Log.d("Bluetooth Service", "changeLED mConnected");
            characteristicTX.setValue(tx);
            Log.d("Bluetooth Service", "changeLED set TX value");
            mBluetoothLeService.writeCharacteristic(characteristicTX);
            Log.d("Bluetooth Service", "changeLED write TX");
            mBluetoothLeService.setCharacteristicNotification(characteristicRX, true);
            Log.d("Bluetooth Service", "changeLED notify RX");
        }
    }
}
