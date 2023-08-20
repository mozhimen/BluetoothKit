package com.mozhimen.bluetoothk.temps;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mozhimen.basick.stackk.monitor.StackMonitor;
import com.mozhimen.bluetoothk.BluetoothK;
import com.mozhimen.bluetoothk.R;

import java.util.ArrayList;
import java.util.Set;

@SuppressLint("MissingPermission")
public class BluetoothKChooseActivity extends AppCompatActivity {

    private Context mContext;
    private BluetoothAdapter mBluetoothAdapter;
    private final int REQUEST_ENABLE_BT = 0x01;
    private RecyclerView mRecyclerView;
    private BluetoothKDevicesAdapter mAdapter;
    private AppCompatTextView mTvRefresh;
    private ArrayList<String> mPairedDeviceList = new ArrayList<>();
    private ArrayList<String> mFoundDeviceList = new ArrayList<>();//0:开始搜索
    private int state = 0;
    private String mKey = "";

    // Create a BroadcastReceiver for ACTION_FOUND
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String s = device.getName() + "\n" + device.getAddress();
                if (!mFoundDeviceList.contains(s)&&!TextUtils.isEmpty(device.getName())) {
                    // Add the name and address to an array adapter to show in a ListView
                    mFoundDeviceList.add(device.getName() + "\n" + device.getAddress());
                    Log.e("TAG", "onReceive: "+device.getName() + "\n" + device.getAddress());
                    mAdapter.notifyDataSetChanged();
                }
            } else if (BluetoothK.INTENT_ACTION_BLUETOOTH_ADAPTER_CANCEL_DISCOVERY.equals(action)) {
                mBluetoothAdapter.cancelDiscovery();
                mTvRefresh.setText("重新搜索");
                state = 2;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_bluetooth);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {

            getSupportActionBar().setTitle(R.string.choose_bluetooth_activity_title);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        mContext = this;

        StackMonitor.getInstance().pushActivity(this);

        mKey = getIntent().getStringExtra("callback_key");
        // Register the BroadcastReceiver
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothK.INTENT_ACTION_BLUETOOTH_ADAPTER_CANCEL_DISCOVERY);
        registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "获取蓝牙适配器失败", Toast.LENGTH_SHORT).show();
            StackMonitor.getInstance().popAllActivity();
        }
        checkBluetoothIsEnable();
        initData();
        initView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
    }

    private Boolean checkBluetoothIsEnable() {
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableBtIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            Toast.makeText(this, "蓝牙未打开", Toast.LENGTH_SHORT).show();
            return false;
        } else {
            return true;
        }
    }

    private void initData() {
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        // If there are paired devices
        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                // Add the name and address to an array adapter to show in a ListView
                String s = device.getName() + "\n" + device.getAddress();
                if (!mPairedDeviceList.contains(s)) {
                    mPairedDeviceList.add(device.getName() + "\n" + device.getAddress());
                    Log.e("TAG", "onReceive1: "+device.getName() + "\n" + device.getAddress());
                }
            }
        }
    }

    private void initView() {

        mRecyclerView = (RecyclerView) findViewById(R.id.rv_bluetooth_device_list);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new BluetoothKDevicesAdapter(mContext,mPairedDeviceList, mFoundDeviceList, mBluetoothAdapter, mKey);
        mRecyclerView.setAdapter(mAdapter);


        mTvRefresh = (AppCompatTextView) findViewById(R.id.tv_refresh);
        mTvRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(!checkBluetoothIsEnable()) {
                    return;
                };

                switch (state) {
                    case 0:
                        mBluetoothAdapter.startDiscovery();
                        Toast.makeText(mContext, "开始搜索", Toast.LENGTH_SHORT).show();
                        mTvRefresh.setText("停止");
                        state = 1;
                        break;
                    case 1:
                        mBluetoothAdapter.cancelDiscovery();
                        Toast.makeText(mContext, "停止搜索", Toast.LENGTH_SHORT).show();
                        mTvRefresh.setText("重新搜索");
                        state = 2;
                        break;
                    case 2:
                        mFoundDeviceList.clear();
                        mAdapter.notifyDataSetChanged();
                        mBluetoothAdapter.startDiscovery();
                        Toast.makeText(mContext, "开始搜索", Toast.LENGTH_SHORT).show();
                        mTvRefresh.setText("停止");
                        state = 1;
                        break;
                }
                mBluetoothAdapter.startDiscovery();
            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "蓝牙打开失败", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "蓝牙打开成功", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            StackMonitor.getInstance().popAllActivity();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


}
