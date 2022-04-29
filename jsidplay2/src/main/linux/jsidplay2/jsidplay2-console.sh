#!/bin/bash
export LD_LIBRARY_PATH=.
java -jar ./${project.artifactId}-console-${project.version}.jar "$@"
