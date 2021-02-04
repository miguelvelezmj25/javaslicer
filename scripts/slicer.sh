#!/bin/bash

TRACE=$1
CRITERIA=$2

java -Xmx12g -Xbootclasspath/a:"$HOME"/.m2/repository/org/ow2/asm/asm-all/5.0.3/asm-all-5.0.3.jar:../lib/utilities-1.2.10.jar:"$HOME"/Documents/programming/java/projects/javaslicer/javaslicer-common/target/classes:"$HOME"/.m2/repository/commons-cli/commons-cli/1.4/commons-cli-1.4.jar \
  -jar ../assembly/slicer.jar \
  -p "$TRACE" \
  "$CRITERIA"
