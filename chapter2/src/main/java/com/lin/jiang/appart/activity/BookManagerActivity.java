package com.lin.jiang.appart.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.lin.jiang.appart.R;
import com.lin.jiang.appart.aidl.Book;
import com.lin.jiang.appart.aidl.BookManagerService;
import com.lin.jiang.appart.aidl.IBookManager;

import java.util.List;

public class BookManagerActivity extends AppCompatActivity {

    private static final String TAG = "TEST:BookManagerActivity";
    private ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            IBookManager bookManager = IBookManager.Stub.asInterface(iBinder);

            try {
                List<Book> list = bookManager.getBookList();
                Log.d(TAG, "onServiceConnected: query book list, list type: " + list.getClass().getCanonicalName());
                Log.d(TAG, "onServiceConnected: query book list: " + list.toString());
            } catch (RemoteException e) {
                e.printStackTrace();
            }

        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_manager);

        Intent intent = new Intent(this, BookManagerService.class);
        bindService(intent, conn, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        unbindService(conn);
        super.onDestroy();
    }
}
