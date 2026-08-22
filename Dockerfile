FROM payara/micro:6.2025.11-jdk17
COPY FIMACOR.war /opt/payara/deployments/ROOT.war
EXPOSE 8080
