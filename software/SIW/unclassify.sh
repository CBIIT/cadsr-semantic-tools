#!/usr/bin/env bash
if [[ -z $1 || -z $2 || -z $3 || -z $4 || -z $5 ]]; then
	echo "Script usage: "
	echo "./unclassify.sh <CSName> <CSVersion> <DB URL> <DB USER> <DB PWD>"
	exit 1;
else 	 
	mvn exec:java@unclassify -Dexec.args="$1 $2" -Ddb.url=$3 -Ddb.user=$4 -Ddb.passwd=$5
fi
