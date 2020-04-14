package com.matias.airconditioner;

import android.app.ActivityManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.matias.airconditioner.ui.SetActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * Created by Matias on 02/08/2016.
 */
public class BLEConnectionService extends Service {

    private Handler bluetoothIn;
    private final int handlerState = 0;
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private StringBuilder recDataString = new StringBuilder();
    private ConnectedThread mConnectedThread;
    private String mac;
    private NotificationCompat.Builder notification;
    private ChangeBluetoothListener bluetoothListener;
    private int idNotification = 1;
    private boolean aBoolean = true;
    private BLEConnectionServiceBinder mBinder = new BLEConnectionServiceBinder();
    private Callback callback;

    private String TAG = "BleConnectionService";

    @Override
    public void onCreate() {
        super.onCreate();
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(!mBluetoothAdapter.isEnabled()) {
            aBoolean = false;
            if(callback != null) callback.onDisconnected();
            return;
        }
        bluetoothIn = new Handler() {
            public void handleMessage(android.os.Message msg) {
                if (msg.what == handlerState) {
                    String readMessage = (String) msg.obj;
                    recDataString.append(readMessage);
                    int endOfLineIndex = recDataString.indexOf("~");
                    if (endOfLineIndex > 0) {
                        String dataInPrint = recDataString.substring(1, endOfLineIndex);
                        if (recDataString.charAt(0) == '#') {
                            if(Constants.IS_LOGGABLE) Log.e("Receive by bluetooth: ", dataInPrint);
                            if(callback != null) callback.onDataReceived(dataInPrint);
                        }
                        recDataString.delete(0, recDataString.length());
                        dataInPrint = " ";
                    }
                }
            }
        };

        bluetoothListener = new ChangeBluetoothListener();
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(bluetoothListener, filter);

        Intent intent = new Intent(this, SetActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        notification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.air_conditioner)
                .setContentIntent(pendingIntent)
                .setPriority(-2)
                .setContentText(getString(R.string.connecting))
                .setContentTitle(getString(R.string.app_name));

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(idNotification, notification.build());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(Constants.IS_LOGGABLE) Log.w(TAG, "on destroy");
        stopForeground(true);
        if(btSocket != null) {
            try {
                btSocket.close();
            } catch (IOException e2) {
                //insert code to deal with this
            }
        }
        if(aBoolean) {
            unregisterReceiver(bluetoothListener);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            manager.cancel(idNotification);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public static boolean isServiceRunning(Context context){
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> serviceList = activityManager.getRunningServices(Integer.MAX_VALUE);
        if(serviceList.size() > 0){
            for(int i = 0; i < serviceList.size(); i++){
                ActivityManager.RunningServiceInfo serviceInfo = serviceList.get(i);
                ComponentName serviceName = serviceInfo.service;
                if(serviceName.getClassName().equals("com.matias.airconditioner.service.BLEConnectionService")){
                    return true;
                }
            }
        }
        return false;
    }

    public void connect(String address){
        mac = address;
        if(Constants.IS_LOGGABLE) Log.d(TAG, mac);
        new AsyncTask<Void, Void, Object>(){
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                btAdapter = BluetoothAdapter.getDefaultAdapter();
            }

            @Override
            protected Object doInBackground(Void... params) {
                BluetoothDevice device = btAdapter.getRemoteDevice(mac);
                try {
                    btSocket = createBluetoothSocket(device);
                }
                catch (IOException e) {
                    return e;
                }
                try {
                    btSocket.connect();
                    notification.setContentText(getString(R.string.connected));
                    startForeground(idNotification, notification.build());
                }
                catch (IOException e) {
                    if(Constants.IS_LOGGABLE) Log.e(TAG, "Excepcion de conexion");
                    try {
                        btSocket.close();
                    }
                    catch (IOException e2) {
                    }
                    stopSelf();
                    return e;
                }
                return null;
            }

            @Override
            protected void onPostExecute(Object aVoid) {
                super.onPostExecute(aVoid);
                if(aVoid instanceof IOException){
                    if(callback != null) callback.onDisconnected();
                    return;
                }
                mConnectedThread = new ConnectedThread(btSocket);
                mConnectedThread.start();
                mConnectedThread.write("d");
            }
        }.execute();
    }

    public void setListener(Callback callback){
        this.callback = callback;
        if(mConnectedThread != null && btSocket != null)
            mConnectedThread.write("%20");
    }

    public void sendData(String s){
        if(mConnectedThread != null && btSocket != null)
            mConnectedThread.write(s);
    }

    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        //creation of the connect thread
        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                //Create I/O streams for connection
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }


        public void run() {
            byte[] buffer = new byte[256];
            int bytes;

            // Keep looping to listen for received messages
            while (true) {
                try {
                    bytes = mmInStream.read(buffer);
                    String readMessage = new String(buffer, 0, bytes);
                    bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }
        //write method
        public void write(String input) {
            byte[] msgBuffer = input.getBytes();
            try {
                mmOutStream.write(msgBuffer);

                if(Constants.IS_LOGGABLE) Log.e(TAG, input);

                /*if(input.equals("d")) {
                    handler.sendEmptyMessage(2);
                }*/

                handler.sendEmptyMessage(2);

            } catch (IOException e) {
                handler.sendEmptyMessage(1);
            }
        }
    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        return device.createRfcommSocketToServiceRecord(Constants.BTMODULEUUID);
    }

    private class ChangeBluetoothListener extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)){
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                switch (state){
                    case BluetoothAdapter.STATE_OFF:
                        handler.sendEmptyMessage(1);
                        break;

                    case BluetoothAdapter.STATE_TURNING_OFF:
                        handler.sendEmptyMessage(1);
                        break;
                }
            }
        }
    }

    public class BLEConnectionServiceBinder extends Binder {
        public BLEConnectionService getService() {
            return BLEConnectionService.this;
        }
    }

    public interface Callback {
        void onConnected();
        void onDisconnected();
        void onDataReceived(String s);
    }

    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch(msg.what) {
                case 1:
                    if (callback != null) callback.onDisconnected();
                    break;
                case 2:
                    if (callback != null) callback.onConnected();
                    break;
            }
        }
    };
}