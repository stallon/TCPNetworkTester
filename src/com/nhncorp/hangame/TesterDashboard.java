package com.nhncorp.hangame;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.view.View;

public class TesterDashboard extends Activity {
	
	private Button btnConnect;
	private Button btnDisconnect;
	private Button btnRefresh;
	private Button btnSend;
	private Button btnStop;
	private Button btnHttp;
	
	private TextView wifi3G;
	private TextView isConnected;
	private TextView ipAddr;
	private TextView txStat;
	private TextView rxStat;
		
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        // Initialized Button Widgets and Add Listeners.
        btnConnect = (Button)findViewById(R.id.btnConnect);
        btnDisconnect = (Button)findViewById(R.id.btnDisconnect);
        btnRefresh = (Button)findViewById(R.id.btnRefresh);
        btnSend = (Button)findViewById(R.id.btnSend);
        btnStop = (Button)findViewById(R.id.btnStop);
        btnHttp = (Button)findViewById(R.id.btnHttp);
        
        wifi3G = (TextView)findViewById(R.id.wifi3g);
        isConnected = (TextView)findViewById(R.id.isConnected);
        ipAddr = (TextView)findViewById(R.id.ipAddr);
        txStat = (TextView)findViewById(R.id.txStat);
        rxStat = (TextView)findViewById(R.id.rxStat);
        
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
      

    }
    
    private void connect() {
    	ConnectivityManager conMgr = (ConnectivityManager)getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
    	wifi3G.setText(conMgr.getActiveNetworkInfo().getState().name());
    }
    
    private void disconnect() {
    	wifi3G.setText("disconnect Pressed");
    }
    
    private void refresh() {
    	wifi3G.setText("refresh Pressed");
    }
    
    private void send() {
    	wifi3G.setText("send Pressed");
    }
    
    private void stop() {
    	wifi3G.setText("stop Pressed");
    }
    
    private void http() {
    	wifi3G.setText("http Pressed");
    }
    
}