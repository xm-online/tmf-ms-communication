FROM jbsukraine/oracle-jre:8

EXPOSE 8701 5701/udp

ENV XMX=512m \
    SPRING_OUTPUT_ANSI_ENABLED=ALWAYS \
    JHIPSTER_SLEEP=0 \
    JAVA_OPTS="-Xms128m -Dcom.sun.management.jmxremote=true -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.local.only=false -Dcom.sun.management.jmxremote.rmi.port=19999 -Dcom.sun.management.jmxremote.port=19999"
ADD src/main/docker/entrypoint.sh /

RUN chmod 750 /entrypoint.sh

ENTRYPOINT /entrypoint.sh

ADD build/libs/*.war /app.war
