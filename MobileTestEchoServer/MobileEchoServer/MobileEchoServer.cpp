/**
* @file		MobileEchoServer.cpp
* @brief	Echo Server for Mobile Environment Test
* @author	SeongBong Hong <stallon.hong@nhn.com>
* @date		$Date: 2011-06-09$
* @version	$Revision: $
* $Id: $
*/


#include "stdafx.h"

#include <iostream>
#include <fstream>
#include <cstring>
#include <string>
#include <map>

#include <XBase/XPlatform.h>
#include <XBase/XSystem.h>
#include <XBase/XInNetwork.h>
#include <XBase/XStream.h>

#include "LinkContext.h"
#include "Packet.h"

using namespace XSystem;
using namespace XInNetwork;

Threading::CriticalSection cs;

char* GetNetworkTypeString (unsigned int x)
{
	return (x) ? "WIFI" : "3G";
}

void __stdcall OnAccepted(Acceptor::THandle hAcceptor, Link::THandle hLink, void * pContext)
{
	Address::Handle RemoteAddress = Link::GetRemoteAddress(hLink);
	if ( NULL == RemoteAddress )
	{
		std::cout << ">>> Accepted. But Invalid Remote Address : " << XPlatform::GetLastError() << std::endl;
		return;
	}

	char RemoteIPAddress[16];
	memset(RemoteIPAddress, 0, sizeof(RemoteIPAddress));
	if ( !Address::GetIPAddress(RemoteAddress, RemoteIPAddress, sizeof(RemoteIPAddress)) )
	{
		std::cout << ">>> Accepted. But Invalid Remote IP Address." << std::endl;
		return;
	}
	
	cs.Enter();
	
	std::cout << ">>> Client Connected From " << RemoteIPAddress << std::endl;
//	Address::DestroyHandle(RemoteAddress);
	LinkContext* ctx = new LinkContext;

	SYSTEMTIME lt;
	char filename[MAX_PATH];
	GetLocalTime(&lt);
	sprintf(filename, "packetlog-%04d%02d%02d_%02d%02d%02d.txt", lt.wYear, lt.wMonth, lt.wDay, lt.wHour, lt.wMinute, lt.wSecond);
	
	ctx->OpenLogfile(filename);

	cs.Leave();

	Link::SetData(hLink, ctx, &LinkContext::OnLinkDestroy, NULL);	// packet index & timer storage
	Link::Recv(hLink, RECV_BUF_SIZE);
}



void __stdcall OnReceived(Acceptor::THandle hAcceptor, Link::THandle hLink, void * pContext)
{
	int receivedBytes = 0;
	int internalBufferedBytes = 0;
	int dummyLength = 0;
	
	Packet packetHeader;
	Packet *packetToSend;

	// Get Client IP Address.
	char RemoteIPAddress[16];
	memset(RemoteIPAddress, 0, sizeof(RemoteIPAddress));
	if ( !Address::GetIPAddress(Link::GetRemoteAddress(hLink), RemoteIPAddress, sizeof(RemoteIPAddress)) )
	{
		std::cout << ">>> OnReceive() called. But Invalid Remote IP Address." << std::endl;
		return;
	}

	// Add link-reference before processing received data via hLink to prevent from being closed or destroyed by another thread!!!
	// Be sure to Release the link-reference before leaving this function
	Link::AddRef(hLink);

	// Obtain bytes from Link
	XStream::Handle hStream = Link::LockReadStream(hLink);
	receivedBytes = XStream::GetLength(hStream);

	// If received byte count is 0, quit.
	if ( receivedBytes <= 0 )
	{
		std::cout << ">>> [" << RemoteIPAddress << "] 0 byte(s) received." << std::endl;
		
		Link::UnlockReadStream(hLink);
		Link::Release(hLink);
		Link::Close(hLink);
		return;
	}

	// move received data to the link-local buffer
	LinkContext* ctx = (LinkContext*)Link::GetData(hLink);
	MemoryPool::XMemory pStream = XStream::Detach(hStream);		// if you use XStream::GetXMemory(hStream) to move received data into link-local buffer,
																// No data is removed from the TCP receive buffer.
	ctx->WriteBuffer(pStream, receivedBytes);
	MemoryPool::Free(pStream);

	Link::UnlockReadStream(hLink);

	// total received bytes count is bigger than PacketHeader size?
	// if no, wait for more bytes to come.
	internalBufferedBytes = ctx->GetBufferLength();

	if ( internalBufferedBytes < sizeof(Packet) )	
	{
		Link::Release(hLink);
		Link::Recv(hLink, RECV_BUF_SIZE);

		return;
	}
	
	// Read the PacketHeader and Check packet signature
	ctx->PeekBuffer((void*)&packetHeader, sizeof(Packet));

	if ( SIGNATURE != packetHeader.signature )
	{
		std::cout << ">>> [" << RemoteIPAddress << "] Invalid SIGNATURE. So Disconnect the link" << std::endl;
		
		int len = ctx->GetBufferLength();
		char* remaining = new char[len];
		ctx->ReadBuffer(remaining, len);
		
		printf(">>> [%s] buffer remaining %d bytes : 0x", RemoteIPAddress, len);
		for(unsigned i = 0; i < len; i++ ) 
		{
			printf("%X", *(remaining+i));
		}

		printf("\n");
		delete [] remaining;

		Link::Release(hLink);	// decrease the ref-count which is increased at the first stage of this function
		Link::Close(hLink);		// disconnect the link

		return;
	}

	// if received data does not contain full message?
	if ( internalBufferedBytes < packetHeader.dummyDataSize + sizeof(Packet) )
	{
		printf(">>> [%s] expected %d bytes, but received %d bytes\n", 
					RemoteIPAddress, 
					packetHeader.dummyDataSize + sizeof(Packet), 
					internalBufferedBytes);
		
		Link::Release(hLink);
		Link::Recv(hLink, RECV_BUF_SIZE);
		return;
	} 
	
	// Now we are sure that we have full message, 
	packetToSend = (Packet*)malloc(sizeof(Packet)+packetHeader.dummyDataSize);		// if you use stack memory for the packetToSend, you might experience buffer overflow.
																					// Because only sizeof(Packet) bytes is allocated for packetToSend automatically.
	ctx->ReadBuffer(packetToSend, sizeof(Packet)+packetHeader.dummyDataSize);

	// Print-out packet information
	printf (">>> [%s] packetsize: %u deviceId: %u type: %u idx: %u network: %u time: %u\n", 
		RemoteIPAddress,
		packetToSend->dummyDataSize + sizeof(Packet),
		packetToSend->deviceID,
		packetToSend->type,
		packetToSend->idx,
		packetToSend->network,
		(unsigned int)packetToSend->timestamp);

	
	// if packet type = 0, create a new timestamp entry and send reply.
	// if packet type = 1, look up a timestamp in the map and calculate the RTT, and write it to the file
	if (packetToSend->type == 0) 
	{
		ctx->SetPacketTime(packetToSend->idx, packetToSend->timestamp);
		if ( 0 != Link::Send(hLink, (const void*)packetToSend, sizeof(Packet)+packetToSend->dummyDataSize) )
		{
			// failed to send reply @type=0
			printf (">>> [%s] error!! send fail. (%d)\n", RemoteIPAddress, XPlatform::GetLastError());
			
			free(packetToSend);
			Link::Release(hLink);
			Link::Close(hLink);
			return;
		}
		else
		{
			// send reply successfully @type=0
			printf (">>> [%s] idx (%u) replied.\n", RemoteIPAddress, packetToSend->idx );
			free(packetToSend);
		}
	} 
	else 
	{
		double prevTime = ctx->GetPacketTime(packetToSend->idx);


		if ( prevTime == 0.0 ) 
		{
			printf(">>> [%s] unknown packet idx(%u).\n", RemoteIPAddress, packetToSend->idx);

			free(packetToSend);
			Link::Release(hLink);
			Link::Recv(hLink, RECV_BUF_SIZE);

			return;
		}
		else
		{
			SYSTEMTIME lt;
			GetLocalTime(&lt);
			char message[1024];
			sprintf (message, "%02d:%02d:%02d, %u,%u,%u,%s,%f\n", lt.wHour, lt.wMinute, lt.wSecond, sizeof(Packet)+packetToSend->dummyDataSize, packetToSend->deviceID, packetToSend->idx, GetNetworkTypeString(packetToSend->network), packetToSend->timestamp-prevTime);

			ctx->WriteLog(message);
			ctx->RemovePacketTime(packetToSend->idx);

			printf (">>> [%s] %s", RemoteIPAddress, message);
			free(packetToSend);
		}
	}	

	Link::Release(hLink);
	Link::Recv(hLink, RECV_BUF_SIZE);
}



void __stdcall OnClosed(Acceptor::THandle hAcceptor, Link::THandle hLink, void * pContext)
{
	// Get Client IP Address.
	char RemoteIPAddress[16];
	memset(RemoteIPAddress, 0, sizeof(RemoteIPAddress));
	if ( !Address::GetIPAddress(Link::GetRemoteAddress(hLink), RemoteIPAddress, sizeof(RemoteIPAddress)) )
	{
		std::cout << ">>> OnClosed() called. But Invalid Remote IP Address." << std::endl;
	}
	else 
	{
		printf(">>> [%s] Link Closed.\n", RemoteIPAddress);
	}

//	Link::Release(hLink);	// Is this the right time to call Release()?
}


int _tmain(int argc, _TCHAR* argv[])
{
	int listening_port = 0;

	if ( argc < 2 ) 
	{
		std::cout << ">>> Usage: MobileEchoServer.exe <tcp port number larger than 10,000>" << std::endl;
		return -1;
	} 
	else
	{
		listening_port = atoi(argv[1]);

		if ( listening_port <= 10000 || listening_port > 65535 ) 
		{
			std::cout << ">>> Port number SHOULD be between 10,001 and 65535." << std::endl;
			return -1;
		}
	}

	// Create a ThreadPool
	ThreadPool::THandle hThreadPool = ThreadPool::CreateHandle(XPlatform::GetProcessorCount() * 2);	// (13)

	// Create an Acceptor. Bind a ThreadPool to Acceptor.
	// A single ThreadPool can be shared between several Acceptors, Connectors and UDPListeners.
	Acceptor::THandle hAcceptor = Acceptor::CreateHandle(hThreadPool);	// (14)

	// (15)
	//Set event callback functions for socket events
	Acceptor::SetOnAccepted(hAcceptor, &OnAccepted, NULL);
	Acceptor::SetOnReceived(hAcceptor, &OnReceived, NULL);
	Acceptor::SetOnClosed(hAcceptor, &OnClosed, NULL);

	std::cout << ">>> Acceptor Started to listen on PORT:" << listening_port << std::endl;

	Acceptor::SetDefaultPort(hAcceptor, listening_port);
	Acceptor::Start(hAcceptor);

	std::string keyInput;
	std::cin >> keyInput;


	return 0;
}

