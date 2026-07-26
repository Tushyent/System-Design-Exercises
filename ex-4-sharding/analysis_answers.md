# MongoDB Horizontal Sharding - Analysis and Lab Report Answers

## 1. Role of Config Server, Shard Server, and mongos Router
* **Config Server (`configsvr`)**: Stores the metadata and configuration settings for the sharded cluster. It keeps a directory of which chunks of data are located on which shard. When a router needs to find data, it caches the mapping from the config server.
* **Shard Server (`shardsvr`)**: Stores the actual application data. In a production environment, each shard is deployed as a replica set to ensure high availability and redundancy. Data is partitioned into chunks and distributed across these shards.
* **Query Router (`mongos`)**: Acts as a routing service and the entry point for the application. It routes read and write operations from the application to the appropriate shard(s) by querying the metadata cached from the config server. The application never connects directly to the shards.

## 2. Justification for using RollNo as the Shard Key (and Tradeoffs)
We selected `{ RollNo: 1 }` (Range-based sharding) for the `Student` collection.
* **Justification**: A RollNo (Roll Number) is highly targeted in queries. Most queries for a student will be a point query based on their RollNo (e.g., fetching a student's profile or grades). Using a range-based shard key on RollNo allows these targeted queries to be routed to a single shard (Targeted Query) instead of broadcasting to all shards (Scatter-Gather Query). It also allows range queries (e.g., getting students with RollNos 100-200) to be extremely efficient.
* **Tradeoff - Monotonically Increasing Key Risk (Hotspotting)**: Roll numbers are often generated sequentially over time (e.g., Year + incrementing ID). Because we used Range-based sharding on a monotonically increasing value, all *newly inserted* documents will have the highest RollNo and therefore map to the "upper bound" chunk. This causes an **Insert Hotspot**, where all write operations hit a single shard simultaneously instead of distributing across the cluster. To solve this in production, one might use a **Hashed Shard Key** (`{ RollNo: "hashed" }`), which distributes sequential inserts uniformly across shards, at the cost of making range queries slower (as they become scatter-gather queries).

## 3. What Happens When Additional Shards are Added?
When a new shard is added to an existing sharded cluster:
1. The **Balancer** (a background process running on the primary config server) notices that the chunk distribution is uneven.
2. It begins **Chunk Migration**. It moves chunks of data from overloaded shards to the new, empty shard.
3. During migration, the cluster remains fully operational. The source shard continues to serve reads/writes for the chunk being moved.
4. Once the chunk data is fully copied, the config server metadata is atomically updated to point to the new shard, and the old chunk is deleted from the source shard. 
5. This process continues until chunks are evenly balanced across all shards.

## 4. Standalone MongoDB vs Sharded Cluster Comparison
* **Storage / Data Volume**: A standalone instance is limited by the disk capacity of a single machine. A sharded cluster scales horizontally; adding more shards directly increases the total storage capacity limitlessly.
* **Throughput (Reads/Writes)**: A standalone instance is bottlenecked by the CPU, RAM, and disk I/O of one machine. In a sharded cluster, read and write operations are parallelized across multiple shards, vastly increasing the overall system throughput.
* **Scalability**: Scaling a standalone database requires vertical scaling (buying a bigger, more expensive server), which has a hard ceiling. Sharded clusters offer horizontal scaling (adding more cheap commodity servers).
* **Failure Tolerance**: A single standalone instance has zero failure tolerance (Single Point of Failure). In a proper sharded cluster (where config servers and shards are Replica Sets), if a node goes down, the replica set elects a new primary, and the cluster continues to function without data loss or downtime.
* **Latency Tradeoff (Observed in Task 8)**: As seen in our standalone comparison, for very small datasets or simple queries on a local machine, a standalone instance often performs slightly *faster*. This is because a sharded setup incurs network and routing overhead (mongos evaluating metadata, routing the request over TCP to the shard, and aggregating results). Sharding only provides a performance advantage when the data volume or concurrency exceeds the capacity of a single machine.
