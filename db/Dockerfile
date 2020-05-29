FROM flyway/flyway:6.4
COPY ./migrations /flyway/sql
CMD ["-url=jdbc:postgresql://192.168.2.109:5432/postgres", "-user=postgres", "-password=P@ssw0rd", "-schemas=public,keycloak", "-connectRetries=60", "migrate"]