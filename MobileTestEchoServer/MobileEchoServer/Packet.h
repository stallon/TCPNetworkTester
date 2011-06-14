#pragma once

#if defined(_WIN32) || defined(_WIN64)
#if _MSC_VER >= 1600
	#include <stdint.h>
#else
	typedef unsigned int	uint32_t;
#endif	// _MSC_VER >= 1600
#endif	// defined(_WIN32) || defined(_WIN64)

#define SIGNATURE	0xCAFEBAB0

#pragma pack(1)
typedef struct _Packet_T 
{
	uint32_t signature;
	uint32_t deviceID;
	uint32_t type;
	uint32_t idx;
	uint32_t network;
	double timestamp;
	uint32_t dummyDataSize;
	unsigned char dummyData[0];
} Packet;
#pragma pack()
