package com.example.firstapp;

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
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Integer.parseInt;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    //public final static String EXTRA_MESSAGE = "com.example.myfirstapp.MESSAGE";
    public final static String GROUPID_EXTRA = "com.example.myfirstapp.GROUPID";
    private static int group_id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
}
