# ---------- BUILD STAGE ----------
FROM maven:3.9.8-eclipse-temurin-21 AS build

WORKDIR /workspace
ARG SERVICE_NAME

COPY pom.xml ./
COPY shopscale-common/pom.xml shopscale-common/pom.xml
COPY shopscale-oidc/pom.xml shopscale-oidc/pom.xml
COPY config-server/pom.xml config-server/pom.xml
COPY discovery-service/pom.xml discovery-service/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY product-service/pom.xml product-service/pom.xml
COPY order-service/pom.xml order-service/pom.xml
COPY inventory-service/pom.xml inventory-service/pom.xml
COPY notification-service/pom.xml notification-service/pom.xml
COPY price-service/pom.xml price-service/pom.xml
COPY cart-service/pom.xml cart-service/pom.xml

RUN --mount=type=cache,target=/root/.m2 \
    mvn -q -B -e -DskipTests -pl "${SERVICE_NAME}" -am dependency:go-offline

COPY . .

RUN --mount=type=cache,target=/root/.m2 \
    mvn -q -B -e -DskipTests -pl "${SERVICE_NAME}" -am package

# ---------- RUN STAGE ----------
FROM eclipse-temurin:21-jre

RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
RUN addgroup --system spring && adduser --system --ingroup spring spring

WORKDIR /app
ARG SERVICE_NAME
COPY --from=build /workspace/${SERVICE_NAME}/target/*.jar app.jar
RUN chown spring:spring app.jar

USER spring:spring
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
