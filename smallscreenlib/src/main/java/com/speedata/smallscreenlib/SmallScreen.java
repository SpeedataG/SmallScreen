package com.speedata.smallscreenlib;

import android.app.smallscreen.SmallScreenManager;
import android.content.Context;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;

import java.io.UnsupportedEncodingException;


public class SmallScreen implements SmallScreenInterface {
    private static SmallScreenManager smallScreenManager;
    private static SmallScreen smallScreen;


    private boolean isCHCyc = false;


    private String writeString;
    private int period;
    private static Context mContext;

    public static SmallScreen getInstance(Context context) {
        mContext = context;
        if (smallScreenManager == null) {
            smallScreenManager = SmallScreenManager.getInstance();
            smallScreen = new SmallScreen();
        }
        return smallScreen;
    }

    private SmallScreen() {
    }

    @Override
    public void clearScreen() throws RemoteException {
        smallScreenManager.clearScreen();
    }

    @Override
    public void writeClockBuffer(int hour, int min) throws RemoteException {
        smallScreenManager.writeClockBuffer(hour, min);
    }

    @Override
    public void writeGb2312Buffer(byte[] gbbuffer, int gblen) throws RemoteException {
        smallScreenManager.writeGb2312Buffer(gbbuffer, gblen);
    }

    @Override
    public void writeAsciiBuffer(byte[] ascii, int asciilen) throws RemoteException {
        smallScreenManager.writeAsciiBuffer(ascii, asciilen);
    }

    @Override
    public void startClock() throws RemoteException {
//        smallScreenManager.startClock();
        SystemProperties.set("persist.sys.smallscreen", "true");
    }

    @Override
    public void stopClock() throws RemoteException {
//        smallScreenManager.stopClock();
        SystemProperties.set("persist.sys.smallscreen", "false");
    }

    @Override
    public boolean isCirculation() {
        return isCHCyc;
    }

    @Override
    public void stopCirculation() {
        isCHCyc = false;
        writeThread.interrupt();
        releaseWakeLock();
    }

    @Override
    public void writeString(String data, boolean isCirculation, int period, boolean isWakeup)
            throws RemoteException {

        if (isWakeup) {
            acquireWakeLock();
        } else {
            releaseWakeLock();
        }
        this.writeString = CHToENBiaoDian(data);
        isCHCyc = isCirculation;
        this.period = period;
        if (period < 1000) {
            this.period = 1000;
        }
        if (writeThread != null) {
            writeThread.interrupt();
            writeThread = null;
        }
        writeThread = new writeThread();
        writeThread.start();
    }


    private String CHToENBiaoDian(String data) {
        String[] regs = {"！", "，", "。", "；", "!", ",", ".", ";"};
        for (int i = 0; i < regs.length / 2; i++) {
            data = data.replaceAll(regs[i], regs[i + regs.length / 2]);
        }
        return data;
    }

    private writeThread writeThread;

    private class writeThread extends Thread {
        @Override
        public void run() {
            while (!isInterrupted()) {
                try {
                    clearScreen();
                    int writecount = 0;
                    char[] ch = writeString.toCharArray();
                    for (char c : ch) {
                        String str = String.valueOf(c);
                        byte[] bytes = str.getBytes("gb2312");

                        if (CharUtils.isChinese(c)) {
                            if (!isInterrupted())
                                writeGb2312Buffer(bytes, bytes.length);
                        } else {
                            if (!isInterrupted())
                                writeAsciiBuffer(bytes, bytes.length);
                        }
                        writecount += bytes.length;
                        if (writecount >= 12 && writeString.getBytes().length > 12) {
                            writecount = 0;
                            SystemClock.sleep(period);
//                            clearScreen();
                        }
                    }
                    if (writeString.getBytes().length <= (isCHCyc?11:12) || !isCHCyc) {
                        interrupt();
                    }
                    SystemClock.sleep(period);
                } catch (UnsupportedEncodingException e) {
//                        e.printStackTrace();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // 申请电源锁，禁止休眠
    private PowerManager.WakeLock mWakeLock = null;

    private void acquireWakeLock() {
        if (null == mWakeLock) {
            PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, this
                    .getClass().getCanonicalName());
            if (null != mWakeLock) {
                mWakeLock.acquire();
            }
        }
    }

    // 释放设备电源锁
    private void releaseWakeLock() {
        if (null != mWakeLock) {
            mWakeLock.release();
            mWakeLock = null;
        }
    }
}
