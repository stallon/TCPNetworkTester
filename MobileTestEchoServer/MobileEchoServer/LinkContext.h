#pragma once

#include <XBase/XSystem.h>
#include <XBase/XStream.h>
#include <XBase/XInNetwork.h>
#include <fstream>
#include <map>

using namespace XSystem;
using namespace XStream;
using namespace XInNetwork;

#define RECV_BUF_SIZE	20480

class LinkContext
{
public:
	LinkContext(); 
	virtual ~LinkContext();

	// packet index method
	void SetPacketTime(unsigned index, double time);
	double GetPacketTime(unsigned index);
	bool Contains(unsigned index) const;
	void RemovePacketTime(unsigned index);
	void ClearAll();

	// buffer method
	MemoryPool::XMemory GetXMemory();
	int WriteBuffer(const void* s, int size);
	int ReadBuffer(void* p, int length);
	int PeekBuffer(void* p, int length);
	void ClearBuffer();
	int GetBufferLength() const;

	// Logger method
	bool OpenLogfile(const char* filename);
	void WriteLog(const char* s);

public:
	static void WINAPI OnLinkDestroy(Link::Handle hLink, void* context);

private:
	std::ofstream logfile_;
	std::map<unsigned, double> packets_; 
	XStream::Handle buffer_;
};
