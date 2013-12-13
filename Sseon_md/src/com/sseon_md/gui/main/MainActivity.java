package com.sseon_md.gui.main;

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
import android.bluetooth.BluetoothServerSocket;
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

import com.sseon_md.R;
import com.sseon_md.util.SseonFinal;

public class MainActivity extends Activity {

	Button connect;
	Button resetDrink;
	Button resetCup;
	TextView btstatus;
	TextView divStatus;
	boolean isConnect = false; // view

	// Handler mainHandler;

	SQLiteDatabase db; // db

	//private ConnectThread conThread = null;
	private AcceptThread accThread = null;
	//private ConnectedThread cTread = null;
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
		divStatus = (TextView) findViewById(R.id.set_cupstatus);
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
				Toast.makeText(this, "블루투스 사용을 요청 ", Toast.LENGTH_SHORT).show();
				Intent enableIntent = new Intent(
						BluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
			}
		} else if (!SseonFinal.isDeviceConnect) { // 블루투스는 켜져있으나 컵받침은 연결 안되어 있다
			Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(intent, REQUEST_CONNECT_DEVICE);
		} else if (SseonFinal.onBTConnect) { // 블루투스가 켜져있다
			((AcceptThread) SseonFinal.connThread).cancel();
			mBtAdapter.disable(); // 블루투스가 연결되어있으면 끔
			isConnect = false;
			SseonFinal.onBTConnect = false;

			Toast.makeText(getApplicationContext(), "연결종료", Toast.LENGTH_SHORT)
					.show();
			connect.setText("연결");
			btstatus.setText("> 블루투스 연결상태 : 꺼짐");
			divStatus.setText("> 관리자 연결상태 : 연결안됨");

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

				accThread = new AcceptThread();
				accThread.start();
				Toast.makeText(this, "관리자 연결 가능 모드", Toast.LENGTH_SHORT)
				.show();
				divStatus.setText("> 관리자 연결상태 : 연결가능");
				Log.d("블루투스", "연결시도 : 관리자 연결시도//소켓오픈");
			} else {
				Log.d("블루투스", "실패 : 블루투스 활성화 실패");
				Toast.makeText(this, "블루투스를 활성화하지 못했습니다.", Toast.LENGTH_SHORT)
						.show();
				isConnect = false;
				SseonFinal.onBTConnect = false;
			}
			break;
		case REQUEST_CONNECT_DEVICE:
			if (resultCode == Activity.RESULT_OK) {
				// When DeviceListActivity returns with a device to connect
				accThread = new AcceptThread();
				accThread.start();
				Toast.makeText(this, "관리자 연결 가능 모드", Toast.LENGTH_SHORT)
				.show();
				divStatus.setText("> 관리자 연결상태 : 연결가능");
				Log.d("블루투스", "연결시도 : 관리자 연결시도//소켓오픈");
				break;
			}
		}
	}
/*
	Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 0: // 장비 연결성공
				Toast.makeText(getApplicationContext(), "연결시작",
						Toast.LENGTH_SHORT).show();
				connect.setText("연결해제");
				btstatus.setText("> 블루투스 연결상태 : 켜짐");
				divStatus.setText("> 관리자 연결상태 : 연결됨");
				break;
			case 1: // 장비를 찾을 수 없어 연결실패. 블루투스는 켜진채로 냅둠
				Toast.makeText(getApplicationContext(), "해당 블루투스를 찾을 수 없습니다",
						Toast.LENGTH_SHORT).show();
				btstatus.setText("> 블루투스 연결상태 : 켜짐");
				divStatus.setText("> 관리자 연결상태 : 연결안됨");
				break;
			}
		}
	};
	*/
	private class AcceptThread extends Thread {
	    private final BluetoothServerSocket mmServerSocket;
	    
	    public AcceptThread() {
	        // Use a temporary object that is later assigned to mmServerSocket,
	        // because mmServerSocket is final
	        BluetoothServerSocket tmp = null;
	        try {
	            // MY_UUID is the app's UUID string, also used by the client code
	            tmp = mBtAdapter.listenUsingRfcommWithServiceRecord("Sseon", MY_UUID);
	        } catch (IOException e) { }
	        mmServerSocket = tmp;
	    }
	    public void run() {
	        BluetoothSocket socket = null;
	        // Keep listening until exception occurs or a socket is returned
	        while (true) {
	            try {
	                socket = mmServerSocket.accept();
	            } catch (IOException e) {
	                break;
	            }
	            // If a connection was accepted
	            if (socket != null) {
	                // Do work to manage the connection (in a separate thread)
	                //manageConnectedSocket(socket);
	                try {
						mmServerSocket.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
	                break;
	            }
	        }
	    }
	    /** Will cancel the listening socket, and cause the thread to finish */
	    public void cancel() {
	        try {
	            mmServerSocket.close();
	        } catch (IOException e) { }
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