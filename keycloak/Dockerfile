FROM jboss/keycloak:10.0.2
COPY ./conf.cli /tmp/conf.cli
COPY ./conf2.cli /tmp/conf2.cli
COPY ./realm-export.json /tmp/realm-export.json
COPY ./my-theme /opt/jboss/keycloak/themes/my-theme
# COPY ./standalone-ha.xml /opt/jboss/keycloak/standalone/configuration/standalone-ha.xml
ENV KEYCLOAK_IMPORT /tmp/realm-export.json
RUN ./opt/jboss/keycloak/bin/jboss-cli.sh --file=/tmp/conf.cli
RUN ./opt/jboss/keycloak/bin/jboss-cli.sh --file=/tmp/conf2.cli
RUN rm -rf /opt/jboss/keycloak/standalone/configuration/standalone_xml_history/current