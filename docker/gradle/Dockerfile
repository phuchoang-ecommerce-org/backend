FROM eclipse-temurin:21-jdk-jammy

ARG LOCAL_UID=1000
ARG LOCAL_GID=1000

RUN if ! getent group "${LOCAL_GID}" >/dev/null; then groupadd --gid "${LOCAL_GID}" gradle; fi \
    && useradd --uid "${LOCAL_UID}" --gid "${LOCAL_GID}" --create-home --shell /bin/bash gradle \
    && mkdir -p /home/gradle/.gradle /workspace \
    && chown -R "${LOCAL_UID}:${LOCAL_GID}" /home/gradle /workspace

ENV GRADLE_USER_HOME=/home/gradle/.gradle

WORKDIR /workspace

USER gradle

ENTRYPOINT ["./gradlew", "--no-daemon"]
CMD ["--help"]
