# System Design (HLD) — SDE 3 Interview Reference

> **Format:** 45-60 min HLD round. Interviewer evaluates breadth of thinking, trade-off reasoning, and scalability instinct — not perfect answers.

---

## HLD Interview Template (use every time)

```
1. CLARIFY (5 min)
   - Functional requirements: what does the system do?
   - Non-functional: scale, latency, availability, consistency needs?
   - Out of scope: what NOT to design?

2. ESTIMATE (3 min)
   - DAU / QPS (reads vs writes)
   - Storage per day / year
   - Bandwidth

3. API DESIGN (5 min)
   - Core REST/gRPC endpoints
   - Key request/response shapes

4. DATA MODEL (5 min)
   - Entities + relationships
   - SQL vs NoSQL choice + why

5. HIGH-LEVEL COMPONENTS (15 min)
   - Draw: Client → LB → App Servers → Cache → DB → Queue → Workers
   - Explain data flow for primary use case

6. DEEP DIVE (10 min)
   - Pick the hardest part: consistency, fan-out, geo-distribution
   - Show trade-offs

7. BOTTLENECKS & SCALING (5 min)
   - Where does it break at 10x load?
   - Sharding strategy, read replicas, CDN
```

---

## Back-of-Envelope Cheat Sheet

| Metric | Value |
|--------|-------|
| 1 million users/day | ~12 QPS |
| 1 billion users/day | ~12,000 QPS |
| Read:Write ratio (social) | 100:1 |
| 1 KB per tweet/message | 1 GB = 1M messages |
| SSD read latency | ~0.1 ms |
| Network round trip | ~1-10 ms |
| 99.9% availability | ~8.7 hrs downtime/year |
| 99.99% availability | ~52 min downtime/year |

---

## CAP Theorem Quick Reference

```
        C (Consistency)
       / \
      /   \
     /     \
    P-------A
(Partition) (Availability)

CP systems: ZooKeeper, HBase, MongoDB (strong consistency mode)
AP systems: Cassandra, DynamoDB, CouchDB
CA systems: Traditional RDBMS (not partition tolerant — only for single node)
```

**Rule of thumb for interviews:**
- Financial / inventory → CP (correctness over availability)
- Social feeds / analytics → AP (availability over perfect consistency)
- Chat / messaging → AP with eventual consistency

---

## Consistent Hashing

```
Problem: When you add/remove servers, simple modulo hashing remaps ALL keys.
Solution: Place servers on a ring. Each key maps to the nearest server clockwise.
          Adding a server only remaps keys between it and its predecessor.

Ring: 0 ─── S1 ─── S2 ─── S3 ─── 360°
             ↑
         key maps here

Virtual nodes: Each physical server gets 100-200 virtual points on the ring
               → more even distribution
```

Used by: DynamoDB, Cassandra, Memcached (ketama), CDN routing

---

## Sharding Strategies

| Strategy | How | Good for | Problem |
|----------|-----|----------|---------|
| Range-based | shard by userId range | Sequential scans | Hot partitions |
| Hash-based | shard by hash(userId) | Even distribution | Range queries hard |
| Directory-based | lookup table maps key→shard | Flexible | Lookup table = SPOF |
| Geo-based | shard by region | Latency, compliance | Cross-region queries |

---

## Design 1: URL Shortener (e.g. bit.ly)

**Scale:** 100M URLs/day write, 10B redirects/day read (read-heavy 100:1)

```
Client → LB → App Server → Cache (Redis) → DB (PostgreSQL)
                    ↓
              ID Generator (Snowflake or Base62)
```

**Key decisions:**
- Short URL = Base62(auto-increment ID) → 7 chars = 62^7 = 3.5 trillion URLs
- Cache top 20% hot URLs (80% of traffic) in Redis with TTL
- DB: simple KV — shortCode → {originalUrl, userId, createdAt, expiresAt}
- Custom aliases: check uniqueness before insert

**Trade-off:** Snowflake ID (distributed, no DB round trip) vs DB auto-increment (simpler, bottleneck at scale)

---

## Design 2: Twitter / News Feed

**Scale:** 300M DAU, 500M tweets/day, feed read at 100:1 read:write

```
Tweet Write:
  User → API → Tweet Service → DB + Queue (Kafka)
                                          ↓
                                    Fan-out Worker → Follower Feed Cache (Redis)

Feed Read:
  User → API → Feed Service → Redis (pre-computed feed) → merge with celebrities
```

**Key decisions:**
- **Fan-out on write** (push): pre-compute feed for followers at write time → fast reads, expensive writes for users with many followers
- **Fan-out on read** (pull): compute feed at read time → cheaper writes, slower reads
- **Hybrid**: fan-out on write for normal users, fan-out on read for celebrities (>1M followers)
- Feed stored in Redis sorted set (score = timestamp)
- Media (images/video) → S3 + CDN

---

## Design 3: WhatsApp / Chat System

**Scale:** 2B users, 100B messages/day, <100ms delivery latency

```
Sender → WebSocket Server → Message Queue (Kafka) → WebSocket Server → Receiver
                                    ↓
                              Message DB (Cassandra)
                              (partitioned by conversationId)
```

**Key decisions:**
- **WebSocket** (persistent connection) for real-time; HTTP polling as fallback
- Messages stored in Cassandra: partition key = `(userId, conversationId)`, clustering key = `messageId (time-based UUID)`
- **Delivery receipts:** sent ✓, delivered ✓✓, read ✓✓ (blue) — tracked via ACK events
- End-to-end encryption: keys exchanged via Signal Protocol
- Media → S3 pre-signed URLs, only URL stored in message

---

## Design 4: Netflix / Video Streaming

**Scale:** 200M subscribers, 15% of global internet traffic

```
Upload:
  Creator → Upload Service → Raw Storage (S3) → Transcoding Workers → CDN (multiple resolutions)

Stream:
  User → CDN (nearest PoP) → if miss → Origin S3
               ↓
         Adaptive Bitrate (ABR): 240p/480p/720p/1080p/4K based on bandwidth
```

**Key decisions:**
- Pre-transcode to 5-6 resolutions using FFmpeg (MP4/H.264 + WebM/VP9)
- CDN pre-warms popular content before prime time
- Manifest file (`.m3u8`) tells client which chunk to request next
- Recommendations: offline ML pipeline, results cached per user

---

## Design 5: Ride Sharing (Uber/Ola)

**Scale:** 20M trips/day, driver location updates every 5 sec, match in <1 sec

```
Driver → Location Service → Geo Index (QuadTree/Redis GEOSEARCH) → Matching Service
User  → Ride Request → Matching Service → Driver notification (WebSocket/Push)
                              ↓
                         Trip DB (PostgreSQL) + Kafka (trip events)
```

**Key decisions:**
- **Geo-index:** Redis `GEOADD` + `GEORADIUS` for O(log n) nearest driver lookup
- Driver location stored in memory (Redis), persisted asynchronously
- Surge pricing: supply/demand ratio per geo-cell → separate pricing service
- Trip state machine: `REQUESTED → ACCEPTED → IN_PROGRESS → COMPLETED`

---

## Design 6: Distributed Rate Limiter

**Scale:** 10M req/sec, per-user limits, distributed across 100 servers

```
Request → API Gateway → Rate Limiter (Redis) → Backend

Redis Lua script (atomic):
  count = INCR user:123:minute:2024010112
  EXPIRE user:123:minute:2024010112 60
  if count > limit: reject
```

**Algorithms:**

| Algorithm | Pros | Cons |
|-----------|------|------|
| Token Bucket | Handles bursts, smooth | Tricky distributed sync |
| Leaky Bucket | Strict rate, no bursts | Drops valid traffic |
| Fixed Window | Simple | Burst at window boundary |
| Sliding Window Log | Accurate | High memory (store all timestamps) |
| Sliding Window Counter | Good accuracy + memory | Approximation |

**Best choice:** Sliding Window Counter in Redis — accurate, low memory, atomic via Lua

---

## Design 7: Distributed Cache (Memcached/Redis clone)

**Scale:** 1M QPS, <1ms p99, 100TB data

```
Client → Consistent Hash Router → Cache Node (shard)
                                         ↓ (miss)
                                    DB + write-back to cache
```

**Key decisions:**
- **Write strategies:**
  - Write-through: write cache + DB synchronously → consistent, slower writes
  - Write-behind: write cache, async write to DB → fast writes, risk of loss
  - Cache-aside: app checks cache, on miss reads DB and populates → most common
- **Eviction:** LRU (most common), LFU (for long-tail access patterns)
- **Replication:** primary-replica per shard for HA
- **Consistency:** cache invalidation on DB write (hardest problem in CS 😄)

---

## Design 8: Search Autocomplete (Google/Amazon search bar)

**Scale:** 10M QPS, results in <100ms, top-k suggestions per prefix

```
Query:
  User types → CDN (cache common prefixes) → Trie Service → top-k results

Offline:
  Search logs → Frequency Aggregator (Spark) → Trie Builder → Trie DB (Redis/ZooKeeper)
```

**Key decisions:**
- Trie stores top-10 suggestions at each node (pre-computed, not traversed at query time)
- Trie sharded by first 2 chars of prefix (26×26 = 676 shards)
- Updated periodically (not real-time) — hourly aggregation from search logs
- CDN caches responses for popular prefixes (covers 80% of traffic)
- For personalization: merge global trie with user history at query time

---

## Common Patterns Summary

| Pattern | When to use |
|---------|-------------|
| CQRS | Read/write have very different scale needs |
| Event Sourcing | Audit trail required, rebuild state from events |
| Saga Pattern | Distributed transactions across microservices |
| Circuit Breaker | Protect from cascading failures (Resilience4j) |
| Outbox Pattern | Guarantee DB write + event publish atomically |
| Strangler Fig | Migrate legacy system incrementally |
| Sidecar | Cross-cutting concerns (logging, auth) without modifying service |

---

## Interview Anti-Patterns to Avoid

- Jumping to solution without clarifying scale
- Designing for perfect consistency everywhere (mention trade-off instead)
- Ignoring failure modes (what if DB is down? cache is cold?)
- Single points of failure with no HA mention
- Not mentioning monitoring / alerting
