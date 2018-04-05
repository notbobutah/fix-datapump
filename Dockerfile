# ---- Base Java ----
FROM docker-registry.neovest.com/neohc/java:8 AS base

# ---- Dependencies ----
FROM docker-registry.neovest.com/neohc/maven:35 AS dependencies
WORKDIR /usr/app/src
COPY . .

# ---- Test and Build ----
FROM dependencies as build
RUN mvn -T 4 package

# ---- Release ----

#TODO create base java runtime image
FROM base AS release
# copy production node_modules
RUN mkdir /deploy
WORKDIR /deploy
COPY --from=build /usr/app/src/target /deploy/
CMD ["java",  "-jar","/deploy/fix-datapump.jar"]
