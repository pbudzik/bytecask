#### Bytecask - low latency k/v file database ####

* lightweight - no dependencies, no underlying storages
* embeddable building block, no daemons, no external scripts needed
* storage component for distributed NoSQL databases
* inspired by Bitcask (Erlang)
* Apache 2.0 License

### Key properties: ###

* keys in memory
* low latency write (appending)
* low latency read (direct read)
* automatic compaction/merging
* optimized for Java 1.7

### Example ###

```scala

val db = new Bytecask("/home/foo")
db.put("foo", "some value...")
println(db.get("foo"))
db.delete("foo")
db.destroy()
```
[See the tests](https://github.com/pbudzik/bytecask/blob/master/src/test/scala/bytecask/BasicSuite.scala)

### API ###
```scala

  def put(key: Array[Byte], value: Array[Byte])

  def get(key: Array[Byte]): Option[Array[Byte]]

  def delete(key: Array[Byte]): Option[Array[Byte]]

  def keys(): Set[Array[Byte]]

  def values(): Iterator[Array[Byte]]

  def merge(): Unit

  def close(): Unit

  def destroy(): Unit
```
### Benchmark ####
```
Date: 1/15/2012
Hardware: Intel Core 2 Quad CPU Q6600@2.40GHz
OS: Ubuntu 3.0.0-14-generic x86_64
Java: 1.7.0_02-b13 64-bit with -server -XX:+TieredCompilation -XX:+AggressiveOpts

--- 64K values

sequential put of different 10000 items: time: 4511 ms, throughput: 2216 TPS at 141.55 MB/s
sequential get of different 10000 items: time: 2215 ms, throughput: 4514 TPS at 282.17 MB/s
sequential get of random items 10000 times: time: 2287 ms, throughput: 4372 TPS at 273.28 MB/s
sequential get of the same item 10000 times: time: 2235 ms, throughput: 4474 TPS at 279.64 MB/s
paralell put of different 10000 items: time: 5408 ms, throughput: 1849 TPS at 115.57 MB/s
paralell get of the same item 10000 times: time: 953 ms, throughput: 10493 TPS at 680.82 MB/s
paralell get of random 10000 items: time: 1051 ms, throughput: 9514 TPS at 594.67 MB/s

--- 128 byte values

sequential put of different 10000 items: time: 170 ms, throughput: 58823 TPS at 7.18 MB/s
sequential get of different 10000 items: time: 130 ms, throughput: 76923 TPS at 9.39 MB/s
sequential get of random items 10000 times: time: 194 ms, throughput: 51546 TPS at 6.29 MB/s
sequential get of the same item 10000 times: time: 119 ms, throughput: 84033 TPS at 10.26 MB/s
paralell put of different 10000 items: time: 313 ms, throughput: 31948 TPS at 3.90 MB/s
paralell get of the same item 10000 times: time: 160 ms, throughput: 62500 TPS at 7.63 MB/s
paralell get of random 10000 items: time: 124 ms, throughput: 80645 TPS at 9.84 MB/s
```

