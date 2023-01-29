#!/bin/bash
pkill -f server.restful.JSIDPlay2Server
java -server -classpath ./${project.artifactId}-${project.version}.jar server.restful.JSIDPlay2Server "$@"

# E.g. Set language
# -Duser.language=en
# E.g. MySQL:
# --whatsSIDDatabaseUrl "jdbc:mysql://127.0.0.1:3306/hvscXX?createDatabaseIfNotExist=true&useUnicode=yes&characterEncoding=UTF-8&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC&autoReconnect=true"
# E.g. RTMP and HLS video support for upload/download videos (for example NGINX server):
# -Djsidplay2.rtmp.upload.url=rtmp://127.0.0.1/live -Djsidplay2.rtmp.external.download.url=rtmp://haendel.ddns.net/live -Djsidplay2.hls.external.download.url=http://haendel.ddns.net:90/hls
# E.g. adjust anchor tags in documentation:
# -Djsidplay2.base.url=https://haendel.ddns.net:8443
# E.g. Tomcat monitoring with jvisualvm:
# -Dcom.sun.management.jmxremote.port=9999 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.local.only=false -Djava.rmi.server.hostname=192.168.1.150
