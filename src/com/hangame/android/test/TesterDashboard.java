package com.hangame.android.test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.Enumeration;

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
	private final static String SERVER_IP = "119.205.221.76";
	private final static int SERVER_PORT = 11001;
	
	// Objects for WAKE-LOCK
	private PowerManager pwrManager;
	private WakeLock wakeLock;
	
	// BroadcastReceiver for Network change events
	private BroadcastReceiver receiver;
	private Handler handler;
	private final static int WHAT_NETWORK_CHANGED = 0;
	
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
        		switch ( msg.what ) {
        		case WHAT_NETWORK_CHANGED:
        			TesterDashboard.this.refresh();
        			break;
    			default:
    				Toast.makeText(getApplicationContext(), "Unknown Message", Toast.LENGTH_SHORT).show();
        		}
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
    		socketToServer = new Socket(SERVER_IP, SERVER_PORT);
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
    
    private void send() {
    	wifi3g.setText("send Pressed");
    }
    
    private void stop() {
    	wifi3g.setText("stop Pressed");
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