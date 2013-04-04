#### Bytecask - low latency k/v file database ####

* lightweight - no dependencies, no underlying storages
* embeddable building block, no daemons, no external scripts needed
* storage component for distributed NoSQL databases
* inspired by [Bitcask](https://github.com/basho/bitcask) (Erlang)
* optional fast compression (backed by [snappy-java](http://code.google.com/p/snappy-java/))
* optional Radix Tree support for keys
* optional eviction and expiration
* optimized for Java 1.7
* Apache 2.0 License

### Key properties: ###

* keys in memory
* low latency write (appending)
* low latency read (direct read)
* automatic compaction/merging

### Install ###

**sbt**

Dependencies:

    "com.github.bytecask" %% "bytecask" % "1.0-SNAPSHOT"

Repos:

    "sonatype-snapshots" at "https://oss.sonatype.org/content/groups/public"

### Example ###

```scala

val db = new Bytecask("/home/foo")
db.put("foo", "some value...")
println(db.get("foo"))
db.delete("foo")
db.destroy()

//other methods of initialization:

val db = new Bytecask("/home/foo") with Compression
val db = new Bytecask("/home/foo") with JmxSupport
val db = new Bytecask("/home/foo", prefixedKeys=true) with Compression with JmxSupport
val db = new Bytecask("/home/foo") with Eviction { val maxCount = 3 }
val db = new Bytecask("/home/foo") with Expiration { val ttl = 15 }
...

```
[See the tests](https://github.com/pbudzik/bytecask/blob/master/src/test/scala/com/github/bytecask/BasicSuite.scala)

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
### Glossary ###

* Index (Keydir) - keys are kept in memory and point to entries in data files
* Data files - files that contain the actual data
* Merge - depending on update/delete intensity more and more space is occupied, so
merge operation compacts data as well as merges files into one
* Hint files - when files are merged to one file a "hint file" is produced out of the new file being
a persisted index, so later the index can be rebuilt w/o processing the data file (however anyway
can be built)

### Benchmark ####

```
Date: 1/22/2012
Hardware: Intel Core 2 Quad CPU Q6600@2.40GHz
OS: Ubuntu 3.0.0-15-generic x86_64
Java: 1.7.0_02-b13 64-bit with -server -XX:+TieredCompilation -XX:+AggressiveOpts

--- 64K values

sequential put of different 10000 items: time: 4349 ms, 1 op: 0.4349 ms, throughput: 2299 TPS at 143.71 MB/s, total: 625.00 MB
sequential get of different 10000 items: time: 1971 ms, 1 op: 0.1971 ms, throughput: 5073 TPS at 317.10 MB/s, total: 625.00 MB
sequential get of random items 10000 times: time: 2152 ms, 1 op: 0.2152 ms, throughput: 4646 TPS at 290.43 MB/s, total: 625.00 MB
sequential get of the same item 10000 times: time: 1939 ms, 1 op: 0.1939 ms, throughput: 5157 TPS at 322.33 MB/s, total: 625.00 MB

concurrent put of different 10000 items: time: 5264 ms, 1 op: 0.5264 ms, throughput: 1899 TPS at 118.73 MB/s, total: 625.00 MB
concurrent get of the same item 10000 times: time: 1058 ms, 1 op: 0.1058 ms, throughput: 9451 TPS at 590.74 MB/s, total: 625.00 MB
concurrent get of random 10000 items: time: 3750 ms, 1 op: 0.375 ms, throughput: 2666 TPS at 166.67 MB/s, total: 625.00 MB

--- 128 byte values

sequential put of different 10000 items: time: 171 ms, 1 op: 0.0171 ms, throughput: 58479 TPS at 7.14 MB/s, total: 1.22 MB
sequential get of different 10000 items: time: 132 ms, 1 op: 0.0132 ms, throughput: 75757 TPS at 9.25 MB/s, total: 1.22 MB
sequential get of random items 10000 times: time: 109 ms, 1 op: 0.0109 ms, throughput: 91743 TPS at 11.20 MB/s, total: 1.22 MB
sequential get of the same item 10000 times: time: 96 ms, 1 op: 0.0096 ms, throughput: 104166 TPS at 12.72 MB/s, total: 1.22 MB

concurrent put of different 10000 items: time: 278 ms, 1 op: 0.0278 ms, throughput: 35971 TPS at 4.39 MB/s, total: 1.22 MB
concurrent get of the same item 10000 times: time: 96 ms, 1 op: 0.0096 ms, throughput: 104166 TPS at 12.72 MB/s, total: 1.22 MB
concurrent get of random 10000 items: time: 63 ms, 1 op: 0.0063 ms, throughput: 158730 TPS at 19.38 MB/s, total: 1.22 MB
```

You can build a jar in sbt:

    > assembly

and issue:

    bin/benchmark.sh

### Collaboration ###

**Reporting bugs and asking for features**

You can use github issue tracker to report bugs or to ask for new features [here](https://github.com/pbudzik/bytecask/issues)

**Submit patches**

Patches are gladly welcome from their original author. Along with any patches, please state that the patch is your original work
and that you license the work to the Bytecask project under the Apache 2.0 or a compatible license.

To propose a patch, fork the project and send a pull request via github.
