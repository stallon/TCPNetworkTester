#include "StdAfx.h"
#include "LinkContext.h"


LinkContext::LinkContext() 
{ 
	buffer_ = XStream::CreateHandle();
}

LinkContext::~LinkContext() 
{ 
	ClearAll(); 
	XStream::DestroyHandle(buffer_);
	if ( logfile_ ) { logfile_.close(); }
}

void LinkContext::SetPacketTime(unsigned index, double time) 
{
	packets_[index] = time;
}

double LinkContext::GetPacketTime(unsigned index)
{
	if ( !Contains(index) )
	{
		return 0.0;
	}
	return packets_[index];
}

bool LinkContext::Contains(unsigned index) const
{
	std::map<unsigned, double>::const_iterator itor = packets_.find(index);
	if ( itor == packets_.end() )
	{
		return false;
	}

	return true;
}

void LinkContext::RemovePacketTime(unsigned index)
{
	packets_.erase(index);
}

void LinkContext::ClearAll()
{
	packets_.clear();
}

bool LinkContext::OpenLogfile(const char* filename)
{
	if ( logfile_.is_open() )
	{
		return true;
	}

	logfile_.open(filename);
	return logfile_.is_open();
}

void LinkContext::WriteLog(const char* s)
{
	logfile_ << s;
	logfile_.flush();
}

MemoryPool::XMemory LinkContext::GetXMemory()
{
	return XStream::GetXMemory(buffer_);
}

int LinkContext::WriteBuffer( const void* s, int size )
{
	return XStream::Write(buffer_, s, size);
}

int LinkContext::ReadBuffer( void* p, int length )
{
	return XStream::Read(buffer_, p, length);
}

int LinkContext::PeekBuffer(void* p, int length)
{
	return XStream::Peek(buffer_, p, length);
}

void LinkContext::ClearBuffer()
{
	XStream::Clear(buffer_);
}

int LinkContext::GetBufferLength() const
{
	return XStream::GetLength(buffer_);
}

void __stdcall LinkContext::OnLinkDestroy(Link::Handle hLink, void* context)
{
	// Get Client IP Address.
	char RemoteIPAddress[16];
	memset(RemoteIPAddress, 0, sizeof(RemoteIPAddress));
	Address::GetIPAddress(Link::GetRemoteAddress(hLink), RemoteIPAddress, sizeof(RemoteIPAddress));

	printf(">>> [%s] Link Destroyed. \n", RemoteIPAddress);

	LinkContext *linkCtx = (LinkContext*)Link::GetData(hLink);

	if ( NULL != linkCtx ) 
	{
		delete linkCtx;
	}
}