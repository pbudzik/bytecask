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
Date: 1/21/2012
Hardware: Intel Core 2 Quad CPU Q6600@2.40GHz
OS: Ubuntu 3.0.0-15-generic x86_64
Java: 1.7.0_02-b13 64-bit with -server -XX:+TieredCompilation -XX:+AggressiveOpts

--- 64K values

sequential put of different 10000 items: time: 5642 ms, 1 op: 0.5642 ms, throughput: 1772 TPS at 110.78 MB/s, total: 625.00 MB
sequential get of different 10000 items: time: 2094 ms, 1 op: 0.2094 ms, throughput: 4775 TPS at 298.47 MB/s, total: 625.00 MB
sequential get of random items 10000 times: time: 1864 ms, 1 op: 0.1864 ms, throughput: 5364 TPS at 335.30 MB/s, total: 625.00 MB
sequential get of the same item 10000 times: time: 1746 ms, 1 op: 0.1746 ms, throughput: 5727 TPS at 357.96 MB/s, total: 625.00 MB

concurrent put of different 10000 items: time: 6437 ms, 1 op: 0.6437 ms, throughput: 1553 TPS at 97.09 MB/s, total: 625.00 MB
concurrent get of the same item 10000 times: time: 843 ms, 1 op: 0.0843 ms, throughput: 11862 TPS at 741.40 MB/s, total: 625.00 MB
concurrent get of random 10000 items: time: 6414 ms, 1 op: 0.6414 ms, throughput: 1559 TPS at 97.44 MB/s, total: 625.00 MB

--- 128 byte values

sequential put of different 10000 items: time: 164 ms, 1 op: 0.0164 ms, throughput: 60975 TPS at 7.44 MB/s, total: 1.22 MB
sequential get of different 10000 items: time: 161 ms, 1 op: 0.0161 ms, throughput: 62111 TPS at 7.58 MB/s, total: 1.22 MB
sequential get of random items 10000 times: time: 126 ms, 1 op: 0.0126 ms, throughput: 79365 TPS at 9.69 MB/s, total: 1.22 MB
sequential get of the same item 10000 times: time: 132 ms, 1 op: 0.0132 ms, throughput: 75757 TPS at 9.25 MB/s, total: 1.22 MB

concurrent put of different 10000 items: time: 258 ms, 1 op: 0.0258 ms, throughput: 38759 TPS at 4.73 MB/s, total: 1.22 MB
concurrent get of the same item 10000 times: time: 126 ms, 1 op: 0.0126 ms, throughput: 79365 TPS at 9.69 MB/s, total: 1.22 MB
concurrent get of random 10000 items: time: 94 ms, 1 op: 0.0094 ms, throughput: 106382 TPS at 12.99 MB/s, total: 1.22 MB
```

