package com.sseon.gui.main;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.sseon.R;
import com.sseon.util.Sseon;

public class MainActivity extends Activity implements View.OnClickListener, Handler.Callback {
	private static final int REQUEST_CONNECT_DEVICE = 1;
	private static final int REQUEST_ENABLE_BT = 3;
	
	private static final int MSG_START_MANAGING = 0;
	private static final int MSG_CONNECT_ERROR = 1;
	private static final int MSG_CONNECTED = 2;
	private static final int MSG_DISCONNECTED = 3;
	
	private RelativeLayout emptyListLayout;
	private Button btEnableButton;
	private TextView btStatusText;
	
	private RelativeLayout mainListLayout;
	private Button addButton;
	private Button removeButton;
	private ListView managedList;
//	private TextView btStatusText;
//	private TextView connectingStatusText;
//	private boolean isConnect = false; // view

	private Handler mHandler;

	private SQLiteDatabase db; // db

	private ConnectThread connectThread;
//	private ManageThread manageThread;
	private ArrayList<ManageThread> threadList;
	
	private BluetoothAdapter mBtAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mHandler = new Handler(this);
		threadList = new ArrayList<ManageThread>();
		initView();
		
		mBtAdapter = BluetoothAdapter.getDefaultAdapter();
		updateBluetoothStatus();
	}

	public void onResume() {
		super.onResume();
	
		mBtAdapter = BluetoothAdapter.getDefaultAdapter();
		updateBluetoothStatus();
	}

	private void initView() {
		setContentView(R.layout.activity_main);
		
		emptyListLayout = (RelativeLayout) findViewById(R.id.manage_empty_layout);
		btEnableButton = (Button) findViewById(R.id.bt_enable);
		btEnableButton.setOnClickListener(this);
		btStatusText = (TextView) findViewById(R.id.bt_status);
		
		mainListLayout = (RelativeLayout) findViewById(R.id.manage_main_layout);
		addButton = (Button) findViewById(R.id.managed_add);
		addButton.setOnClickListener(this);
		removeButton = (Button) findViewById(R.id.managed_remove);
		managedList = (ListView) findViewById(R.id.managed_list);
		managedList.setEmptyView(findViewById(R.id.empty_managed_list));
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.bt_enable:
			// 블투 쓰게 해주세요
			startActivityForResult(
					new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),
					REQUEST_ENABLE_BT);
			break;
			
		case R.id.managed_add:
			// 장치 목록 액티비티 보여주기
//			startActivityForResult(
//					new Intent(this, DeviceListActivity.class),
//					REQUEST_CONNECT_DEVICE);
			// TODO 땜빵
			
			break;
			
		case R.id.managed_remove:
			// TODO 연결된 거 끊는 액티비티 보여주기
			break;
		}
	}
	
	private void updateBluetoothStatus() {
		if (mBtAdapter != null && mBtAdapter.isEnabled()) {
			// 사용 가능함
			emptyListLayout.setVisibility(View.GONE);
			mainListLayout.setVisibility(View.VISIBLE);
		} else {
			mainListLayout.setVisibility(View.GONE);
			emptyListLayout.setVisibility(View.VISIBLE);
			
			if (mBtAdapter == null) {
				// 장치가 아예 없음...
				btStatusText.setTextColor(Color.RED);
				btStatusText.setText("블루투스 장치가 없습니다.");
				btEnableButton.setEnabled(false);
			} else {
				// 블루투스가 꺼져있음
				btStatusText.setTextColor(Color.BLACK);
				btStatusText.setText("블루투스가 꺼져있습니다.");
				btEnableButton.setEnabled(true);
			}
		}
	}
	
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQUEST_ENABLE_BT:
			updateBluetoothStatus();
			if (resultCode != Activity.RESULT_OK) {
				Log.d("블루투스", "실패 : 블루투스 활성화 실패");
				Toast.makeText(this, "블루투스를 켜야 합니다.", Toast.LENGTH_SHORT).show();
			}
			break;
			
		case REQUEST_CONNECT_DEVICE:
			Log.d("블루투스", "연결시도 : 관리 대상 연결시도");
			if (resultCode == Activity.RESULT_OK) {
				// 장치 골랐을 때
				String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
				BluetoothDevice device = mBtAdapter.getRemoteDevice(address);

				BluetoothSocket sock = null;
				try {
					sock = device.createRfcommSocketToServiceRecord(Sseon.SerialPortServiceClass_UUID);
				} catch (IOException e) {
					e.printStackTrace();
					return;
				}

				connectThread = new ConnectThread(sock, mHandler);
				connectThread.start();
			}
			break;
		}
	}

	@Override
	public boolean handleMessage(Message msg) {
		ManageThread manageThread;
		
		switch (msg.what) {
		case MSG_START_MANAGING: // 컵받침 연결성공
			Toast.makeText(getApplicationContext(), "연결시작",
					Toast.LENGTH_SHORT).show();
			updateBluetoothStatus();
			break;
			
		case MSG_CONNECT_ERROR:	// 장치 연결 실패
			Toast.makeText(this, "해당 장치에 연결할 수 없습니다.", Toast.LENGTH_SHORT).show();
			updateBluetoothStatus();
			break;
			
		case MSG_CONNECTED:		// 됐어 가
			connectThread = null;
			
			// 피관리자 추가
			manageThread = new ManageThread((BluetoothSocket) msg.obj, mHandler);
			threadList.add(manageThread);
			manageThread.start();
			break;
			
		case MSG_DISCONNECTED:	// 끊김
			manageThread = (ManageThread) msg.obj;
			threadList.remove(manageThread);
			// TODO 끊겼네요; 알려주셈
			Toast.makeText(this, "끊김ㅡㅡ", Toast.LENGTH_LONG).show();
		}
		return true;
	}
	
	private static class ConfigurableListAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return 0;
		}

		@Override
		public Object getItem(int arg0) {
			return null;
		}

		@Override
		public long getItemId(int arg0) {
			return 0;
		}

		@Override
		public View getView(int arg0, View arg1, ViewGroup arg2) {
			return null;
		}
		
	}

	private static class ConnectThread extends Thread {
		private BluetoothSocket mSocket;
		private Handler mHandler;

		public ConnectThread(BluetoothSocket sock, Handler h) {
			this.mSocket = sock;
			this.mHandler = h;
		}

		public void run() {
			Log.d("블루투스", "연결시도 : 소켓접속시도");

			try {
				mSocket.connect();
			} catch (IOException e) {
				mHandler.sendEmptyMessage(MSG_CONNECT_ERROR);
				// mBtAdapter.disable();
//				Sseon.isDeviceConnect = false;
//				Sseon.onBTConnect = false;
				Log.d("블루투스", "실패 : 소켓을 못열었다");
				e.printStackTrace();
				return;
			}
			
			// 연결 성공함; 관리해주셈
			Message msg = new Message();
			msg.what = MSG_CONNECTED;
			msg.obj = mSocket;
			mHandler.sendMessage(msg);
		}
	}

	private class ManageThread extends Thread {
		private final BluetoothSocket mSocket;
		private final InputStream mInStream;
		private final OutputStream mOutStream;
		private Handler mHandler;

		public ManageThread(BluetoothSocket sock, Handler h) {
			this.mSocket = sock;
			this.mHandler = h;
			
			InputStream tmpIn = null;
			try { tmpIn = sock.getInputStream(); } catch (IOException e) {}
			
			OutputStream tmpOut = null;
			try { tmpOut = sock.getOutputStream(); } catch (IOException e) {}

			this.mInStream = tmpIn;
			this.mOutStream = tmpOut;
		}

		public void run() {
			byte[] buffer = new byte[256];
			int readBytes;

			Log.d("블루투스", "연결성공");
			mHandler.sendEmptyMessage(MSG_START_MANAGING);
//			Sseon.isDeviceConnect = true;
//			Sseon.connThread = this;

			// Keep listening to the InputStream while connected
			try {
				while (true) {
					readBytes = mInStream.read(buffer);
//					if (readBytes == 1
//							&& (buffer[0] == (byte) 0xFA || buffer[0] == (byte) 0xFB))
//						ManageBTMessange(buffer[0]);

				}
			} catch (IOException e) {
			}

			Message msg = new Message();
			msg.what = MSG_DISCONNECTED;
			msg.obj = this;
			mHandler.sendMessage(msg);
			
			Log.d("블루투스", "읽기쓰기 스레드 종료");
		}

//		private void ManageBTMessange(byte code) {
//			int weight;
//			byte[] buffer = new byte[256];
//			int bytes = 0;
//			String rfid = null;
//
//			try {
//				if (code == (byte) 0xFA) {
//					bytes = mInStream.read(buffer);
//					rfid = byteArrayToHex(buffer, bytes);
//					// Log.d("블루투스", "RFID : " + rfid);
//
//					Intent cupintent = new Intent();
//					cupintent.setAction("com.howcup.cuprfid");
//					cupintent.putExtra("rfid", rfid);
//					sendBroadcast(cupintent);
//				} else if (code == (byte) 0xFB) {
//					bytes = mInStream.read(buffer);
//					weight = Integer
//							.parseInt(byteArrayToHex(buffer, bytes), 16);
//					// Log.d("블루투스", "CUP : " + weight);
//					// mainHandler.sendEmptyMessage(0);
//
//					Intent drinkintent = new Intent();
//					drinkintent.setAction("com.howcup.cupweight");
//					drinkintent.putExtra("weight", weight);
//					sendBroadcast(drinkintent);
//				}
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		}
//
//		/**
//		 * Write to the connected OutStream.
//		 * 
//		 * @param buffer
//		 *            The bytes to write
//		 */
//		public void write(byte[] buffer) {
//			try {
//				mOutStream.write(buffer);
//			} catch (IOException e) {
//			}
//		}

		public void cancel() {
			try {
//				Sseon.isDeviceConnect = false;
//				Sseon.connThread = null;
				mInStream.close();
				mOutStream.close();
				mSocket.close();
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