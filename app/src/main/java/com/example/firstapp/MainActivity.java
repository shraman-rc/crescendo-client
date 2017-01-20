package com.example.firstapp;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Integer.parseInt;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static int mgid;
    private static String mloc;
    private static long mlast_loc_update_time;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // TODO Block until location actually updated
        update_location();

        // Display firebase token
        Log.d(TAG, "Firebase token: " + MyFirebaseInstanceIDService.token);
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
//			List<ScanResult> tmp_scan_result = wifiManager.getScanResults();

            for (ScanResult scanResult : wifiManager.getScanResults()) {
                JSONObject ap = new JSONObject();
                ap.put("BSSID", scanResult.BSSID);
                ap.put("SSID", scanResult.SSID);
                ap.put("frequency", scanResult.frequency);
                ap.put("level", scanResult.level);
                //netwrok.put("timestamp", String.valueOf(scanResult.timestamp));
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

//            Log.d("mainlog", "Result: " + result);
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
                            mloc = response.get(loc_key).toString();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        textView.setText(mloc);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        VolleyLog.d("mainlog", "Error: " + error.getMessage());
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
}
