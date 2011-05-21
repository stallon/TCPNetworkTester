package com.hangame.android.test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

// packet definition
class Packet {
	public Packet(int deviceID, int type, int idx, int network, double timestamp) {
		this.deviceID = deviceID;
		this.type = type;
		this.idx = idx;
		this.network = network;
		this.timestamp = timestamp;
	}
	public byte[] encode() {
		ByteBuffer packet = ByteBuffer.allocate(PACKET_SIZE); 
		packet.order(ByteOrder.LITTLE_ENDIAN);
		
		packet.putInt(this.deviceID);
		packet.putInt(this.type);
		packet.putInt(this.idx);
		packet.putInt(network);
		packet.putDouble(timestamp);
		
		return packet.array();
	}
	public int deviceID;
	public int type;
	public int idx;
	public int network;
	public double timestamp;
	
	public final static int PACKET_SIZE = 24;
};