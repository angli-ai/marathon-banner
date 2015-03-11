#!/bin/bash

if [ ! -e "BioC.dtd" ]; then
	pwd
	echo "go to root directory"
	exit
fi

if [ $# -lt 1 ]; then
	echo "run.sh [tasktype: train|test] [config-file] [log]"
	exit
fi

tasktype=$1

if [ ! -d "log" ]; then
	mkdir log
fi

maxmem=6g
if [ $tasktype == "train" ]; then
	if [ $# -lt 2 ]; then
		echo "configfile missing for training"
		exit
	fi
	echo "training"
	configfile=$2
	if [ $# == 3 ] && [ $3 == "log" ]; then
		logfile=`basename $configfile`
		java -Xmx$maxmem -cp 'lib/*' banner.eval.BANNER train $configfile > log/$logfile.txt
	else
		java -Xmx$maxmem -cp 'lib/*' banner.eval.BANNER train $configfile
	fi
fi

if [ $tasktype == "test" ]; then
	if [ $# -lt 2 ]; then
		echo "configfile missing for testing"
		exit
	fi
	echo "testing"
	configfile=$2
	if [ $# == 3 ] && [ $3 == "log" ]; then
		logfile=`basename $configfile`
		java -cp 'lib/*' BANNER_BioC $configfile data/test_file.xml out.xml > log/$logfile.txt
	else
		java -cp 'lib/*' BANNER_BioC $configfile data/test_file.xml out.xml 
	fi
fi
