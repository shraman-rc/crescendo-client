package com.example.firstapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import static java.lang.Integer.parseInt;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    //public final static String EXTRA_MESSAGE = "com.example.myfirstapp.MESSAGE";
    public final static String GROUPID_EXTRA = "com.example.myfirstapp.GROUPID";
    public static int group_id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Display firebase token
        Log.d(TAG, "Firebase token: " + MyFirebaseInstanceIDService.token);
    }

    /** Called when the user clicks the Send button */
//    public void sendMessage(View view) {
//        Intent intent = new Intent(this, DisplayMessageActivity.class);
//        EditText editText = (EditText) findViewById(R.id.edit_message);
//        String message = editText.getText().toString();
//        intent.putExtra(EXTRA_MESSAGE, message);
//        startActivity(intent);
//    }

    /** Called when the user clicks the Register Group button */
    public void createGroup(View view) {
        Log.d("Main: ", "Firing RegisterGroupActivity");
        Intent intent = new Intent(this, RegisterGroupActivity.class);
        startActivity(intent);
    }

    /** Called when the user clicks the Join Group button */
    public void joinGroup(View view) {
        Log.d("Main: ", "Firing JoinGroupActivity");
        Intent intent = new Intent(this, JoinGroupActivity.class);
        EditText editText = (EditText) findViewById(R.id.edit_groupid);
        int gid = parseInt(editText.getText().toString());
        intent.putExtra(GROUPID_EXTRA, gid);
        startActivity(intent);
    }

    /** Called when the user clicks the Register Group button */
    public void alert(View view) {
        Log.d("Main: ", "Firing AlertActivity");
        Intent intent = new Intent(this, AlertActivity.class);
        startActivity(intent);
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
