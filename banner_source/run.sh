#!/bin/bash

if [ ! -e "BioC.dtd" ]; then
	pwd
	echo "go to root directory"
	exit
fi

if [ $# -lt 1 ]; then
	echo "#args < 1"
	exit
fi

tasktype=$1

if [ ! -d "log" ]; then
	mkdir log
fi

if [ $tasktype == "test" ]; then
	if [ $# -lt 2 ]; then
		echo "configfile missing for testing"
		exit
	fi
	echo "testing"
	configfile=$2
	java -cp 'lib/*' BANNER_BioC $configfile data/test_file.xml out.xml > log/current-test.txt
fi
