#!/bin/bash

CP=../lib/utilities-1.2.10.jar:$1
PROGRAM=$2
ARG1=$3

java -Xbootclasspath/a:../lib/utilities-1.2.10.jar:"$HOME"/.m2/repository/org/ow2/asm/asm-all/5.0.3/asm-all-5.0.3.jar \
  -javaagent:../assembly/tracer.jar=tracefile:test.trace \
  -cp "$CP" \
  "$PROGRAM" "$ARG1"
