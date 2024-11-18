FROM 416670754337.dkr.ecr.eu-west-2.amazonaws.com/ci-corretto-runtime-21:latest

WORKDIR /opt
COPY /app .
COPY docker_start.sh .
ENV JAVA_OPTS="-Xmx2g -verbose:gc -XX:+UseG1GC"
CMD ["./docker_start.sh"]
