#!/bin/bash
pkill -f server.restful.JSIDPlay2Server
java -server -classpath ./${project.artifactId}-${project.version}.jar server.restful.JSIDPlay2Server "$@"

# E.g. MySQL:
# --whatsSIDDatabaseUrl "jdbc:mysql://127.0.0.1:3306/hvscXX?createDatabaseIfNotExist=true&useUnicode=yes&characterEncoding=UTF-8&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC&autoReconnect=true"
# E.g. Monitoring with jvisualvm:
# -Dcom.sun.management.jmxremote.port=9999 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.local.only=false -Djava.rmi.server.hostname=192.168.1.150
