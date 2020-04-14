package com.matias.airconditioner.ui;

import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.matias.airconditioner.BLEConnectionService;
import com.matias.airconditioner.BTBaseActivity;
import com.matias.airconditioner.Constants;
import com.matias.airconditioner.DeviceListAdapter;
import com.matias.airconditioner.R;
import com.matias.airconditioner.data.Device;
import com.matias.airconditioner.provider.DeviceContentProvider;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Matias on 16/07/2017.
 */
public class SetActivity extends BTBaseActivity {
    private static int temp;

    private Button accept;
    private ImageButton button_on, connection_state, button_temp_up, button_temp_down, button_delete, button_add;
    private TextView text_title, text_current_temp, text_set_temp;
    private Spinner spinner;
    private Toolbar toolbar;

    private ServiceConnection serviceConnection = null;
    private BLEConnectionService service;

    private ContentResolver contentResolver;
    private Cursor cursor;
    private List<Device> devices;
    private String mac;

    private FirebaseRemoteConfig mFirebaseRemoteConfig;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_set);

        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        mFirebaseRemoteConfig.setDefaults(R.xml.remote_config_defaults);
        mFirebaseRemoteConfig.fetch(0)
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            // Once the config is successfully fetched it must be activated before newly fetched
                            // values are returned.
                            mFirebaseRemoteConfig.activateFetched();
                        } else {
                            if(Constants.IS_LOGGABLE)
                                Log.w("FirebaseRemoteConfig", "Error", task.getException());
                        }

                        if(Constants.IS_LOGGABLE)
                            Log.w("FirebaseRemoteConfig", mFirebaseRemoteConfig.getString("welcome_message"));

                        if(mFirebaseRemoteConfig.getBoolean("lock_app")) {
                            if(!PreferenceManager
                                    .getDefaultSharedPreferences(SetActivity.this)
                                    .getBoolean("is_app_unlocked", false)) {
                                final EditText text = new EditText(SetActivity.this);
                                new AlertDialog
                                        .Builder(SetActivity.this)
                                        .setView(text)
                                        .setPositiveButton(getString(R.string.accept), new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                String password = text.getText().toString();
                                                if(password.equals(mFirebaseRemoteConfig.getString("code_app"))) {
                                                    PreferenceManager
                                                            .getDefaultSharedPreferences(SetActivity.this)
                                                            .edit()
                                                            .putBoolean("is_app_unlocked", true)
                                                            .apply();
                                                }
                                                else {
                                                    System.exit(0);
                                                }
                                            }
                                        })
                                        .show();

                            }
                        }
                    }
                });

        contentResolver = getContentResolver();
        if(!getContentProviderInstance()) return;

        toolbar = (Toolbar) findViewById(R.id.toolbar);

        accept = (Button) findViewById(R.id.accept);

        button_on = (ImageButton) findViewById(R.id.button_on);
        connection_state = (ImageButton) findViewById(R.id.connection_state);
        button_temp_up = (ImageButton) findViewById(R.id.button_temp_up);
        button_temp_down = (ImageButton) findViewById(R.id.button_temp_down);
        button_add = (ImageButton) toolbar.findViewById(R.id.add_button);
        button_delete = (ImageButton) toolbar.findViewById(R.id.delete);

        text_title = (TextView) toolbar.findViewById(R.id.toolbar_title);
        text_current_temp = (TextView) findViewById(R.id.text_current_temp_label);
        text_set_temp = (TextView) findViewById(R.id.text_set_temp);

        spinner = (Spinner) toolbar.findViewById(R.id.select_ac);

        //accept.setEnabled(false);
        button_on.setEnabled(false);

        text_current_temp.setText(getString(R.string.unknown));

        updateSpinner();

        temp = devices.get(spinner.getSelectedItemPosition()).temp;

        updateTempInTextView();

        button_temp_up.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                temp = temp + 1;
                if(temp >= Constants.maxTemp) temp = Constants.maxTemp;
                updateTempInTextView();
            }
        });

        button_temp_down.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                temp = temp - 1;
                if(temp <= Constants.minTemp) temp = Constants.minTemp;
                updateTempInTextView();
            }
        });

        button_on.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    JSONObject object = new JSONObject();
                    object.put("type", "power");
                    object.put("data", "switch");
                    if(service != null) service.sendData(object.toString());
                }
                catch(JSONException e){
                    e.printStackTrace();
                }
            }
        });

        accept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ContentValues contentValues = new ContentValues();
                contentValues.put(DeviceContentProvider.temp, temp);
                contentResolver.update(DeviceContentProvider.CONTENT_URI, contentValues,
                        DeviceContentProvider.mac + "=?", new String[] {mac});
                sendTemp();
            }
        });

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String address = devices.get(i).mac;
                if(Constants.IS_LOGGABLE) Log.w("mac", address);
                mac = address;
                temp = devices.get(i).temp;
                updateTempInTextView();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        button_add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(SetActivity.this, ConnectActivity.class));
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                finish();
            }
        });

        button_delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AlertDialog.Builder(SetActivity.this)
                        .setTitle(getString(R.string.delete_title))
                        .setMessage(getString(R.string.delete_text))
                        .setPositiveButton(getString(R.string.delete), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                contentResolver.delete(DeviceContentProvider.CONTENT_URI,
                                        DeviceContentProvider.mac + " = '" + mac + "'", null);
                                getContentProviderInstance();
                                updateSpinner();
                                if(service != null) unbindService(serviceConnection);
                                dialogInterface.dismiss();
                            }
                        })
                        .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            }
        });

        final BLEConnectionService.Callback callback = new BLEConnectionService.Callback() {
            @Override
            public void onConnected() {
                connection_state.setBackgroundResource(R.mipmap.ic_bluetooth);
                sendTemp();
                //accept.setEnabled(true);
                button_on.setEnabled(true);
                setSpinnerStatus(getString(R.string.connected));
            }

            @Override
            public void onDisconnected() {
                connection_state.setBackgroundResource(R.mipmap.ic_bluetooth_off);
                setSpinnerStatus(getString(R.string.disconnected));
                if(service != null) unbindService(serviceConnection);
            }

            @Override
            public void onDataReceived(String s) {
                try {
                    JSONObject object = new JSONObject(s);

                    if(object.getString("type").equals("selected_temp")) {
                        temp = object.getInt("data");
                        updateTempInTextView();
                    }

                    else if(object.getString("type").equals("update")) {
                        text_current_temp.setText(String.valueOf(object.getDouble("data")) + "°C");
                    }

                    else if(object.getString("type").equals("power")) {
                        if(object.getInt("data") == 1) {
                            button_on.setBackgroundResource(R.mipmap.ic_power_on);
                        }
                        else if(object.getInt("data") == 0) {
                            button_on.setBackgroundResource(R.mipmap.ic_power_off);
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        };

        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                service = ((BLEConnectionService.BLEConnectionServiceBinder) iBinder).getService();
                service.setListener(callback);
                service.connect(mac);
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                //accept.setEnabled(false);
                button_on.setEnabled(false);
                connection_state.setBackgroundResource(R.mipmap.ic_bluetooth_off);
                setSpinnerStatus(getString(R.string.disconnected));
            }
        };

        final Intent intent = new Intent(this, BLEConnectionService.class);

        connection_state.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(service != null) {
                    setSpinnerStatus(getString(R.string.disconnected));
                    unbindService(serviceConnection);
                    connection_state.setBackgroundResource(R.mipmap.ic_bluetooth_off);
                }
                else {
                    setSpinnerStatus(getString(R.string.connecting));
                    bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
                    connection_state.setBackgroundResource(R.mipmap.ic_bluetooth_connecting);
                }
            }
        });

        setBluetoothImportantForThisActivity(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        animateChange();
    }

    @Override
    protected void onPause() {
        super.onPause();
        button_on.setEnabled(false);
        connection_state.setBackgroundResource(R.mipmap.ic_bluetooth_off);
        setSpinnerStatus(getString(R.string.disconnected));
        if(service != null) unbindService(serviceConnection);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(service != null) unbindService(serviceConnection);
    }

    @Override
    protected void onBluetoothOn() {

    }

    @Override
    protected void onBluetoothOff() {

    }

    @Override
    protected void onDeviceFounded(BluetoothDevice device) {

    }

    private boolean getContentProviderInstance(){
        devices = new ArrayList<>();
        String[] projection = new String[]{
                DeviceContentProvider.name,
                DeviceContentProvider.mac,
                DeviceContentProvider.temp
        };
        cursor = contentResolver.query(DeviceContentProvider.CONTENT_URI,
                projection, null, null, null);

        if(cursor == null){
            startActivity(new Intent(this, ConnectActivity.class));
            finish();
            return false;
        }

        if(!cursor.moveToFirst()){
            startActivity(new Intent(this, ConnectActivity.class));
            finish();
            return false;
        }

        do {
            String name = cursor.getString(cursor.getColumnIndex(DeviceContentProvider.name));
            String mac = cursor.getString(cursor.getColumnIndex(DeviceContentProvider.mac));
            String connection_status = getString(R.string.disconnected);
            int temp = cursor.getInt(cursor.getColumnIndex(DeviceContentProvider.temp));
            devices.add(new Device(name, mac, connection_status, temp));
        } while(cursor.moveToNext());

        return true;
    }

    private void sendTemp(){
        try {
            JSONObject object = new JSONObject();
            object.put("type", "temp");
            object.put("data", temp);
            if(Constants.IS_LOGGABLE) Log.w("SetActivity", object.toString());
            if(service != null) service.sendData(object.toString());
        }
        catch(JSONException e) {
            e.printStackTrace();
        }
    }

    private void updateSpinner(){
        DeviceListAdapter listAdapter = new DeviceListAdapter(this, R.layout.list_item, devices);
        spinner.setAdapter(listAdapter);
        //spinner.setDropDownWidth(500);
    }

    private void setSpinnerStatus(String status){
        if(spinner.getAdapter() != null) {
            DeviceListAdapter adapter = (DeviceListAdapter) spinner.getAdapter();
            List<String> stringList = new ArrayList<>();
            for(int i = 0; i < adapter.getCount(); i++){
                if(spinner.getSelectedItemPosition() == i)
                    stringList.add(status);
                else
                    stringList.add(getString(R.string.disconnected));
            }
            adapter.updateConnectionStatus(stringList);
        }
    }

    private void updateTempInTextView(){
        text_set_temp.setText(String.valueOf(temp) + "°C");
        ((DeviceListAdapter) spinner.getAdapter()).updateTemp(spinner.getSelectedItemPosition(), temp);
    }

    private void animateChange() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                text_title.setVisibility(View.GONE);
                spinner.setVisibility(View.VISIBLE);
                button_add.setVisibility(View.VISIBLE);
                button_delete.setVisibility(View.VISIBLE);
            }
        }, 800);
    }
}