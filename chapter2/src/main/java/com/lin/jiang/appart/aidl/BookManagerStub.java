package com.lin.jiang.appart.aidl;

import android.os.Parcel;
import android.os.RemoteException;

import com.lin.jiang.appart.aidl.Book;
import com.lin.jiang.appart.aidl.IBookManager;
import com.lin.jiang.appart.aidl.IOnNewBookArrivedListener;

import java.util.List;

/**
 * Created by jianglin on 17-8-14.
 */

public class BookManagerStub extends IBookManager.Stub {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    @Override
    public List<Book> getBookList() throws RemoteException {
        return null;
    }

    @Override
    public void addBook(Book book) throws RemoteException {

    }

    @Override
    public void registerListener(IOnNewBookArrivedListener listener) throws RemoteException {

    }

    @Override
    public void unregisterListener(IOnNewBookArrivedListener listener) throws RemoteException {

    }

    @Override
    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        return super.onTransact(code, data, reply, flags);
    }
}
