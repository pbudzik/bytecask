set CP=target/bytecask-assembly-1.0-SNAPSHOT.jar

java -Droot-level=OFF -XX:+TieredCompilation -XX:+AggressiveOpts -cp %CP% benchmark.Benchmark

