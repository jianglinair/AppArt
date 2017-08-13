package com.lin.jiang.appart.aidl;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class BookManagerService extends Service {

    private static final String TAG = "TEST:BookManagerService";

    private CopyOnWriteArrayList<Book> mBookList = new CopyOnWriteArrayList<>();

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: ");
        mBookList.add(new Book(1, "Android"));
        mBookList.add(new Book(2, "Python"));
    }

    private Binder mBinder = new IBookManager.Stub() {
        /**
         * Demonstrates some basic types that you can use as parameters
         * and return values in AIDL.
         */
        @Override
        public List<Book> getBookList() throws RemoteException {
            return mBookList;
        }

        @Override
        public void addBook(Book book) throws RemoteException {
            mBookList.add(book);
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
}
