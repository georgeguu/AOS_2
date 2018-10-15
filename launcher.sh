#!/bin/bash

# Change this to your netid
netid=gxg171430

# Root directory of your project
PROJDIR=/home/010/g/gx/gxg171430/CS6378/AOS2

# Directory where the config file is located on your local system
CONFIGLOCAL=$PROJDIR/config.txt

CONFIGSERVER=$PROJDIR/config.txt
# Directory your java classes are in
BINDIR=$PROJDIR

# Your main project class
PROG=parseFile_test

n=1

cat $CONFIGLOCAL | sed -e "s/#.*//" | sed -e "/^\s*$/d" |
(
    read i
    echo $i
	i=$(( i + 1 ))
	#read root
    #echo $root
    while [[ $n -lt $i ]]
    do
        read line
        #p=$( echo $line | awk '{ print $1 }' )
        host=$( echo $line | awk '{ print $1 }' )
        port=$( echo $line | awk '{ print $2 }' ) # port

    bash -c "ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no $netid@$host.utdallas.edu java -cp $BINDIR $PROG $port $n $CONFIGSERVER; exec bash" &

        n=$(( n + 1 ))
    done
)

