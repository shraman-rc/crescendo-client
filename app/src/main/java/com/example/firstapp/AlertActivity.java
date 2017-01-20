package com.example.firstapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
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

public class AlertActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alert);

        Intent intent = getIntent();

        TextView textView = (TextView) findViewById(R.id.text_view_alert);
        textView.setText("Attempting to alert group!");
        Log.d("Alert: ", "Attempting to alert");

        requestAlert();
    }

    private void requestAlert() {
        //final String fcm_token = MyFirebaseInstanceIDService.token;
        final String loc = "555";
        final String gid_str = Integer.toString(MainActivity.group_id);

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

        final TextView textView = (TextView) findViewById(R.id.text_view_alert);

        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest sr = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d("Alert Response", gid_str);
                textView.setText("Group " + gid_str + " has been alerted!");
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError err) {
                Log.e("Alert Error", err.toString());
                textView.setText("Failed to alert group " + gid_str);
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put(getString(R.string.param_name_groupid), gid_str);
                params.put(getString(R.string.param_name_location), loc);

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
}
