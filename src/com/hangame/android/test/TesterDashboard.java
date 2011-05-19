package com.hangame.android.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;
import java.util.Enumeration;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

public class TesterDashboard extends Activity {
	
	private Button btnConnect;
	private Button btnDisconnect;
	private Button btnRefresh;
	private Button btnSend;
	private Button btnStop;
	private Button btnHttp;
	
	private TextView isAvailable;
	private TextView wifi3g;
	private TextView isConnected;
	private TextView ipAddr;
	private TextView socketIpAddr;
	private TextView txStat;
	private TextView rxStat;
		
	private Socket socketToServer = null;
	private String SERVER_IP;
	private int SERVER_PORT;
	
	// Objects for WAKE-LOCK
	private PowerManager pwrManager;
	private WakeLock wakeLock;
	
	// BroadcastReceiver for Network change events
	private BroadcastReceiver receiver;
	
	// Message Handler
	private Handler handler;
	private final static int WHAT_NETWORK_CHANGED = 0;
	private final static int WHAT_SEND_ECHO = 1;
	private final static int WHAT_RECEIVE_ECHO = 2;
	private final static int WHAT_SEND_FAIL = 3;
	
	// Objects for echo data transmission
	private int deviceID;
	private int packetIndex = 0;
	private boolean isSending = false;		// TRUE when a user presses StartSend, FALSE when a user presses StopSend.
	private Timer sendTimer;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        // WakeLock Initialization
        pwrManager = (PowerManager)getSystemService(Context.POWER_SERVICE);
        wakeLock = pwrManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, "NetworkTester");
                
        // Initialized Button Widgets and Add Listeners.
        btnConnect = (Button)findViewById(R.id.btnConnect);
        btnDisconnect = (Button)findViewById(R.id.btnDisconnect);
        btnRefresh = (Button)findViewById(R.id.btnRefresh);
        btnSend = (Button)findViewById(R.id.btnSend);
        btnStop = (Button)findViewById(R.id.btnStop);
        btnHttp = (Button)findViewById(R.id.btnHttp);
        
        wifi3g = (TextView)findViewById(R.id.wifi3g);
        isAvailable = (TextView)findViewById(R.id.isAvailable);
        isConnected = (TextView)findViewById(R.id.isConnected);
        ipAddr = (TextView)findViewById(R.id.ipAddr);
        socketIpAddr = (TextView)findViewById(R.id.socketIpAddr);
        txStat = (TextView)findViewById(R.id.txStat);
        rxStat = (TextView)findViewById(R.id.rxStat);
        
        // Obtain Server IP, Port, deviceID from strings.xml
        SERVER_IP = getResources().getString(R.string.server_ip);
        SERVER_PORT = Integer.parseInt(getResources().getString(R.string.server_port));
        deviceID = Integer.parseInt(getResources().getString(R.string.device_id));
        
        // Register Button Event Handlers
        btnConnect.setOnClickListener(new View.OnClickListener() {	
			@Override
			public void onClick(View v) {
				connect();
			}
		});
        btnConnect.setOnClickListener(new View.OnClickListener() {	
			@Override
			public void onClick(View v) {
				connect();
			}
		});
        btnConnect.setOnClickListener(new View.OnClickListener() {	
			@Override
			public void onClick(View v) {
				connect();
			}
		});
        btnConnect.setOnClickListener(new View.OnClickListener() {	
			@Override
			public void onClick(View v) {
				connect();
			}
		});
        btnDisconnect.setOnClickListener(new View.OnClickListener() {	
			@Override
			public void onClick(View v) {
				disconnect();
			}
		});
        btnRefresh.setOnClickListener(new View.OnClickListener() {	
			@Override
			public void onClick(View v) {
				refresh();
			}
		});
        btnSend.setOnClickListener(new View.OnClickListener() {	
			@Override
			public void onClick(View v) {
				send();
			}
		});
        btnStop.setOnClickListener(new View.OnClickListener() {	
			@Override
			public void onClick(View v) {
				stop();
			}
		});
        btnHttp.setOnClickListener(new View.OnClickListener() {	
			@Override
			public void onClick(View v) {
				http();
			}
		});
   
        // refresh screen
        refresh();
        
        // register network event handler
        handler = new Handler() {
        	@Override
        	public void handleMessage(Message msg) {
        		TesterDashboard.this.handleMessage(msg);
        	}
        };
    }
    
	protected void onResume() {
		super.onResume();
		wakeLock.acquire();
		
		// register receiver
		receiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				String action = intent.getAction();
				
				// In case the received event is related to network connectivity change,
				if ( action.equals(ConnectivityManager.CONNECTIVITY_ACTION) ) {
					ConnectivityManager conManager = 
						(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
					NetworkInfo netInfo = conManager.getActiveNetworkInfo();
					if ( null == netInfo ) {
						Toast.makeText(context, "No Active Network", Toast.LENGTH_SHORT).show();
					} else {
						String netType = netInfo.getTypeName();
				    	if ( netType.equals("MOBILE") || netType.equals("mobile") ) {
				    		netType = "3G";
				    	}
						Toast.makeText(context,"Active Network : " + netType, Toast.LENGTH_SHORT).show();
					}
					
					// send a UI Message to the UI Framework
					handler.sendMessage(handler.obtainMessage(WHAT_NETWORK_CHANGED));
				}
			}
		};	// end-of-implementation of BroadcastReceiver
		
		this.registerReceiver(receiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
	}

	protected void onPause() {
		super.onPause();
		if (wakeLock.isHeld()) {
			wakeLock.release();
		}
		
		this.unregisterReceiver(receiver);
	}
    
    private void connect() {
    	
    	if ( null != socketToServer && socketToServer.isConnected() ) {
    		Toast.makeText(getApplicationContext(), "Already Connected", Toast.LENGTH_SHORT).show();
    		return;
    	}
    	
    	try {
    		// Connect to echo server
    		socketToServer = new Socket(SERVER_IP, SERVER_PORT);
    		
    		// state initialization
    		packetIndex = 0;
    		isSending = false;
    		
    	} catch (Exception e) {
    		Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
    	} finally {
    		refresh();
    	}
    }
    
    private void disconnect() {
    	if ( null == socketToServer || socketToServer.isClosed() ) {
			Toast.makeText(getApplicationContext(), "Already Disconnected", Toast.LENGTH_SHORT).show();
			return;
		}
    	
    	try {
			socketToServer.close();
			socketToServer = null;
			
		} catch (IOException e) {
			Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
		} finally {
			refresh();
		}
    }
    
    private void refresh() {
    	ConnectivityManager conMgr = (ConnectivityManager)getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
    	NetworkInfo netInfo = conMgr.getActiveNetworkInfo();
    	
    	if ( null == netInfo ) {
    		Toast.makeText(getApplicationContext(), "Network Down (NetworInfo NULL)", Toast.LENGTH_SHORT).show();
    		return;
    	}
    	
    	// Display Network Type - 3G or WiFi
    	String netType = netInfo.getTypeName();
    	if ( netType.equals("MOBILE") || netType.equals("mobile") ) {
    		netType = "3G";
    	}
    	wifi3g.setText(netType);
    	
    	// Display Network Availability and Connectivity
    	if ( netInfo.isAvailable() ) {
    		isAvailable.setText("Available/" + netInfo.getState().name());
    	} else {
    		isAvailable.setText("Unavailable");
    	}
    	
    	// Display TCP Connection Status
    	if ( null != socketToServer ) {
    		if ( socketToServer.isConnected() ) {
    			isConnected.setText("TCP Connected");
    		} else if ( socketToServer.isClosed() ) {
    			isConnected.setText("TCP Disconnected");
    		} else {
    			isConnected.setText("TCP Unknown");
    		}
    	} else {
    		isConnected.setText("TCP Disconnected");
    	}	 
    	
    	// Get Local IP Address and Display on the screen
    	String ipAddress = getLocalIpAddress();
    	if ( null != ipAddress ) {
    		ipAddr.setText(ipAddress);
    	} else {
    		ipAddr.setText("Invalid IP");
    	}
    	
    	// Display TCP Socket's IP Address
    	if ( null == socketToServer || !socketToServer.isConnected() ) {
    		socketIpAddr.setText("0.0.0.0");
    	} else {
    		socketIpAddr.setText(socketToServer.getLocalAddress().getHostAddress());
    	}
    }
    
    private void handleMessage(Message msg) {
    	
    	int idx;
    	long rtt;
    	Packet packet;
    	
    	switch ( msg.what ) {
		case WHAT_NETWORK_CHANGED:
			TesterDashboard.this.refresh();
			break;
			
		case WHAT_SEND_ECHO:
			idx = msg.arg1; // packet index
			txStat.setText("OK -- idx[" + idx + "] sentTime[" + new Date().toLocaleString() + "]");
			break;
			
		case WHAT_SEND_FAIL:
			idx = msg.arg1; // packet index
			txStat.setText("FAIL -- idx[" + idx + "] failTime[" + new Date().toLocaleString() + "]");
			break;
			
		case WHAT_RECEIVE_ECHO:
			packet = (Packet)msg.obj;
			Date receivedTime = new Date();
			
			if ( packet.deviceID != this.deviceID ) {
				rxStat.setText("Wrong DeviceID [" + packet.deviceID + "] ReceiveTime [" + receivedTime.toLocaleString() + "]");
			} else {
				
				rtt = receivedTime.getTime() - (long)(packet.timestamp * 1000);
				rxStat.setText("OK [idx:" + packet.idx + "]" + "   RTT [" + rtt + "ms]");
			}
			break;
		default:
			Toast.makeText(getApplicationContext(), "Unknown Message", Toast.LENGTH_SHORT).show();
		}
	}
    
    private void send() {
    	
    	if ( null == socketToServer || !socketToServer.isConnected() ) {
    		Toast.makeText(getApplicationContext(), "TCP Connect Required", Toast.LENGTH_SHORT).show();
    		return;
    	} else if ( isSending ) {
    		Toast.makeText(getApplicationContext(), "Already Sending", Toast.LENGTH_SHORT).show();
    		return;
    	}
    	
    	// send flag is set to TRUE
    	isSending = true;
    	
    	// Spawn a Message Dispatching and Reading Thread
    	Thread receiveThread = new Thread() {
    		@Override
    		public void run() {

    			while ( isSending ) {
    				try {
    					if ( null == socketToServer || !socketToServer.isConnected() ) {
    						Toast.makeText(getApplicationContext(), "Receive Thread Terminated[Socket Invalid]", Toast.LENGTH_SHORT).show();
    						handler.sendMessage(handler.obtainMessage(WHAT_NETWORK_CHANGED));
    						break;
    					}
    					
    					InputStream in = socketToServer.getInputStream();
    					byte[] echoReply = new byte[Packet.PACKET_SIZE];
    					int count = 0;
    					
    					while ( count < Packet.PACKET_SIZE ) {
    						count += in.read(echoReply, count, Packet.PACKET_SIZE-count);	// read till byte stream read can fill the packet buffer (20bytes)
    					}
    					
    					ByteBuffer readBuf = ByteBuffer.wrap(echoReply);
    					readBuf.order(ByteOrder.LITTLE_ENDIAN);
    					
    					int deviceID = readBuf.getInt();
    					int type = readBuf.getInt();
    					int idx = readBuf.getInt();
    					int network = readBuf.getInt();
    					double timestamp = readBuf.getDouble();
    					Packet packet = new Packet(deviceID, type, idx, network, timestamp);
    					
    					// send internel message to update UI with received data
    					handler.sendMessage(handler.obtainMessage(WHAT_RECEIVE_ECHO, packet));
    					
    					// Echo-back type 1 packet
    					if ( 0 == type && deviceID == TesterDashboard.this.deviceID ) {
    						OutputStream out = socketToServer.getOutputStream();
    						type = 1;
    						timestamp = (double)(new java.util.Date().getTime()/1000.0);
    						
    						// send echo-back with type 1 and new timestamp
    						Packet sendPacket = new Packet(deviceID, type, idx, network, timestamp);
    						out.write(sendPacket.encode());
    					}    					
    					
    				} catch(IOException ioe) {
    					Toast.makeText(getApplicationContext(), ioe.getMessage(), Toast.LENGTH_SHORT).show();
    				}
    			}
    		}
    	};
    	receiveThread.start();
    	
    	// Spawns a sending task which runs every 1 second periodically.
    	TimerTask sendTask = new TimerTask() {
    		@Override
    		public void run() {
				if ( socketToServer.isConnected() ) {
					int network = ((ConnectivityManager)getApplicationContext().
    						getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo().getType();
        			long timestamp = new Date().getTime();
        			Packet sendPacket = null;
        			
        			try {
						OutputStream out = socketToServer.getOutputStream();
						
						sendPacket = new Packet(TesterDashboard.this.deviceID,
						                        0, 
						                        packetIndex++, 
								                network, 
								                (double)(timestamp/1000.0));
						
						out.write(sendPacket.encode());
						handler.sendMessage(handler.obtainMessage(WHAT_SEND_ECHO, sendPacket));
					} catch (IOException e) {
						Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
						handler.sendMessage(handler.obtainMessage(WHAT_SEND_FAIL, sendPacket));
					}
				}	
    		}
    	};
    	
    	// Schedule Send Task
    	sendTimer = new Timer();
    	sendTimer.scheduleAtFixedRate(sendTask, 0, 1000);  
    	Toast.makeText(getApplicationContext(), "Start Sending", Toast.LENGTH_SHORT).show();
    }
    
    private void stop() {
    	if ( !isSending ) {
    		Toast.makeText(getApplicationContext(), "Try Start First", Toast.LENGTH_SHORT).show();
    		return;
    	}
    	
    	isSending = false;
    	sendTimer.cancel();
    	
    	Toast.makeText(getApplicationContext(), "Stop Sending", Toast.LENGTH_SHORT).show();
    }
    
    private void http() {
    	wifi3g.setText("http Pressed");
    }    
    
	private String getLocalIpAddress() {
		try {
			Enumeration<NetworkInterface> netInterfaces = NetworkInterface.getNetworkInterfaces();
			
			while ( netInterfaces.hasMoreElements() ) {
				NetworkInterface netInterface = netInterfaces.nextElement();
				Enumeration<InetAddress> inetAddrs = netInterface.getInetAddresses();
				while ( inetAddrs.hasMoreElements() ) {
					InetAddress inetAddr = inetAddrs.nextElement();
					if ( !inetAddr.isLoopbackAddress() ) {
						return inetAddr.getHostAddress();
					}
				}
			}
		} catch (Exception e) {

		}
		return null;
	}
}