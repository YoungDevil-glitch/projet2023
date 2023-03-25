# Project 2023
## Emmanuel Junior Wafo Wembe
Cloud Computing project
## Repos presentation 
1. Day 1 part 1: SimpleStore.scala / SimpleStoreSplit.scala
2. Day 1 part 2 : SimpleStoreActor.scala
3. Day 2 part 1 : SimpleStorePersistent.scala
4. Day 2 part 2: SimpleStorePersistentCache.scala
2. Day 3 part 1 : LimitedCache.scala / Latency_Slow.scala 
3. Day 3 part 3 : Active_Client_Cache.scala / Active_client_journal.scala / MultipleCacheActors.scala
4. Day 4 part 1 : Cluster_App.scala
5. Compiled code in  target/scala-2.13
## How to Run 

SimpleStore:

    sbt "runMain com.example.SimpleStore"

SimpleStoreSplit

    sbt "runMain com.example.SimpleStoreSplit"
    
SimpleStoreActor

    sbt "runMain com.example.SimpleStoreActors"
    
SimpleStorePersitent

    sbt "runMain com.example.StoreActorsPers"
    
SimpleStorePersistentCache:

    sbt "runMain com.example.CacheJournal"

LimitedCache 

    sbt "runMain com.example.LimitedCacheJournal"

Latency_Slow (Adding latency)
    
    sbt "runMain com.example.LimitedCacheJournalLat"
    
Test System 
 
     sbt "runMain com.example.LimitedCacheJournalAuto"
    
     sbt "runMain com.example.Journal_Auto"
     
The first test the system with a caching actor with limited storage and the second the client against a simple journal.

     sbt "runMain com.example.LimitedCacheJournalLoadBalancerAuto"
     

## Interesting Design Choices 
 
+ The code run asynchrously.  Every request message is accompagned by a reference to the original sender.
+ For the limited cache i used a frequency based strategy for deletion
+ In MultipleCacheActors, I implemented a load balancer that maintain at least a certain number of caching actors and assign task to the actor whose load is the smalllest ( Number of request waiting), if this number is higher than a threshold, I initiate a delay and then tries for a maximum of ten time to get a caching Actor. If not possible I just spawn a new Caching Actor. Idle Actors are regurlarly deleted. 


## Clusters 
This doesn't seem to work very well. 
basic Syntax 

sbt "runMain -Dconfig.ressource=configfile_name  Classe Name  "

config file 

- server.conf for Journal Actor 
    + com.example.DistributedJournalStoreSystem
        
        sbt "sbt "runMain -Dconfig.ressource=server.conf com.example.DistributedJournalStoreSystem
    
- cache.conf for Caching type Actor 

    sbt "sbt "runMain -Dconfig.ressource=cache.conf com.example.DistributedMultiCacheStoreSystem
    sbt "sbt "runMain -Dconfig.ressource=cache.conf com.example.DistributedCacheStoreSystem
    
- client.conf for Client

        sbt "sbt "runMain -Dconfig.ressource=client.conf com.example.DistributedClientAutoStoreSystem
        sbt "sbt "runMain -Dconfig.ressource=client.conf com.example.DistributedClientAutoJournalStoreSystem
        sbt "sbt "runMain -Dconfig.ressource=client.conf com.example.DistributedClientJournalStoreSystem
        sbt "sbt "runMain -Dconfig.ressource=client.conf com.example.DistributedClientStoreSystem

