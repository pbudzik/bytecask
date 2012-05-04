#!/bin/sh

CP=target/bytecask-assembly-1.0-SNAPSHOT.jar

java -Droot-level=OFF -server -XX:+TieredCompilation -XX:+AggressiveOpts -cp $CP com.github.bytecask.Benchmark

