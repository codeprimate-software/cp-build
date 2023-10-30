#!/bin/bash
export CP_BUILD_TOOLS_HOME=${CP_BUILD_TOOLS_HOME:-/Users/jblum/cpdev/Codeprimate/projects/cp-core-workspace/cp-build/cp-build-tools}

java -Xdebug -Xrunjdwp:server=y,transport=dt_socket,address=5005,suspend=n -jar \
  ${CP_BUILD_TOOLS_HOME}/target/build-tools-2.0.0-SNAPSHOT.jar
