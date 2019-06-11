#!/usr/bin/env bash
export UNCLASSIFY_ARGS=`echo "$@"`
if [[ -z $1 || -z $2 ]]; then
	echo "Please provide the CS Name followed by the CS version in the below format"
	echo "./unclassify.sh <CSName> <CSVersion>"
	exit 1;
else 	 
	mvn exec:java@unclassify -Dexec.args="$UNCLASSIFY_ARGS"
fi
