#!/bin/bash
java -classpath ./${project.artifactId}-${project.version}-ui.jar ui.tools.SIDBlasterTool  "$@"
