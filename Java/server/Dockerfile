# Builds the server in production mode. The build context is the Maven reactor root (the parent directory), because
# the server depends on the aspsms-integration module:
#
#   docker build -f server/Dockerfile .

FROM maven:3-eclipse-temurin-25 AS build
WORKDIR /build
COPY . .
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -pl server -am -Passemble -DskipTests package

FROM eclipse-temurin:25-jre-alpine
RUN addgroup -S idispatch && adduser -S -G idispatch idispatch
WORKDIR /app
COPY --from=build /build/server/target/idispatch-server.jar app.jar
USER idispatch
EXPOSE 8080
# CMD, not ENTRYPOINT, keeps the base image's entrypoint, which adds certificates mounted in /certificates to the JVM's
# trust store when USE_SYSTEM_CA_CERTS is set.
CMD ["java", "-jar", "app.jar"]
