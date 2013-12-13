package com.sseon.gui.main;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.sseon.R;
import com.sseon.gui.widget.ButtonListAdapter;
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
	private ButtonListAdapter<ManagedThread> managedListAdapter;

	private Handler mHandler;

	private SQLiteDatabase db; // db
	private MediaPlayer player;

//	private ConnectThread connectThread;
	private ArrayList<ManagedThread> threadList;
	
	private BluetoothAdapter mBtAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mHandler = new Handler(this);
		threadList = new ArrayList<ManagedThread>();
		initView();
		
		mBtAdapter = BluetoothAdapter.getDefaultAdapter();
		updateBluetoothStatus();
		
		player = MediaPlayer.create(this, R.raw.hong);
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
		removeButton.setOnClickListener(this);
		
		managedList = (ListView) findViewById(R.id.managed_list);
		managedList.setEmptyView(findViewById(R.id.empty_managed_list));
		managedListAdapter = new ButtonListAdapter<ManagedThread>(this, threadList);
		managedList.setAdapter(managedListAdapter);
		managedList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		managedListAdapter.setButtonLabel("끊기");
		managedListAdapter.setButtonClickListener(new AdapterView.OnItemClickListener() {
			public void onItemClick(AdapterView<?> unused, View v, int position,
					long id) {
				Toast.makeText(MainActivity.this, "누르지 마라 " + position, Toast.LENGTH_LONG).show();
			}
		});
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
			startActivityForResult(
					new Intent(this, DeviceListActivity.class),
					REQUEST_CONNECT_DEVICE);
			break;
			
		case R.id.managed_remove:
			boolean visible = managedListAdapter.getButtonVisible();
			removeButton.setTextColor(!visible ? 0xff22b14c : Color.BLACK);
			managedListAdapter.setButtonVisible(!visible);
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
	
	private void alertToUser() {
			// TODO 끊겼네요; 알려주셈
	//		Toast.makeText(this, "끊김ㅡㅡ", Toast.LENGTH_LONG).show();
			Toast.makeText(this, "경고!!\n피관리 대상과 연결이 끊겼습니다.",
					Toast.LENGTH_LONG).show();
			player.start();
			Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
			vib.vibrate(5000);
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
				String name = data.getStringExtra(DeviceListActivity.EXTRA_DEVICE_NAME);
				String address = data.getStringExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
				BluetoothDevice device = mBtAdapter.getRemoteDevice(address);

				BluetoothSocket sock = null;
				try {
					sock = device.createRfcommSocketToServiceRecord(Sseon.SerialPortServiceClass_UUID);
				} catch (IOException e) {
					e.printStackTrace();
					return;
				}

				// 스레드 따로 만들 필요가 없지 흠흠
//				connectThread = new ConnectThread(sock, mHandler);
//				connectThread.start();

				ManagedThread manageThread = new ManagedThread(name, sock, mHandler);
				threadList.add(manageThread);
				managedListAdapter.notifyDataSetChanged();
				
				manageThread.start();
			}
			break;
		}
	}

	@Override
	public boolean handleMessage(Message msg) {
		switch (msg.what) {
		case MSG_START_MANAGING: // 컵받침 연결성공
			Toast.makeText(getApplicationContext(), "연결시작",
					Toast.LENGTH_SHORT).show();
//			updateBluetoothStatus();
			break;
			
		case MSG_CONNECT_ERROR:	// 장치 연결 실패
			Toast.makeText(this, "해당 장치에 연결할 수 없습니다.", Toast.LENGTH_SHORT).show();
			threadList.remove(msg.obj);
			managedListAdapter.notifyDataSetChanged();
			alertToUser(); // TODO 테스트용
			break;
			
		case MSG_CONNECTED:		// 됐어 가
			managedListAdapter.notifyDataSetChanged();
			break;
			
		case MSG_DISCONNECTED:	// 끊김
			threadList.remove(msg.obj);
			managedListAdapter.notifyDataSetChanged();
			alertToUser();
		}
		return true;
	}
	
	private class ManagedThread extends Thread {
		private static final int STATE_CONNECTING = 0;
		private static final int STATE_CONNECTED = 1;
		private static final int STATE_ERROR = 2;
		
		private final String[] stateMessage = {
			"연결 중", "연결 됨", "오류"
		};
		
		private final BluetoothSocket mSocket;
		private InputStream mInStream;
		private OutputStream mOutStream;
		private Handler mHandler;
		
		private String mName;
		private int mState = STATE_CONNECTING;
		
		public ManagedThread(String name, BluetoothSocket sock, Handler h) {
			this.mName = name;
			this.mSocket = sock;
			this.mHandler = h;
		}
		
		public void run() {
			if (doConnect()) {
				doKeepAlive();
				sendMessage(MSG_DISCONNECTED);
				
				Log.d("블루투스", "읽기쓰기 스레드 종료");
			}
		}

		private void sendMessage(int what) {
			Message msg = new Message();
			msg.what = what;
			msg.obj = this;
			mHandler.sendMessage(msg);
		}
		
		private boolean doConnect() {
			Log.d("블루투스", "연결시도 : 소켓접속시도");

			try {
				mSocket.connect();
			} catch (IOException e) {
				Log.d("블루투스", "실패 : 소켓을 못열었다");
				e.printStackTrace();
				mState = STATE_ERROR;
				sendMessage(MSG_CONNECT_ERROR);
				return false;
			}
			
			InputStream tmpIn = null;
			try { tmpIn = mSocket.getInputStream(); } catch (IOException e) {}
			
			OutputStream tmpOut = null;
			try { tmpOut = mSocket.getOutputStream(); } catch (IOException e) {}
			
			this.mInStream = tmpIn;
			this.mOutStream = tmpOut;
			
			// 연결 성공함; 관리해주셈
			mState = STATE_CONNECTED;
			sendMessage(MSG_CONNECTED);
			return true;
		}

		private void doKeepAlive() {
			byte[] buffer = new byte[256];
			int readBytes;

			Log.d("블루투스", "연결성공");
			sendMessage(MSG_START_MANAGING);
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
		
		@Override
		public String toString() {
			return mName + " (" + stateMessage[mState] + ")";
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