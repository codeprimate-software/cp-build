#!/bin/bash

export CP_BUILD_TOOLS_HOME=${CP_BUILD_TOOLS_HOME:-$HOME/cpdev/Codeprimate/projects/cp-core-workspace/cp-build/cp-build-tools}

java -jar ${CP_BUILD_TOOLS_HOME}/target/build-tools-2.0.0-SNAPSHOT.jar
