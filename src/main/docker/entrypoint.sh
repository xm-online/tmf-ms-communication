#!/usr/bin/env bash
secrets=`ls /run/secrets/ 2>/dev/null |egrep -v '.*_FILE$'`
for s in $secrets
do
    echo "set env $s"
    export "$s"="$(cat /run/secrets/$s)"
done
exec java ${JAVA_OPTS} -Xmx$XMX -Djava.security.egd=file:/dev/./urandom -jar /app.war
