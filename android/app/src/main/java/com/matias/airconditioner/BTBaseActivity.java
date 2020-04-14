package com.matias.airconditioner;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by Matias Nicolas on 1/10/2017.
 */
public abstract class BTBaseActivity extends AppCompatActivity {
    private AlertDialog dialog;

    private BluetoothAdapter bluetoothAdapter;
    private int REQUEST_ENABLE_BT = 1;
    private boolean aBoolean = false;
    private boolean bluetoothImportantForThisActivity = false;

    private BroadcastReceiver bReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent){
            String action =  intent.getAction();
            if(action.equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                onDeviceFounded(device);
            }
            else if(action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                if(intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)
                        == BluetoothAdapter.STATE_OFF) {
                    // Bluetooth is disconnected, do handling here
                    showAlertBTDialog();
                    onBluetoothOff();
                }
                else if(intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)
                        == BluetoothAdapter.STATE_ON) {
                    if(dialog != null) dialog.dismiss();
                    onBluetoothOn();
                }
            }
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        int btStatus = isBluetoothActivated();
        if(btStatus == 0 && bluetoothImportantForThisActivity) {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.not_compatible))
                    .setMessage(getString(R.string.your_phone_does_not_support_bluetooth))
                    .setPositiveButton(getString(R.string.exit), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            System.exit(0);
                        }
                    })
                    .setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialogInterface) {
                            System.exit(0);
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
            return;
        }
        else if(btStatus == 1) {
            onBluetoothOn();
        }
        else if(btStatus == 2) {
            onBluetoothOff();
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(bReceiver, filter);
        aBoolean = true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_ENABLE_BT) {
            if(resultCode == Activity.RESULT_OK) {
                onBluetoothOn();
            }
            else {
                showAlertBTDialog();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(aBoolean) unregisterReceiver(bReceiver);
    }

    private void showAlertBTDialog() {
        dialog = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.required_action))
                .setMessage(getString(R.string.app_wont_work_bt_disabled))
                .setPositiveButton(getString(R.string.exit), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        System.exit(0);
                    }
                })
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        if(!bluetoothAdapter.isEnabled()) System.exit(0);
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    public void startDiscovery() {
        bluetoothAdapter.startDiscovery();
    }

    public int isBluetoothActivated() {
        if (bluetoothAdapter == null) {
            return 0;
        } else {
            if (bluetoothAdapter.isEnabled()) {
                return 1;
            }
            else {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                return 2;
            }
        }
    }

    public BluetoothAdapter getBluetoothAdapter() {
        return bluetoothAdapter;
    }

    public void setBluetoothImportantForThisActivity(boolean bluetoothImportantForThisActivity) {
        this.bluetoothImportantForThisActivity = bluetoothImportantForThisActivity;
    }

    protected abstract void onDeviceFounded(BluetoothDevice device);
    protected abstract void onBluetoothOff();
    protected abstract void onBluetoothOn();
}