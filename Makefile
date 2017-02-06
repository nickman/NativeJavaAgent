
SHELL = /bin/sh
CC=gcc
LCPP=g++ -v
W64CPP=x86_64-w64-mingw32-g++
W32CPP=i686-w64-mingw32-g++


C64FLAGS       = -O3 -m64 -fPIC -pthread -DLINUX -D_LP64=1 
W64FLAGS       = -O3 -m64 -fPIC -pthread -DWINDOWS -D_LP64=1 
W32FLAGS       = -O3 -m32 -fPIC -pthread -DWINDOWS -D_LP32=1 
C32FLAGS       = -Wall -O3 -m32 -fPIC -pthread -DLINUX -D_LP32=1 
#-fPIC -g #-pedantic -Wall -Wextra -march=native -ggdb3
LDFLAGS      = -shared
#LDFLAGS      = -static-libgcc -shared -lc

DEBUGFLAGS   = -O0 -D _DEBUG
RELEASEFLAGS = -O2 -D NDEBUG -combine -fwhole-program
INCLUDES = -I $(JAVA_HOME)/include -I $(JAVA_HOME)/include/linux
DISTDIR = ./target/native
L64TARGET  = -o ${DISTDIR}/linux64/oif_agent.so
L32TARGET  = -o ${DISTDIR}/linux32/oif_agent.so
W64TARGET  = -o ${DISTDIR}/win64/oif_agent.dll
W32TARGET  = -o ${DISTDIR}/win32/oif_agent.dll

# /usr/bin/i686-w64-mingw32-c++
# /usr/bin/x86_64-w64-mingw32-g++




linux_native-agent-64: ./src/main/cpp/native-agent.cpp	distdirl64
	${LCPP} ${C64FLAGS} ${LDFLAGS} ${INCLUDES} ${L64TARGET}  ./src/main/cpp/native-agent.cpp

linux_native-agent-32: ./src/main/cpp/native-agent.cpp	distdirl32
	${LCPP} ${C32FLAGS} ${LDFLAGS} ${INCLUDES} ${L32TARGET}  ./src/main/cpp/native-agent.cpp


windows_native-agent-64: ./src/main/cpp/native-agent.cpp	distdirw64
	${W64CPP} ${W64FLAGS} ${LDFLAGS} ${INCLUDES} ${W64TARGET}  ./src/main/cpp/native-agent.cpp

windows_native-agent-32: ./src/main/cpp/native-agent.cpp	distdirw32
	${W32CPP} ${W32FLAGS} ${LDFLAGS} ${INCLUDES} ${W32TARGET}  ./src/main/cpp/native-agent.cpp

all: linux_native-agent-64 linux_native-agent-32 windows_native-agent-64 windows_native-agent-32

distdirl64:
	test -d ${DISTDIR}/linux64 || mkdir -p ${DISTDIR}/linux64

distdirl32:
	test -d ${DISTDIR}/linux32 || mkdir -p ${DISTDIR}/linux32

distdirw64:
	test -d ${DISTDIR}/win64 || mkdir -p ${DISTDIR}/win64

distdirw32:
	test -d ${DISTDIR}/win32 || mkdir -p ${DISTDIR}/win32
	
clean:
	rm -rf ./target/native
