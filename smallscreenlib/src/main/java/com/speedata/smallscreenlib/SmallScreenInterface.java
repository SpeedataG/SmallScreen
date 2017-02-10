package com.speedata.smallscreenlib;

import android.os.RemoteException;

/**
 * Created by brxu on 2016/12/26.
 */

public interface SmallScreenInterface {
    public void clearScreen() throws RemoteException;

    public void writeClockBuffer(int hour, int min) throws RemoteException;

    public void writeGb2312Buffer(byte[] gbbuffer, int gblen) throws RemoteException;

    public void writeAsciiBuffer(byte[] ascii, int asciilen) throws RemoteException;

    public void startClock() throws RemoteException;

    public void stopClock() throws RemoteException;

    public boolean isCirculation();

    public void stopCirculation();

    public void writeString(String data, boolean isCirculation, int period,boolean isWake) throws RemoteException;
}
