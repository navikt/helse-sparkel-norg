FROM gcr.io/distroless/java21-debian12:nonroot

ENV TZ="Europe/Oslo"
ENV JDK_JAVA_OPTIONS='-XX:MaxRAMPercentage=75'

WORKDIR /app

COPY build/install/app/ /app/
ENTRYPOINT ["java", "-cp", "/app/lib/*", "no.nav.helse.sparkel.norg.AppKt"]
CMD []
