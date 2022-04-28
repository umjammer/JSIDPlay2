#!/bin/bash
export LD_LIBRARY_PATH=.
java -jar ./${project.artifactId}_console-${project.version}.jar "$@"
