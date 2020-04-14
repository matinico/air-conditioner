package com.matias.airconditioner;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.matias.airconditioner.data.Device;

import java.util.List;

/**
 * Created by Matias on 17/07/2017.
 */
public class DeviceListAdapter extends ArrayAdapter<Device> {
    private LayoutInflater flater;

    public DeviceListAdapter(@NonNull Context context, @LayoutRes int resource, @NonNull List<Device> objects) {
        super(context, resource, objects);
        flater = LayoutInflater.from(context);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if(convertView == null) convertView = flater.inflate(R.layout.item_spinner, parent, false);
        Device device = getItem(position);
        ((TextView) convertView.findViewById(R.id.name)).setText(device.name);
        ((TextView) convertView.findViewById(R.id.status)).setText(device.connection_status);
        return convertView;
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if(convertView == null) convertView = flater.inflate(R.layout.list_item, parent, false);
        Device device = getItem(position);
        ((TextView) convertView.findViewById(R.id.name)).setText(device.name);
        ((TextView) convertView.findViewById(R.id.major_device_class))
                .setText(String.valueOf(device.temp) + "°C" + "  ");
        ((TextView) convertView.findViewById(R.id.mac)).setText(device.mac);
        return convertView;
    }

    public void updateConnectionStatus(int position, String connection_status){
        Device device = getItem(position);
        device.connection_status = connection_status;
        notifyDataSetChanged();
    }

    public void updateTemp(int position, int temp){
        Device device = getItem(position);
        device.temp = temp;
        notifyDataSetChanged();
    }

    public void updateConnectionStatus(List<String> connection_status){
        for(int i = 0; i < getCount(); i++) {
            Device device = getItem(i);
            device.connection_status = connection_status.get(i);
        }
        notifyDataSetChanged();
    }
}