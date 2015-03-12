
#CFLAGS=-Wall -pedantic -s -O3 -m64 

#JAVA_HOME=/usr/lib/jvm/jdk1.7.0_71
#JAVA_HOME=/usr/lib/jvm/jdk1.8.0_31
SHELL = /bin/sh
CC=gcc
LCPP=g++ -v


#CFLAGS = 
#C64FLAGS       = -O2 -fPIC -pthread -DLINUX -D_LP64=1 
C64FLAGS       = -Wall -O3 -m64 -fPIC -pthread -DLINUX -D_LP64=1 
C32FLAGS       = -Wall -O3 -m32 -fPIC -pthread -DLINUX -D_LP32=1 
#-fPIC -g #-pedantic -Wall -Wextra -march=native -ggdb3
LDFLAGS      = -shared
#LDFLAGS      = -static-libgcc -shared -lc

DEBUGFLAGS   = -O0 -D _DEBUG
RELEASEFLAGS = -O2 -D NDEBUG -combine -fwhole-program
INCLUDES = -I $(JAVA_HOME)/include -I $(JAVA_HOME)/include/linux
L64TARGET  = -o linux_agent_64.so
L32TARGET  = -o linux_agent_32.so


native-agent: native-agent.cpp
	${LCPP} ${C64FLAGS} ${LDFLAGS} ${INCLUDES} ${L64TARGET} native-agent.cpp

native-agent32: native-agent.cpp
	${LCPP} ${C32FLAGS} ${LDFLAGS} ${INCLUDES} ${L32TARGET} native-agent.cpp

	
clean:
	rm *.so
