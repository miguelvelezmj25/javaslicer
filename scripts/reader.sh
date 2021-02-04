#!/bin/bash

TRACE=$1

java -Xbootclasspath/a:../lib/utilities-1.2.10.jar:"$HOME"/.m2/repository/org/ow2/asm/asm-all/5.0.3/asm-all-5.0.3.jar \
  -jar ../assembly/traceReader.jar \
  "$TRACE"