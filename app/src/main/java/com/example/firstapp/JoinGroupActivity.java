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

public class JoinGroupActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_group);

        Intent intent = getIntent();
        int gid = intent.getIntExtra(MainActivity.GROUPID_EXTRA, R.integer.groupid_nonexistent);

        TextView textView = (TextView) findViewById(R.id.text_view_join_group);
        textView.setText("Attempting to join group " + Integer.toString(gid));
        Log.d("Join Group: ","Attempting to join group " + Integer.toString(gid));

        if (gid != R.integer.groupid_nonexistent) {
            requestJoin(gid);
        } else {
            Log.e("Intent Error:", "No group ID in intent?");
        }
    }

    private void requestJoin(final int gid) {
        final String gid_str = Integer.toString(gid);
        final String fcm_token = MyFirebaseInstanceIDService.token;

        String protocol = getString(R.string.app_server_protocol);
        String host = getString(R.string.app_server_host);
        int port = this.getResources().getInteger(R.integer.app_server_port);
        String path = getString(R.string.app_server_path_join_group);
        String url = "";
        try {
            url = new URL(protocol, host, port, path).toString();
        } catch (MalformedURLException e) {
            Log.e("Reg Request URL", "Malformed URL Error");
        }
        Log.d("Join Request URL", url);

        final TextView textView = (TextView) findViewById(R.id.text_view_join_group);

        RequestQueue queue = Volley.newRequestQueue(this);
        StringRequest sr = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d("Join Request Response: ", response);
                textView.setText("Successfully joined group " + gid_str);
                MainActivity.group_id = gid;
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError err) {
                Log.e("Join Request Error: ", err.toString());
                textView.setText("Failed to join group " + gid_str);
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put(getString(R.string.param_name_groupid), gid_str);
                params.put(getString(R.string.param_name_fcm_token), fcm_token);

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
