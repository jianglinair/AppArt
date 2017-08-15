package com.lin.jiang.appart.aidl;

import android.os.RemoteException;

public class ComputeStub extends ICompute.Stub {

    @Override
    public int add(int a, int b) throws RemoteException {
        return a + b;
    }

}
