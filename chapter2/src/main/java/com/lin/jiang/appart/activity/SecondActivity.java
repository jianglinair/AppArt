package com.lin.jiang.appart.activity;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.lin.jiang.appart.R;
import com.lin.jiang.appart.utils.UserManager;

public class SecondActivity extends AppCompatActivity {

    private static final String TAG = "TEST:SecondActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_second);

        TextView tv = (TextView) findViewById(R.id.text_view);
        tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SecondActivity.this, ThirdActivity.class);
                startActivity(intent);
            }
        });
        Log.d(TAG, "onCreate: sUserId = " + UserManager.sUserId);
    }
}
