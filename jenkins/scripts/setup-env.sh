#!/bin/bash

ONT=$1
DIR=/home/hohndor/aberowl-meta/ontologies/

mkdir $DIR/$ONT
mkdir $DIR/$ONT/new
mkdir $DIR/$ONT/release
/usr/local/bin/groovy GenConfig $ONT $DIR/$ONT/
