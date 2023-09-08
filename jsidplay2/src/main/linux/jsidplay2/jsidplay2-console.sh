#!/bin/bash
export LD_LIBRARY_PATH=.
java -classpath ./${project.artifactId}-${project.version}-ui.jar sidplay.ConsolePlayer "$@"
