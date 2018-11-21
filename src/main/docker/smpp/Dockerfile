FROM alpine:3.8
RUN cd /home &&\
apk --update add openjdk7-jre &&\
wget -O /home/file.zip https://drive.google.com/uc?id=0B7YOzDQMt_TmaWN4UkJFa3M0Rkk &&\
unzip ./file.zip &&\
chmod a+x /home/SMPPSim/startsmppsim.sh
WORKDIR /home/SMPPSim/
EXPOSE 2775
ENTRYPOINT /home/SMPPSim/startsmppsim.sh

