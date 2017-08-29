package com.lin.jiang.appart;

import android.app.Dialog;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

public class DemoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);

        initView();
    }

    private void initView() {
//        Dialog dialog = new Dialog(this.getApplicationContext());
        Thread t = new Thread();
        Dialog dialog = new Dialog(this);
        TextView textView = new TextView(this);
        textView.setText("this is toast!");
        dialog.setContentView(textView);
//        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ERROR);
        dialog.show();
    }
}
