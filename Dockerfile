FROM eclipse-temurin:21

RUN apt-get update \
    && apt-get install -y \
        curl \
        libxrender1 \
        fontconfig \
        libxtst6 \
        xfonts-75dpi \
        xfonts-base \
        xz-utils

COPY sunbird-entity/src/main/resources/keys /opt/keys
COPY entity-0.0.1-SNAPSHOT.jar /opt/
CMD ["java", "-XX:+PrintFlagsFinal", "-XX:+UnlockExperimentalVMOptions", "-jar", "/opt/entity-0.0.1-SNAPSHOT.jar"]
