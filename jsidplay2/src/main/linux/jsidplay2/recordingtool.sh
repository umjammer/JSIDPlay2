#!/bin/bash
java -classpath ./${project.artifactId}-${project.version}.jar ui.tools.RecordingTool  "$@"