CC=g++

SRCS=LinkContext.cpp MobileEchoServer.cpp stdafx.cpp
OBJS=$(SRCS:.cpp=.o)

XBASE_PATH=/home/stallon/PROJECT/lib/XBase
XBASE_LIB=-lXInNetworkStatic -lXPlatformStatic -lXSystemStatic -lXStreamStatic
#XBASE_LIB=-lXInNetwork -lXPlatform -lXSystem -lXStream

CFLAGS=
LFLAGS=

INCLUDES= -I$(XBASE_PATH)/include
LIBPATH=-L$(XBASE_PATH)/lib
#LIBS= -lpthread -ldl $(XBASE_LIB)
LIBS= -lpthread $(XBASE_LIB)

TARGET=MobileEchoServer

all: $(TARGET)

$(TARGET): $(OBJS)
	$(CC) -o $@ $(OBJS) $(LFLAGS) $(LIBPATH) $(LIBS) 

$(OBJS):
	$(CC) -c $(SRCS) $(INCLUDES) $(CFLAGS)

clean:
	$(RM) $(OBJS) $(TARGET) core

recompile: clean all

