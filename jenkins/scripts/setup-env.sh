#!/bin/bash

ONT=$1
DIR=/home/hohndor/aberowl-meta/ontologies/

mkdir $DIR/$ONT
chmod 777 $DIR/$ONT
mkdir $DIR/$ONT/new
mkdir $DIR/$ONT/release
mkdir $DIR/$ONT/live
ln -s /home/hohndor/aberowl-meta/aberowl-server/onts/${ONT}_*.ont $DIR/$ONT/release
/usr/local/bin/groovy GenConfig $ONT $DIR/$ONT/
chmod 777 $DIR/$ONT/*
