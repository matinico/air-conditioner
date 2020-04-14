package com.matias.airconditioner.ui;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.matias.airconditioner.BTBaseActivity;
import com.matias.airconditioner.Constants;
import com.matias.airconditioner.R;
import com.matias.airconditioner.data.BTDevice;
import com.matias.airconditioner.provider.DeviceContentProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ConnectActivity extends BTBaseActivity {
    private RecyclerView recyclerView;
    private Toolbar toolbar;

    private List<BTDevice> devices;
    private boolean initActivity;

    private BluetoothAdapter bluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final ContentResolver contentResolver = getContentResolver();
        String[] projection = new String[]{
                DeviceContentProvider.name,
                DeviceContentProvider.mac,
        };
        Cursor cursor = contentResolver.query(DeviceContentProvider.CONTENT_URI,
                projection, null, null, null);

        initActivity = cursor != null && cursor.moveToFirst();
        if(initActivity) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        devices = new ArrayList<>();
        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);

        setBluetoothImportantForThisActivity(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                startActivity(new Intent(ConnectActivity.this, SetActivity.class));
                finish();
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if(initActivity){
            startActivity(new Intent(ConnectActivity.this, SetActivity.class));
            finish();
        }
    }

    @Override
    protected void onBluetoothOn() {
        bluetoothAdapter = getBluetoothAdapter();
        initRecyclerView();
    }

    @Override
    protected void onBluetoothOff() {

    }

    @Override
    protected void onDeviceFounded(BluetoothDevice device) {

    }

    private void initRecyclerView() {
        devices = new ArrayList<>();
        Set<BluetoothDevice> pairedDevice = bluetoothAdapter.getBondedDevices();
        if(pairedDevice.size() > 0){
            for(BluetoothDevice device : pairedDevice){
                if(!isAdded(device.getAddress())) {
                    BTDevice btDevice = new BTDevice();
                    btDevice.name = device.getName();
                    btDevice.address = device.getAddress();
                    btDevice.id = getBTMajorDeviceClass(device.getBluetoothClass()
                            .getMajorDeviceClass()) + "  ";
                    devices.add(btDevice);
                }
            }
        }

        RecyclerViewAdapter adapter = new RecyclerViewAdapter(devices, new Listener() {
            @Override
            public void onItemClick(BTDevice btDevice) {
                ContentResolver contentResolver = getContentResolver();

                ContentValues contentValues = new ContentValues();
                contentValues.put(DeviceContentProvider.name, btDevice.name);
                contentValues.put(DeviceContentProvider.mac, btDevice.address);
                contentValues.put(DeviceContentProvider.temp, Constants.temp);

                contentResolver.insert(DeviceContentProvider.CONTENT_URI, contentValues);

                startActivity(new Intent(ConnectActivity.this, SetActivity.class));
                finish();
            }
        });

        RecyclerView.LayoutManager mLayoutManager =
                new LinearLayoutManager(this);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setHasFixedSize(true);
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.HORIZONTAL));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(adapter);
    }

    private boolean isAdded(String mac){
        ContentResolver contentResolver = getContentResolver();

        String[] projection = new String[]{
                DeviceContentProvider.name,
                DeviceContentProvider.mac
        };
        Cursor cursor = contentResolver.query(DeviceContentProvider.CONTENT_URI,
                projection, null, null, null);
        while(cursor.moveToNext()){
            int colMac = cursor.getColumnIndex(DeviceContentProvider.mac);
            if(cursor.getString(colMac).equals(mac)){
                return true;
            }
        }
        return false;
    }

    private String getBTMajorDeviceClass(int major){
        switch(major){
            case BluetoothClass.Device.Major.AUDIO_VIDEO:
                return getString(R.string.audio_video);
            case BluetoothClass.Device.Major.COMPUTER:
                return getString(R.string.computer);
            case BluetoothClass.Device.Major.HEALTH:
                return getString(R.string.health);
            case BluetoothClass.Device.Major.IMAGING:
                return getString(R.string.imaging);
            case BluetoothClass.Device.Major.MISC:
                return getString(R.string.misc);
            case BluetoothClass.Device.Major.NETWORKING:
                return getString(R.string.networking);
            case BluetoothClass.Device.Major.PERIPHERAL:
                return getString(R.string.peripheral);
            case BluetoothClass.Device.Major.PHONE:
                return getString(R.string.phone);
            case BluetoothClass.Device.Major.TOY:
                return getString(R.string.toy);
            case BluetoothClass.Device.Major.UNCATEGORIZED:
                return getString(R.string.uncategorized);
            case BluetoothClass.Device.Major.WEARABLE:
                return getString(R.string.wearable);
            default: return getString(R.string.unknown);
        }
    }

    private class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.MyHolder> {
        private List<BTDevice> devices;
        private Listener listener;

        public RecyclerViewAdapter(List<BTDevice> devices, Listener listener){
            this.devices = devices;
            this.listener = listener;
        }

        @Override
        public MyHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.list_item, parent, false);
            return new MyHolder(itemView);
        }

        @Override
        public void onBindViewHolder(MyHolder holder, int position) {
            BTDevice btDevice = devices.get(position);
            holder.name.setText(btDevice.name);
            holder.mac.setText(btDevice.address);
            holder.major_device_class.setText(btDevice.id);
            holder.bind(btDevice, listener);
        }

        @Override
        public int getItemCount() {
            return devices != null ? devices.size() : 0;
        }

        public class MyHolder extends RecyclerView.ViewHolder {
            TextView name, mac, major_device_class;
            public MyHolder(View itemView) {
                super(itemView);
                name = (TextView) itemView.findViewById(R.id.name);
                mac = (TextView) itemView.findViewById(R.id.mac);
                major_device_class = (TextView) itemView.findViewById(R.id.major_device_class);
            }

            public void bind(final BTDevice btDevice, final Listener listener){
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        listener.onItemClick(btDevice);
                    }
                });
            }
        }

        public void addFoundedDevice(BTDevice device){
            devices.add(device);
            notifyDataSetChanged();
        }
    }

    private interface Listener {
        void onItemClick(BTDevice btDevice);
    }
}