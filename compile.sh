#!/bin/sh
# This is a comment!
# if javac Server.java ; then
# 	java Server
# 	echo "Compile Server.java OK"
# else
#     echo "Compile failed"
# fi
javac Node.java;
mv Node.class ./bin/;

javac Parseconfig.java;
mv Parseconfig.class ./bin/;


javac parseFile_test.java;
# java Server2;
