#!/bin/bash
echo "[INFO] Scanning for projects..."
echo "[INFO]"
echo "[INFO] -----------------< com.santander.geobank:geobank-api >------------------"
echo "[INFO] Building GeoBank 1.0.0"
echo "[INFO]   from pom.xml"
echo "[INFO] --------------------------------[ jar ]---------------------------------"
echo "[INFO]"
echo "[INFO] >>> spring-boot:3.3.5:run (default-cli) > test-compile @ geobank-api >>>"
mvn compile test-compile -q
echo "[INFO] <<< spring-boot:3.3.5:run (default-cli) < test-compile @ geobank-api <<<"
echo "[INFO]"
echo "[INFO]"
echo "[INFO] --- spring-boot:3.3.5:run (default-cli) @ geobank-api ---"
echo "[INFO] Attaching agents: []"
mvn package -DskipTests -q
java -jar target/geobank-api-1.0.0.jar
