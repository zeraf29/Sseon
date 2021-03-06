/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sseon.gui.main;

import java.util.Set;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import com.sseon.R;

/**
 * This Activity appears as a dialog. It lists any paired devices and devices
 * detected in the area after discovery. When a device is chosen by the user,
 * the MAC address of the device is sent back to the parent Activity in the
 * result Intent.
 */
public class DeviceListActivity extends Activity {
	// Debugging
	private static final String TAG = "DeviceListActivity";
	private static final boolean D = true;

	// Return Intent extra
	public static final String EXTRA_DEVICE_NAME = "device_name";
	public static final String EXTRA_DEVICE_ADDRESS = "device_address";

	// Member fields
	private BluetoothAdapter mBtAdapter;
	private ArrayAdapter<NameAndAddress> mPairedDevicesArrayAdapter;
	private ArrayAdapter<NameAndAddress> mNewDevicesArrayAdapter;
	
	private ListView pairedListView;
	private ListView newDevicesListView;
	
	private static class NameAndAddress {
		String name;
		String address;
		
		public NameAndAddress(String name, String address) {
			this.name = name;
			this.address = address;
		}
		
		public String toString() {
			return (address != null ? name + "\n" + address : name);
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Setup the window
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.device_list);

		// Set result CANCELED in case the user backs out
		setResult(Activity.RESULT_CANCELED);

		// Initialize the button to perform device discovery
		Button scanButton = (Button) findViewById(R.id.button_scan);
		scanButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				doDiscovery();
				v.setVisibility(View.GONE);
			}
		});

		// Initialize array adapters. One for already paired devices and
		// one for newly discovered devices
		mPairedDevicesArrayAdapter = new ArrayAdapter<NameAndAddress>(this,
				R.layout.device_name);
		mNewDevicesArrayAdapter = new ArrayAdapter<NameAndAddress>(this,
				R.layout.device_name);

		// Find and set up the ListView for paired devices
		pairedListView = (ListView) findViewById(R.id.paired_devices);
		pairedListView.setAdapter(mPairedDevicesArrayAdapter);
		pairedListView.setOnItemClickListener(mDeviceClickListener);

		// Find and set up the ListView for newly discovered devices
		newDevicesListView = (ListView) findViewById(R.id.new_devices);
		newDevicesListView.setAdapter(mNewDevicesArrayAdapter);
		newDevicesListView.setOnItemClickListener(mDeviceClickListener);

		// Register for broadcasts when a device is discovered
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		this.registerReceiver(mReceiver, filter);

		// Register for broadcasts when discovery has finished
		filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		this.registerReceiver(mReceiver, filter);

		// Get the local Bluetooth adapter
		mBtAdapter = BluetoothAdapter.getDefaultAdapter();

		// Get a set of currently paired devices
		Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

		// If there are paired devices, add each one to the ArrayAdapter
		if (!pairedDevices.isEmpty()) {
			findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
			for (BluetoothDevice device : pairedDevices) {
				mPairedDevicesArrayAdapter.add(new NameAndAddress(
						device.getName(), device.getAddress()));
			}
		} else {
			mPairedDevicesArrayAdapter.add(
					new NameAndAddress("(연결된 장치 없음)", null));
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		// Make sure we're not doing discovery anymore
		if (mBtAdapter != null) {
			mBtAdapter.cancelDiscovery();
		}

		// Unregister broadcast listeners
		this.unregisterReceiver(mReceiver);
	}

	/**
	 * Start device discover with the BluetoothAdapter
	 */
	private void doDiscovery() {
		if (D)
			Log.d(TAG, "doDiscovery()");

		// Indicate scanning in the title
		setProgressBarIndeterminateVisibility(true);
		setTitle("장치를 검색합니다...");

		// Turn on sub-title for new devices
		findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);

		// If we're already discovering, stop it
		if (mBtAdapter.isDiscovering()) {
			mBtAdapter.cancelDiscovery();
		}

		// Request discover from BluetoothAdapter
		mBtAdapter.startDiscovery();
	}

	// The on-click listener for all devices in the ListViews
	private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
		public void onItemClick(AdapterView<?> av, View v, int position, long arg3) {
			// Cancel discovery because it's costly and we're about to connect
			mBtAdapter.cancelDiscovery();

			NameAndAddress name = null;
			if (newDevicesListView == av)
				name = mNewDevicesArrayAdapter.getItem(position);
			else
				name = mPairedDevicesArrayAdapter.getItem(position);
			if (D) Log.d(TAG, "picked: " + name.name + ", " + name.address);
			
			if (name.address != null) {
				// 선택한 장치의 MAC주소를 넘겨줍니다.
				Intent intent = new Intent();
				intent.putExtra(EXTRA_DEVICE_NAME, name.name);
				intent.putExtra(EXTRA_DEVICE_ADDRESS, name.address);
				setResult(Activity.RESULT_OK, intent);
				finish();
			}
		}
	};

	// The BroadcastReceiver that listens for discovered devices and
	// changes the title when discovery is finished
	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				// 장치를 찾았을 때
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				// 이미 연결된 장치가 아닐 때만 추가함
				if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
					mNewDevicesArrayAdapter.add(new NameAndAddress(device.getName(), device.getAddress()));
				}
			} else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
				// 장치를 다 찾았을 때 (끝)
				setProgressBarIndeterminateVisibility(false);
				setTitle("연결할 장치 선택");
				
				// 찾은 장치가 없으면 없다고 해줌
				if (mNewDevicesArrayAdapter.getCount() == 0) {
					mNewDevicesArrayAdapter.add(new NameAndAddress("(장치 없음)", null));
				}
			}
		}
	};

}