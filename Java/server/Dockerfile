# Builds the server in production mode from the sources in this directory, which are all the server needs:
#
#   docker build .

FROM maven:3-eclipse-temurin-25 AS build
WORKDIR /build
COPY . .
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -pl app -am -Pproduction -DskipTests package

FROM eclipse-temurin:25-jre-alpine
RUN addgroup -S idispatch && adduser -S -G idispatch idispatch
WORKDIR /app
COPY --from=build /build/app/target/idispatch-server.jar app.jar
USER idispatch
EXPOSE 8080
# CMD, not ENTRYPOINT, keeps the base image's entrypoint, which adds certificates mounted in /certificates to the JVM's
# trust store when USE_SYSTEM_CA_CERTS is set.
CMD ["java", "-jar", "app.jar"]
