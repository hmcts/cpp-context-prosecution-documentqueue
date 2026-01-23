#!/usr/bin/env bash

CONTEXT_NAME=documentqueue
FRAMEWORK_LIBRARIES_VERSION=$(mvn help:evaluate -Dexpression=framework-libraries.version -q -DforceStdout)
FRAMEWORK_VERSION=$(mvn help:evaluate -Dexpression=framework.version -q -DforceStdout)
EVENT_STORE_VERSION=$(mvn help:evaluate -Dexpression=event-store.version -q -DforceStdout)


LIQUIBASE_ACTION=update
#LIQUIBASE_ACTION=dropAll

function integrationTests {
  echo
  echo "Running Integration Tests"
  mvn -f ${CONTEXT_NAME}-integration-test/pom.xml clean integration-test -P${CONTEXT_NAME}-integration-test
  echo "Finished running Integration Tests"
}

function runEventLogLiquibase() {
    echo "Running event log Liquibase"
    mvn org.apache.maven.plugins:maven-dependency-plugin:3.0.1:copy -DoutputDirectory=target -Dartifact=uk.gov.justice.event-store:event-repository-liquibase:${EVENT_STORE_VERSION}:jar
    java -jar target/event-repository-liquibase-${EVENT_STORE_VERSION}.jar --url=jdbc:postgresql://localhost:5432/${CONTEXT_NAME}eventstore --username=${CONTEXT_NAME} --password=${CONTEXT_NAME} --logLevel=info ${LIQUIBASE_ACTION}
    echo "Finished running event log liquibase"
}

function runEventLogAggregateSnapshotLiquibase() {
    echo "Running aggregate snapshot liquibase"
    mvn org.apache.maven.plugins:maven-dependency-plugin:3.0.1:copy -DoutputDirectory=target -Dartifact=uk.gov.justice.event-store:aggregate-snapshot-repository-liquibase:${EVENT_STORE_VERSION}:jar
    java -jar target/aggregate-snapshot-repository-liquibase-${EVENT_STORE_VERSION}.jar --url=jdbc:postgresql://localhost:5432/${CONTEXT_NAME}eventstore --username=${CONTEXT_NAME} --password=${CONTEXT_NAME} --logLevel=info ${LIQUIBASE_ACTION}
    echo "Finished running aggregate snapshot liquibase"
}

function runEventBufferLiquibase() {
    echo "Running event buffer liquibase"
    mvn org.apache.maven.plugins:maven-dependency-plugin:3.0.1:copy -DoutputDirectory=target -Dartifact=uk.gov.justice.event-store:event-buffer-liquibase:${EVENT_STORE_VERSION}:jar
    java -jar target/event-buffer-liquibase-${EVENT_STORE_VERSION}.jar --url=jdbc:postgresql://localhost:5432/${CONTEXT_NAME}viewstore --username=${CONTEXT_NAME} --password=${CONTEXT_NAME} --logLevel=info ${LIQUIBASE_ACTION}
    echo "finished running event buffer liquibase"
}

function runViewStoreLiquibase {
    echo "Running view store liquibase"
    mvn -f ${CONTEXT_NAME}-viewstore/${CONTEXT_NAME}-viewstore-liquibase/pom.xml -Dliquibase.url=jdbc:postgresql://localhost:5432/${CONTEXT_NAME}viewstore -Dliquibase.username=${CONTEXT_NAME} -Dliquibase.password=${CONTEXT_NAME} -Dliquibase.logLevel=info resources:resources liquibase:${LIQUIBASE_ACTION}
    echo "Finished executing vew store liquibase"
}

function runSystemLiquibase {
    echo "Running system liquibase"
    mvn org.apache.maven.plugins:maven-dependency-plugin:3.0.1:copy -DoutputDirectory=target -Dartifact=uk.gov.justice.services:framework-system-liquibase:${FRAMEWORK_VERSION}:jar
    java -jar target/framework-system-liquibase-${FRAMEWORK_VERSION}.jar --url=jdbc:postgresql://localhost:5432/${CONTEXT_NAME}system --username=${CONTEXT_NAME} --password=${CONTEXT_NAME} --logLevel=info ${LIQUIBASE_ACTION}
    echo "Finished executing system liquibase"
}

function runEventTrackingLiquibase {
    echo "Running event tracking liquibase"
    mvn org.apache.maven.plugins:maven-dependency-plugin:3.0.1:copy -DoutputDirectory=target -Dartifact=uk.gov.justice.event-store:event-tracking-liquibase:${EVENT_STORE_VERSION}:jar
    java -jar target/event-tracking-liquibase-${EVENT_STORE_VERSION}.jar --url=jdbc:postgresql://localhost:5432/${CONTEXT_NAME}viewstore --username=${CONTEXT_NAME} --password=${CONTEXT_NAME} --logLevel=info ${LIQUIBASE_ACTION}
    echo "Finished executing event tracking liquibase"
}

function runFileServiceLiquibase {
    echo "Running File Service liquibase"
    mvn org.apache.maven.plugins:maven-dependency-plugin:3.0.1:copy -DoutputDirectory=target -Dartifact=uk.gov.justice.services:file-service-liquibase:${FRAMEWORK_LIBRARIES_VERSION}:jar
    java -jar target/file-service-liquibase-${FRAMEWORK_LIBRARIES_VERSION}.jar --url=jdbc:postgresql://localhost:5432/fileservice --username=fileservice --password=fileservice --logLevel=debug ${LIQUIBASE_ACTION}
    echo "Finished executing File Service liquibase"
}

function runLiquibase {

  echo "Running liquibase ${LIQUIBASE_ACTION}"
  runEventLogLiquibase
  runEventLogAggregateSnapshotLiquibase
  runEventBufferLiquibase
  runViewStoreLiquibase
  runSystemLiquibase
  runEventTrackingLiquibase
  #runFileServiceLiquibase
  echo "All liquibase ${LIQUIBASE_ACTION} scripts run"
}

runLiquibase
