package com.lin.jiang.appart;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.lin.jiang.appart.activity.BookManagerActivity;
import com.lin.jiang.appart.activity.MessengerActivity;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "TEST:MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView tv = (TextView) findViewById(R.id.text_view);
        tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, BookManagerActivity.class);
                startActivity(intent);
            }
        });

//        UserManager.sUserId = 2;
//        Log.d(TAG, "onCreate: sUserId = " + UserManager.sUserId);
    }
}
