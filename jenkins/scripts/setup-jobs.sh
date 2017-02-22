#!/bin/bash

ONT=$1

cat template.yaml | sed s/_ONT_/$ONT/g > ../jobs/$ONT.yaml
/usr/bin/jenkins-jobs --conf /home/hohndor/aberowl-meta/jenkins/config/jenkins_jobs.ini update ../jobs/$ONT.yaml
