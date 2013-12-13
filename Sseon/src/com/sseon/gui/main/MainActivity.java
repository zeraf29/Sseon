package com.sseon.gui.main;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;
import java.util.UUID;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.sseon.R;
import com.sseon.util.SseonFinal;

public class MainActivity extends Activity {

	Button connect;
	Button resetDrink;
	Button resetCup;
	TextView btstatus;
	TextView cupstatus;
	boolean isConnect = false; // view

	// Handler mainHandler;

	SQLiteDatabase db; // db

	private ConnectThread conThread = null;
	private ConnectedThread cTread = null;
	private static final UUID MY_UUID = UUID
			.fromString("00001101-0000-1000-8000-00805f9b34fb");
	private static final int REQUEST_CONNECT_DEVICE = 1;
	private static final int REQUEST_ENABLE_BT = 3;
	private BluetoothAdapter mBtAdapter = null; // bluetooth

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		initView();
		// mainHandler = MainActivity.getHandler();
	}

	private void initView() {
		// TODO Auto-generated method stub
		connect = (Button) findViewById(R.id.set_connect);
		connect.setOnClickListener(listener);
		btstatus = (TextView) findViewById(R.id.set_btstatus);
		cupstatus = (TextView) findViewById(R.id.set_cupstatus);
		
	}

	public void onResume() {
		super.onResume();

		mBtAdapter = BluetoothAdapter.getDefaultAdapter();
		if (mBtAdapter != null && mBtAdapter.isEnabled()) {
			SseonFinal.onBTConnect = true;
			connect.setText("연결");
			btstatus.setText("> 블루투스 연결상태 : 켜짐");
		}
	}

	Button.OnClickListener listener = new Button.OnClickListener() {

		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.set_connect:
				manageBluetooth(); 
				break;
			}
		}
	};

	public void manageBluetooth() {
		
		if(mBtAdapter.isEnabled()){
			SseonFinal.onBTConnect = true;
		}else{
			btstatus.setText("> 블루투스 연결상태 : 꺼짐");
			SseonFinal.onBTConnect = false;
		}
		
		if (!SseonFinal.onBTConnect) { // 블루투스가 꺼져있다
			mBtAdapter = BluetoothAdapter.getDefaultAdapter();
			if (mBtAdapter == null) {
				Toast.makeText(this, "블루투스를 사용할 수 없습니다.", Toast.LENGTH_SHORT)
						.show();
				SseonFinal.onBTConnect = false;
				return;
			}

			// 블루투스 활성화 요청
			if (!mBtAdapter.isEnabled()) {
				Log.d("블루투스", "연결 요청");
				Toast.makeText(this, "블루투스 사용을 요청 ", Toast.LENGTH_SHORT).show();
				Intent enableIntent = new Intent(
						BluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
			}
		} else if (!SseonFinal.isDeviceConnect) { // 블루투스는 켜져있으나 컵받침은 연결 안되어 있다
			Intent intent = new Intent(this, DeviceListActivity.class);
			startActivityForResult(intent, REQUEST_CONNECT_DEVICE);
		} else if (SseonFinal.onBTConnect) { // 블루투스가 켜져있다
			((ConnectedThread) SseonFinal.connThread).cancel();
			mBtAdapter.disable(); // 블루투스가 연결되어있으면 끔
			isConnect = false;
			SseonFinal.onBTConnect = false;

			Toast.makeText(getApplicationContext(), "연결종료", Toast.LENGTH_SHORT)
					.show();
			connect.setText("연결");
			btstatus.setText("> 블루투스 연결상태 : 꺼짐");
			cupstatus.setText("> 관리 대상 연결상태 : 연결안됨");

		}
	}

	BluetoothSocket mSocket = null;

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQUEST_ENABLE_BT:
			if (resultCode == Activity.RESULT_OK) {
				Log.d("블루투스", "성공 : 블루투스 활성화");
				Toast.makeText(this, "블루투스를 활성화하였습니다.", Toast.LENGTH_SHORT)
						.show();
				isConnect = true;
				SseonFinal.onBTConnect = true;
				btstatus.setText("> 블루투스 연결상태 : 켜짐");

				Intent intent = new Intent(this, DeviceListActivity.class);
				startActivityForResult(intent, REQUEST_CONNECT_DEVICE);
			} else {
				Log.d("블루투스", "실패 : 블루투스 활성화 실패");
				Toast.makeText(this, "블루투스를 활성화하지 못했습니다.", Toast.LENGTH_SHORT)
						.show();
				isConnect = false;
				SseonFinal.onBTConnect = false;
			}
			break;
		case REQUEST_CONNECT_DEVICE:
			Log.d("블루투스", "연결시도 : 관리 대상 연결시도");
			// When DeviceListActivity returns with a device to connect
			if (resultCode == Activity.RESULT_OK) {
				String address = data.getExtras().getString(
						DeviceListActivity.EXTRA_DEVICE_ADDRESS);
				// Get the BluetoothDevice object
				BluetoothDevice device = mBtAdapter.getRemoteDevice(address);

				try {
					mSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
				} catch (IOException e) {
					e.printStackTrace();
					return;
				}

				conThread = new ConnectThread(mSocket, mHandler);
				conThread.start();
			}
			break;
		}
	}

	Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 0: // 컵받침 연결성공
				Toast.makeText(getApplicationContext(), "연결시작",
						Toast.LENGTH_SHORT).show();
				connect.setText("연결해제");
				btstatus.setText("> 블루투스 연결상태 : 켜짐");
				cupstatus.setText("> 관리 대상 연결상태 : 연결됨");
				break;
			case 1: // 컵받침을 찾을 수 없어 연결실패. 블루투스는 켜진채로 냅둠
				Toast.makeText(getApplicationContext(), "해당 블루투스를 찾을 수 없습니다",
						Toast.LENGTH_SHORT).show();
				btstatus.setText("> 블루투스 연결상태 : 켜짐");
				cupstatus.setText("> 관리 대상 연결상태 : 연결안됨");
				break;
			}
		}
	};

	private class ConnectThread extends Thread {
		BluetoothSocket msoc = null;
		Handler mhand;

		public ConnectThread(BluetoothSocket _soc, Handler hand) {
			msoc = _soc;
			mhand = hand;
		}

		public void run() {
			Log.d("블루투스", "연결시도 : 소켓접속시도");
			mBtAdapter.cancelDiscovery();

			try {
				msoc.connect();
			} catch (IOException e) {
				mhand.sendEmptyMessage(1);
				// mBtAdapter.disable();
				SseonFinal.isDeviceConnect = false;
				SseonFinal.onBTConnect = false;
				Log.d("블루투스", "실패 : 소켓을 못열었다");
				e.printStackTrace();
				return;
			}
			cTread = new ConnectedThread(msoc, mHandler);
			cTread.start();

		}
	}

	private class ConnectedThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final InputStream mmInStream;
		private final OutputStream mmOutStream;
		private Handler mhand;

		public ConnectedThread(BluetoothSocket socket, Handler hand) {
			mmSocket = socket;
			InputStream tmpIn = null;
			OutputStream tmpOut = null;
			mhand = hand;

			// Get the BluetoothSocket input and output streams
			try {
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
			} catch (IOException e) {
			}

			mmInStream = tmpIn;
			mmOutStream = tmpOut;
		}

		public void run() {
			byte[] buffer = new byte[256];
			int bytes;

			Log.d("블루투스", "연결성공");
			mhand.sendEmptyMessage(0);
			SseonFinal.isDeviceConnect = true;
			SseonFinal.connThread = this;

			// Keep listening to the InputStream while connected
			while (SseonFinal.isDeviceConnect) {
				try { // Read from the InputStream
					bytes = mmInStream.read(buffer);

					// Log.d("블루투스", "메세지1 : " + byteArrayToHex(buffer, bytes));
					// String readMessage = new String(buffer, 0, bytes);

					// try { Thread.sleep(1000); } catch (InterruptedException
					// e) {e.printStackTrace();} // for deley

					
					
					
					
				} catch (IOException e) {
					break;
				}
			}
			Log.d("블루투스", "읽기쓰기 스레드 종료");
		}

		/**
		 * Write to the connected OutStream.
		 * 
		 * @param buffer
		 *            The bytes to write
		 */
		public void write(byte[] buffer) {
			try {
				mmOutStream.write(buffer);

			} catch (IOException e) {
			}
		}

		public void cancel() {
			try {
				SseonFinal.isDeviceConnect = false;
				SseonFinal.connThread = null;
				mmInStream.close();
				mmOutStream.close();
				mmSocket.close();
			} catch (IOException e) {
			}
		}
	}

	public static String byteArrayToHex(byte[] ba, int size) {
		if (ba == null || size == 0) {
			return null;
		}

		StringBuffer sb = new StringBuffer(size * 2);
		String hexNumber;
		for (int x = 0; x < size; x++) {
			hexNumber = "0" + Integer.toHexString(0xff & ba[x]);

			sb.append(hexNumber.substring(hexNumber.length() - 2));
		}
		return sb.toString();
	}
}