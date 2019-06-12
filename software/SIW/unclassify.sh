#!/usr/bin/env bash
if [[ -z $1 || -z $2 || -z $3 || -z $4 || -z $5 || -z $6 ]]; then
	echo "Script usage: "
	echo "./unclassify.sh <CSName> <CSVersion> <TIER> <DB URL> <DB USER> <DB PWD>"
	exit 1;
else 	 
	mvn exec:java@unclassify -Dexec.args="$1 $2" -Dtier=$3 -Ddb.url=$4 -Ddb.user=$5 -Ddb.passwd=$6
fi
