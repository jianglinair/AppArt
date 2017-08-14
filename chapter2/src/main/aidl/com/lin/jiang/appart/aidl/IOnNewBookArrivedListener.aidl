// IOnNewBookArrivedListener.aidl
package com.lin.jiang.appart.aidl;

// Declare any non-default types here with import statements
import com.lin.jiang.appart.aidl.Book;
interface IOnNewBookArrivedListener {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
//    void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat,
//            double aDouble, String aString);
    void onNewBookArrived(in Book book);
}
