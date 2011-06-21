package com.hangame.android.test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

// packet definition
class Packet {
	
	public final static int PACKET_SIZE_WITHOUT_DUMMY = 32;
	public final static int PACKET_SIGNATURE = 0xCAFEBAB0;
	
	public int deviceID, type, idx, network;
	public double timestamp;
	public int length;	// byte length with header size plus dummy size
	
	public Packet(int deviceID, int type, int idx, int network, double timestamp, int packetLength) {
		this.deviceID = deviceID;
		this.type = type;
		this.idx = idx;
		this.network = network;
		this.timestamp = timestamp;
		this.length = packetLength;
	}
	
	public ByteBuffer encode() {
		
		ByteBuffer buf = ByteBuffer.allocateDirect(length);
		
		buf = ByteBuffer.allocateDirect(length);
		buf.order(ByteOrder.LITTLE_ENDIAN);
		
		buf.putInt(PACKET_SIGNATURE);
		buf.putInt(deviceID);
		buf.putInt(type);
		buf.putInt(idx);
		buf.putInt(network);
		buf.putDouble(timestamp);
		buf.putInt(length-PACKET_SIZE_WITHOUT_DUMMY);
		
		byte[] padding = new byte[length - PACKET_SIZE_WITHOUT_DUMMY];
		Arrays.fill(padding, (byte)'1');
		buf.put(padding);
		buf.flip();
		
		return buf;
	}
};