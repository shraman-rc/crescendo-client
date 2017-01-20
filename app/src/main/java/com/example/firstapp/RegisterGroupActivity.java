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

import static java.lang.Integer.parseInt;

public class RegisterGroupActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_group);

        Intent intent = getIntent();

        TextView textView = (TextView) findViewById(R.id.text_view_reg_group);
        textView.setText("Attempting to register new group");
        Log.d("Reg Group: ", "Attempting to register new group");

        requestRegister();
    }

    private void requestRegister() {
        final String fcm_token = MyFirebaseInstanceIDService.token;

       // Uri.Builder builder = new Uri.Builder();
       // builder.scheme(getString(R.string.app_server_protocol))
       //     .authority(getString(R.string.app_server_auth))
       //     .appendPath(getString(R.string.app_server_path_reg_group));
       // String url = builder.build().toString();
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

        final TextView textView = (TextView) findViewById(R.id.text_view_reg_group);

        RequestQueue queue = Volley.newRequestQueue(this);
        //StringRequest sr = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
        //    @Override
        //    public void onResponse(String new_gid) {
        //        Log.d("Reg Request Response", new_gid);
        //        //mtextView.setText("This is your group number " + new_gid);
        //    }
        //}, new Response.ErrorListener() {
        //    @Override
        //    public void onErrorResponse(VolleyError err) {
        //        Log.e("Reg Request Error", err.toString());
        //        //mtextView.setText("Failed to register group ");
        //    }
        //});
        StringRequest sr = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String new_gid) {
                Log.d("Reg Request Response", new_gid);
                textView.setText("This is your group number: " + new_gid);
                MainActivity.group_id = parseInt(new_gid);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError err) {
                Log.e("Reg Request Error", err.toString());
                textView.setText("Failed to register group ");
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
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
