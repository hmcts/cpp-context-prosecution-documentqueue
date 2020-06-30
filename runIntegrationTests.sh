#!/usr/bin/env bash

#The prerequisite for this script is that vagrant is running
#Script that runs, liquibase, deploys wars and runs integration tests

VAGRANT_DIR=${VAGRANT_DIR:?"Please export VAGRANT_DIR environment variable to point at atcm-vagrant"}
WILDFLY_DEPLOYMENT_DIR="${VAGRANT_DIR}/deployments"
CONTEXT_NAME=documentqueue
FRAMEWORK_VERSION=7.0.6
EVENT_STORE_VERSION=7.0.4
FRAMEWORK_LIBRARIES_VERSION=7.0.8

#fail script on error
set -e

function usage() {
  cat <<-END

END
}

function buildWars {
  echo
  echo "Building wars."
  mvn clean install -nsu
  echo "\n"
  echo "Finished building wars"
}

function startVagrant {
  echo "Starting Vagrant machine from " $VAGRANT_DIR
  export VAGRANT_CWD=$VAGRANT_DIR
  output=$(vagrant status)
  regex="[^$'\n']default\s+running"
  if [[ ! $output =~ $regex ]] ; then
    vagrant up;
  fi
}

function deleteWars {
  echo
  echo "Deleting wars from $WILDFLY_DEPLOYMENT_DIR....."
  rm -rf $WILDFLY_DEPLOYMENT_DIR/*.war
  rm -rf $WILDFLY_DEPLOYMENT_DIR/*.deployed
}

function deployWars {
  rm -rf $WILDFLY_DEPLOYMENT_DIR/*.undeployed
  find ./${CONTEXT_NAME}-service -name "*.war"  -exec cp {} $WILDFLY_DEPLOYMENT_DIR \;
  echo "Copied wars to $WILDFLY_DEPLOYMENT_DIR"
}


function healthCheck {
  CONTEXT=()
  CONTEXT=("$CONTEXT_NAME-service"  "$CONTEXT_NAME-command-api"  "$CONTEXT_NAME-command-handler" "${CONTEXT_NAME}-query-api"  "${CONTEXT_NAME}-event-listener" "${CONTEXT_NAME}-event-processor")

  CONTEXT_COUNT=${#CONTEXT[@]}
  TIMEOUT=90
  RETRY_DELAY=5
  START_TIME=$(date +%s)

  echo "Start time is $START_TIME"
  echo "Starting health check on ${CONTEXT[@]}"
  echo "Conducting health check on $CONTEXT_COUNT contexts"
  echo "TIMEOUT is $TIMEOUT Seconds"
  echo "RETRY_DELAY $RETRY_DELAY Seconds"

  while [ true ]
  do
      DEPLOYED=0

      for i in ${CONTEXT[@]}
      do
        CHECK_STRING="curl --connect-timeout 1 -s http://localhost:8080/$i/internal/metrics/ping"
        echo -n $CHECK_STRING
        CHECK=$( $CHECK_STRING )  >/dev/null 2>&1
        echo $CHECK | grep pong >/dev/null 2>&1 && DEPLOYED=$((DEPLOYED + 1))
        echo $CHECK | grep pong >/dev/null 2>&1 && echo " pong" || echo " DOWN"
      done

      echo
      echo RESULT:  ${DEPLOYED} out of  ${CONTEXT_COUNT} wars came back with pong

      [ "${DEPLOYED}" -eq "${CONTEXT_COUNT}" ] && break

      TIME_NOW=$(date +%s)
      TIME_ELAPSED=$(( $TIME_NOW - $START_TIME ))

      echo "Start time is $START_TIME"
      echo "Time Now is $TIME_NOW"
      echo "Time elapsed is $TIME_ELAPSED"


     [ "${TIME_ELAPSED}" -gt "${TIMEOUT}" ] && exit
      sleep $RETRY_DELAY

  done
}

function integrationTests {
  echo
  echo "Running Integration Tests"
  mvn -f ${CONTEXT_NAME}-integration-test/pom.xml clean integration-test -P${CONTEXT_NAME}-integration-test
  echo "Finished running Integration Tests"
}

function runEventLogLiquibase() {
    echo "Running event log Liquibase"
    mvn org.apache.maven.plugins:maven-dependency-plugin:3.0.1:copy -DoutputDirectory=target -Dartifact=uk.gov.justice.event-store:event-repository-liquibase:${EVENT_STORE_VERSION}:jar
    java -jar target/event-repository-liquibase-${EVENT_STORE_VERSION}.jar --url=jdbc:postgresql://localhost:5432/${CONTEXT_NAME}eventstore --username=${CONTEXT_NAME} --password=${CONTEXT_NAME} --logLevel=info update
    echo "Finished running event log liquibase"
}

function runEventLogAggregateSnapshotLiquibase() {
    echo "Running aggregate snapshot liquibase"
    mvn org.apache.maven.plugins:maven-dependency-plugin:3.0.1:copy -DoutputDirectory=target -Dartifact=uk.gov.justice.event-store:aggregate-snapshot-repository-liquibase:${EVENT_STORE_VERSION}:jar
    java -jar target/aggregate-snapshot-repository-liquibase-${EVENT_STORE_VERSION}.jar --url=jdbc:postgresql://localhost:5432/${CONTEXT_NAME}eventstore --username=${CONTEXT_NAME} --password=${CONTEXT_NAME} --logLevel=info update
    echo "Finished running aggregate snapshot liquibase"
}

function runEventBufferLiquibase() {
    echo "Running event buffer liquibase"
    mvn org.apache.maven.plugins:maven-dependency-plugin:3.0.1:copy -DoutputDirectory=target -Dartifact=uk.gov.justice.event-store:event-buffer-liquibase:${EVENT_STORE_VERSION}:jar
    java -jar target/event-buffer-liquibase-${EVENT_STORE_VERSION}.jar --url=jdbc:postgresql://localhost:5432/${CONTEXT_NAME}viewstore --username=${CONTEXT_NAME} --password=${CONTEXT_NAME} --logLevel=info update
    echo "finished running event buffer liquibase"
}

function runViewStoreLiquibase {
    echo "Running view store liquibase"
    mvn -f ${CONTEXT_NAME}-viewstore/${CONTEXT_NAME}-viewstore-liquibase/pom.xml -Dliquibase.url=jdbc:postgresql://localhost:5432/${CONTEXT_NAME}viewstore -Dliquibase.username=${CONTEXT_NAME} -Dliquibase.password=${CONTEXT_NAME} -Dliquibase.logLevel=info resources:resources liquibase:update
    echo "Finished executing vew store liquibase"
}

function runSystemLiquibase {
    echo "Running system liquibase"
    mvn org.apache.maven.plugins:maven-dependency-plugin:3.0.1:copy -DoutputDirectory=target -Dartifact=uk.gov.justice.services:framework-system-liquibase:${FRAMEWORK_VERSION}:jar
    java -jar target/framework-system-liquibase-${FRAMEWORK_VERSION}.jar --url=jdbc:postgresql://localhost:5432/${CONTEXT_NAME}system --username=${CONTEXT_NAME} --password=${CONTEXT_NAME} --logLevel=info update
    echo "Finished executing system liquibase"
}

function runEventTrackingLiquibase {
    echo "Running event tracking liquibase"
    mvn org.apache.maven.plugins:maven-dependency-plugin:3.0.1:copy -DoutputDirectory=target -Dartifact=uk.gov.justice.event-store:event-tracking-liquibase:${EVENT_STORE_VERSION}:jar
    java -jar target/event-tracking-liquibase-${EVENT_STORE_VERSION}.jar --url=jdbc:postgresql://localhost:5432/${CONTEXT_NAME}viewstore --username=${CONTEXT_NAME} --password=${CONTEXT_NAME} --logLevel=info update
    echo "Finished executing event tracking liquibase"
}

function runFileServiceLiquibase {
    echo "Running File Service liquibase"
    mvn org.apache.maven.plugins:maven-dependency-plugin:3.0.1:copy -DoutputDirectory=target -Dartifact=uk.gov.justice.services:file-service-liquibase:${FRAMEWORK_LIBRARIES_VERSION}:jar
    java -jar target/file-service-liquibase-${FRAMEWORK_LIBRARIES_VERSION}.jar --url=jdbc:postgresql://localhost:5432/fileservice --username=fileservice --password=fileservice --logLevel=debug ${LIQUIBASE_ACTION}
    echo "Finished executing File Service liquibase"
}

function deployWiremock() {
    mvn org.apache.maven.plugins:maven-dependency-plugin:2.10:copy -DoutputDirectory=$WILDFLY_DEPLOYMENT_DIR -Dartifact=uk.gov.justice.services:wiremock-service:1.1.0:war
}

function runLiquibase {
  runEventLogLiquibase
  runEventLogAggregateSnapshotLiquibase
  runEventBufferLiquibase
  runViewStoreLiquibase
  runSystemLiquibase
  runEventTrackingLiquibase
  echo "All liquibase update scripts run"
}

function buildDeployAndTest {
  buildWars
  deployAndTest
}

function deployAndTest {
  deleteWars
  deployWiremock
  startVagrant
  runLiquibase
  deployWars
  healthCheck
  integrationTests
}

buildDeployAndTest
