
#CFLAGS=-Wall -pedantic -s -O3 -m64 

#JAVA_HOME=/usr/lib/jvm/jdk1.7.0_71
JAVA_HOME=/usr/lib/jvm/jdk1.8.0_31
SHELL = /bin/sh
CC=gcc
CPP=g++ -v


#CFLAGS = 
CFLAGS       = -O2 -fPIC -pthread -DLINUX -D_LP64=1 
#-fPIC -g #-pedantic -Wall -Wextra -march=native -ggdb3
LDFLAGS      = -shared
#LDFLAGS      = -static-libgcc -shared -lc

DEBUGFLAGS   = -O0 -D _DEBUG
RELEASEFLAGS = -O2 -D NDEBUG -combine -fwhole-program
INCLUDES = -I $(JAVA_HOME)/include -I $(JAVA_HOME)/include/linux
TARGET  = -o agent.so


native-agent: native-agent.cpp
	${CPP} ${CFLAGS} ${LDFLAGS} ${INCLUDES} ${TARGET} native-agent.cpp
	
clean:
	rm *.so
