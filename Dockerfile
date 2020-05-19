FROM adoptopenjdk/openjdk14:debianslim-jre
RUN mkdir /opt/app
COPY ./build/install/banana /opt/app
COPY ./src/main/conf /opt/app
ENV CONFIG_FILE /opt/app/config.json
EXPOSE 8081
CMD ["./opt/app/bin/banana", "run", "com.victor.banana.verticles.SupervisorVerticle"]