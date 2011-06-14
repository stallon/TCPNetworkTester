// echo.cpp : 콘솔 응용 프로그램에 대한 진입점을 정의합니다.
//

#include "stdafx.h"
#include <tchar.h>
#include <winsock2.h>
#include <stdio.h>

#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <process.h>
#include <map>

 
#define BUFSIZE 1024

#pragma comment (lib, "ws2_32.lib")
 
typedef struct  {
	int id;
	SOCKET hClientSock;
	SOCKADDR_IN clntAddr;
} THREAD_PARAM, *PTHREAD_PARAM;

#pragma pack(1)
struct Packet {
	unsigned int signature;
	unsigned int deviceID;
	unsigned int type;
	unsigned int idx;
	unsigned int network;
	double timestamp;
	unsigned int dummyDataSize;
	unsigned char dummyData[0];
};
#pragma pack()

#define SIGNATURE	0xCAFEBAB0
#define SIGNATURE_LENGTH	4
#define HEADER_LENGTH	sizeof(Packet)

char* GetNetworkTypeString (unsigned int x)
{
	return (x) ? "WIFI" : "3G";
}


DWORD WINAPI KeyInputThreadProc(LPVOID lpParameter)
{
	SOCKET hServerSock = (SOCKET) lpParameter;

	getchar ();

	closesocket (hServerSock);
	return 0;
}

unsigned __stdcall PacketThreadProc(void* lpParameter)
{
	PTHREAD_PARAM threadParam = (PTHREAD_PARAM) lpParameter;
	
	char message[BUFSIZE] = {0,};
	char headerBuf[HEADER_LENGTH];
	Packet * packet = NULL;

	int len;			// return value placeholder of recv() function
	int packetLength;	// length of (SIGNATURE + HEADER + dummyData)
	int dummyDataSize;	// only dummyData length
	
	SYSTEMTIME lt;
	char filename[MAX_PATH];
	std::map<unsigned int, double> packets;

	GetLocalTime(&lt);
	sprintf(filename, "packetlog-%02d%02d%02d.txt", lt.wHour, lt.wMinute, lt.wSecond);
	FILE *fp = fopen (filename, "w+");
	if (!fp) 
		printf ("** fopen error!!\n");

	if(threadParam->hClientSock==INVALID_SOCKET) {
        printf("accept() Error!!");
		goto error;
	}

	printf ("** accepted(%s).\n", inet_ntoa ( (struct in_addr)threadParam->clntAddr.sin_addr ));

	while( true ) {
		
		// STEP 1. peeking packet header with SIGNATURE (32bytes)
		if ( (len = recv(threadParam->hClientSock, headerBuf, HEADER_LENGTH, MSG_PEEK)) >= HEADER_LENGTH ) 
		{
			int signatureTemp;
			memcpy(&signatureTemp, headerBuf, SIGNATURE_LENGTH);
		//	printf(">>> Received signature -- 0x%X \n", signatureTemp);			

			if ( SIGNATURE != signatureTemp ) {
				// if signature doesn't match, discard first 4 bytes from the socket buffer.
				// And try to read again 
				printf(">>> Wrong singnature. Move 1 byte forward and read again... \n");
				recv(threadParam->hClientSock, headerBuf, 1, 0);		
				continue;
			}
		} 
		else if ( len <= 0 )
		{
			// recv() failed or connection closed
			printf(">>> recv() fail. len=%d (%d) \n", len, WSAGetLastError());
			break;
		}
		else
		{
			// if peeked data size is less than FULL_HEADER_LENGTH, read more socket buffer.
			continue;
		}
		
		// STEP 2. read dummyDataSize and prepare buffer.
		memcpy(&dummyDataSize, headerBuf+28, sizeof(unsigned int));

		packetLength = HEADER_LENGTH + dummyDataSize;
		packet = (Packet*)malloc(packetLength);

		// STEP 3. read the full packet from the socket 
		int receivedBytes = 0;	// received bytes counter SHOULD be reset at this point.
		
		while ( receivedBytes < packetLength )
		{
			// read along the buffer (shifted by receivedBytes)
			if ( (len = recv(threadParam->hClientSock, (char*)(packet)+receivedBytes, packetLength-receivedBytes, 0)) > 0 )
			{
				receivedBytes += len;
				continue;
			}
			else 
			{
				printf(">>> recv() fail. len=%d (%d) \n", len, WSAGetLastError());
				goto thread_cleanup;
			}
		}

		// STEP 4. if reading the packet is successful, print the information
		printf ("(%u) signature: 0x%X dsize: %u deviceId: %u type: %u idx: %u network: %u time: %u\n", 
			len,
			packet->signature,
			packet->dummyDataSize,
			packet->deviceID,
			packet->type,
			packet->idx,
			packet->network,
			(unsigned int)packet->timestamp
			);
		
		// STEP 5. if packet type = 0, create a new timestamp entry and send reply.
		//         if packet type = 1, look up a timestamp in the map and calculate the RTT, and write it to the file
		if (packet->type == 0) {
			packets[packet->idx] = packet->timestamp;
			int slen = send(threadParam->hClientSock, (char*)packet, HEADER_LENGTH+packet->dummyDataSize, 0);
			if (slen <= 0)
			{
				printf ("** error!! send fail. (%d)\n", len, GetLastError());
				break;
			}
			else
			{
				printf ("** idx (%u) reply bytes: %u\n", packet->idx, slen);
			}
		} else {

			std::map<unsigned int, double>::iterator itor;
			itor = packets.find(packet->idx);
			if (itor == packets.end()) {
				printf("** unknown packet idx(%u).\n", packet->idx);
				continue;
			}
			
			GetLocalTime(&lt);
			sprintf (message, "%02d:%02d:%02d, %u,%u,%u,%s,%f\n", lt.wHour, lt.wMinute, lt.wSecond, sizeof(Packet)+packet->dummyDataSize, packet->deviceID, packet->idx, GetNetworkTypeString(packet->network), packet->timestamp - packets[packet->idx]);
			if (fp) {
				fputs(message,fp);
				fflush(fp);
			}
			packets.erase(packet->idx);
			printf ("%s", message);
		}
	}
	/*


	
	char recvbuf [256] = {0,};

	while((len=recv(threadParam->hClientSock, recvbuf, sizeof(recvbuf), 0)) > 0) {
		printf ("%s\n", recvbuf);
		send (threadParam->hClientSock, recvbuf, len, 0);
		ZeroMemory (recvbuf, sizeof(recvbuf));
			
	}
	*/
	
thread_cleanup:

	free(packet);

	closesocket (threadParam->hClientSock);
	if (fp) fclose(fp);
	packets.clear();

	printf ("** disconnected(%s).\n", inet_ntoa ( (struct in_addr)threadParam->clntAddr.sin_addr ));

error:
	free (threadParam);
	return 0;
}
 
int _tmain(int argc, _TCHAR* argv[])
{
	//int j = sizeof(double);
    WSADATA wsaData;
    SOCKET  hServSock;
    SOCKET  hClntSock;
	int id = 0;  
 
    SOCKADDR_IN servAddr;
    SOCKADDR_IN clntAddr;
    int clntAddrSize;
 
    /*윈속 초기화*/
    if(WSAStartup(MAKEWORD(2,2),&wsaData) !=0)
        printf("WSAStartup Error!");
 
    hServSock=socket(PF_INET, SOCK_STREAM, 0);
    if(hServSock == INVALID_SOCKET)
        printf("socket Error!!");
 
    memset(&servAddr, 0, sizeof(servAddr));
    servAddr.sin_family=AF_INET;
    servAddr.sin_addr.s_addr=htonl(INADDR_ANY);
    servAddr.sin_port=htons(11001);
 
    if(bind(hServSock, (SOCKADDR *)&servAddr, sizeof(servAddr))==SOCKET_ERROR)
        printf("bind() Error!!");
 
    if(listen(hServSock, 5)==SOCKET_ERROR)
        printf("listen() Error!!");
 
	CreateThread (NULL, 0, KeyInputThreadProc, (LPVOID)hServSock, 0, NULL);

    clntAddrSize=sizeof(clntAddr);	
	while (1) {
		hClntSock=accept(hServSock, (SOCKADDR *) &clntAddr, &clntAddrSize);
		if ( INVALID_SOCKET==hClntSock) {
			break;
		}

		PTHREAD_PARAM threadParam = (PTHREAD_PARAM) malloc (sizeof(THREAD_PARAM));
		threadParam->hClientSock = hClntSock;
		threadParam->clntAddr = clntAddr;
		threadParam->id = ++id;

		_beginthreadex(NULL, 0, PacketThreadProc, threadParam, 0, NULL);
	}
		
    WSACleanup();
    return 0;
}

