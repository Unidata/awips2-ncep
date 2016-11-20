#!/bin/bash
# 1. Create a Java project and source folder....
# 2. Configure build path (see M. Li's lecture note):
#	Right click the working project -> Build Path -> Configure build path
#	-> Libraries -> Add JARs -> com.sun.jna – jna.jar
# 3. cp /usr/lib/gcc/i386-redhat-linux/3.4.6/libg2c.so locally, e.g.,
#	$AWIPS2/lib/so
# 4. Use this script to create a shared library (.so)
# 5. Deploy the SL and add the path, e.g., AWIPS2/lib, to LD_LIBRARY_PATH
#    (in ~/.alias)

. ~/GEMPAK7/Gemenviron.profile
CC=gcc
FC=gfortran

myCflags="$CFLAGS -I. -I$GEMPAK/source/diaglib/dg -I$GEMPAK/source/gemlib/er -DDEBUG -c -fPIC"
myFflags="-I. -I$OS_INC -I$GEMPAK/include -fPIC -g -c -Wall -Wtabs -fno-second-underscore"
myLinkflags="-L/usr/lib/gcc/x86_64-redhat-linux/4.8.2/ -shared -Wl,-soname,libgempak.so -o libgempak.so"
myLinktail="/usr/lib64/libg2c.so.0 -lc -lgfortran"

myLibs="-I$OS_INC -I$GEMPAK/include -I.  \
$OS_LIB/ginitp_alt.o $OS_LIB/gendp_alt.o $OS_LIB/libsflist.a $OS_LIB/libgdlist.a $OS_LIB/libgemlib.a $OS_LIB/libgplt.a $OS_LIB/libdevice.a $OS_LIB/libgn.a $OS_LIB/libcgemlib.a $OS_LIB/libgemlib.a /usr/lib64/libnetcdf.so.7 $OS_LIB/libtextlib.a /usr/lib64/libxslt.so.1 /usr/lib64/libxml2.so /usr/lib64/libz.so $OS_LIB/libbz2.a"







$CC $myCflags *.c
$FC $myFflags *.f
$CC $myLinkflags *.o $myLibs $myLinktail

echo ""
echo "$CC $myCflags *.c"
echo ""
echo "$FC $myFflags *.f"
echo ""
echo "$CC $myLinkflags *.o $myLibs $myLinktail"



cp libgempak.so /home/awips/awips2-ncep/viz/gov.noaa.nws.ncep.viz.gempak.nativelib.linux64/libgempak.so
