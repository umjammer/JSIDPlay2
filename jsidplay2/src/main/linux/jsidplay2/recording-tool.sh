#!/bin/bash
pkill -f ui.tools.RecordingTool
java -server -classpath ./${project.artifactId}-${project.version}.jar ui.tools.RecordingTool  "$@"
