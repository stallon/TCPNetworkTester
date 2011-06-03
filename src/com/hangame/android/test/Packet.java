package com.hangame.android.test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

// packet definition
class Packet {
	public Packet(int deviceID, int type, int idx, int network, double timestamp, int packetLength) {
		this.deviceID = deviceID;
		this.type = type;
		this.idx = idx;
		this.network = network;
		this.timestamp = timestamp;
		this.packetLength = packetLength;
	}
	public byte[] encode() {
		ByteBuffer packet = ByteBuffer.allocate(packetLength); 
		packet.order(ByteOrder.LITTLE_ENDIAN);
		
		packet.putInt(PACKET_SIGNATURE);
		packet.putInt(this.deviceID);
		packet.putInt(this.type);
		packet.putInt(this.idx);
		packet.putInt(network);
		packet.putDouble(timestamp);
		packet.putInt(packetLength - PACKET_SIZE_WITHOUT_DUMMY);
		
		byte[] padding = new byte[packetLength - PACKET_SIZE_WITHOUT_DUMMY];
		Arrays.fill(padding, (byte)'1');
		packet.put(padding);
		
		return packet.array();
	}
	public int deviceID;
	public int type;
	public int idx;
	public int network;
	public double timestamp;
	public int packetLength;
	
	public final static int PACKET_SIZE_WITHOUT_DUMMY = 32;
	public final static int PACKET_SIGNATURE = 0xCAFEBAB0;
};