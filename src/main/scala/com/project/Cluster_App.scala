package com.project

import akka.actor.{Actor, ActorRef, ActorSystem, Props,Address, Deploy}
import scala.util.Random._
import scala.collection.mutable.HashMap
import java.io._
import akka.pattern.ask
import akka.util.Timeout
import akka.actor.typed.scaladsl.AskPattern._
import scala.io.Source
import scala.io.StdIn.readLine
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.concurrent.Future
import scala.util.Success
import scala.util.Failure
import com.project.JournalActor
import com.project.LimitedCacheActor
import com.project.Messages 
import com.project.Client
import com.project.ClientAuto
import com.project.ManagerActor
import akka.cluster.ClusterEvent._
import akka.cluster.Cluster
import scala.concurrent.{ExecutionContext}
import com.typesafe.config.ConfigFactory


object DistributedJournalStoreSystem extends App{
    import Messages._
    val as = ActorSystem("ClusterSystem")
    val cluster = Cluster(as)
    val journal = as.actorOf(Props(new JournalActor("Data.txt")), "journal") 
}

object DistributedMultiCacheStoreSystem extends App{
    import Messages._
    val as = ActorSystem("ClusterSystem", ConfigFactory.load())
    val cluster = Cluster(as)
    implicit lazy val  ec = ExecutionContext.fromExecutor(as.dispatcher)
    val size: Int = 5
    val init: Int = 5
    val load : Int = 20
    val delay : Int = 1000
    val chatServerAddress = "akka://ClusterSystem@127.0.0.1:25251/user/journal"
    as.actorSelection(chatServerAddress).resolveOne(3 seconds).onComplete{
    case Success(journal : ActorRef )=> {
      val cache = as.actorOf(Props(new ManagerActor(journal  , size , delay , load , init)), "cache")
      cache ! Start()
    }
    case Failure(exception) => { 
        println("Not found")
    }
}
}

object DistributedCacheStoreSystem extends App{
    import Messages._
    val as = ActorSystem("ClusterSystem")
    val cluster = Cluster(as)
    implicit lazy val  ec = ExecutionContext.fromExecutor(as.dispatcher)

    val size: Int = 5
    val init: Int = 5
    val load : Int = 20
    val delay : Int = 1000
    val chatServerAddress = "akka://ClusterSystem@127.0.0.1:25251/user/journal"
    as.actorSelection(chatServerAddress).resolveOne(3 seconds).onComplete{
    case Success(journal : ActorRef )=> {
      val cache = as.actorOf(Props(new LimitedCacheActor(journal  , size)), "cache")
        cache ! Start()

    }
    case Failure(exception) => { 
        println("Not found")
    }
}
}

object DistributedClientStoreSystem extends App{
    import Messages._
    val as = ActorSystem("ClusterSystem")
    val cluster = Cluster(as)
    implicit lazy val  ec = ExecutionContext.fromExecutor(as.dispatcher)
    val client = as.actorOf(Props(new JournalActor("sample.txt")), "text")
    val chatServerAddress = "akka://ClusterSystem@127.0.0.1:25254/user/cache"
    as.actorSelection(chatServerAddress).resolveOne(3 seconds).onComplete{
    case Success(journal : ActorRef )=> {
        as.actorOf(Props(new Client(journal)), "client")
    }
    case Failure(exception) => { 
        println("Not found")
    }
    client ! Messages.Check()
}
}
object DistributedClientJournalStoreSystem extends App{
    import Messages._
    val as = ActorSystem("ClusterSystem",ConfigFactory.load())

    val cluster = Cluster(as)
    implicit lazy val  ec = ExecutionContext.fromExecutor(as.dispatcher)

    val chatServerAddress = "akka://ClusterSystem@127.0.0.1:25251/user/journal"
    as.actorSelection(chatServerAddress).resolveOne(3 seconds).onComplete{
    case Success(journal : ActorRef )=> {
        println("got it")
        val client = as.actorOf(Props(new Client(journal)), "client1")
    }
    case Failure(exception) => { 
        println("Not found")

    }
}
}

object DistributedClientAutoStoreSystem extends App{
    var runs = 20
    import Messages._
    val size:Int = 5
    val request: Int = 1000
    val stored: Int = 500
    var runtime: Float = 0 
    val message:Int = 2000 
    for(run <- 0 to runs){
        val startTimeMillis = System.currentTimeMillis()
        val as = ActorSystem("ClusterSystem")
        val cluster = Cluster(as)
        implicit lazy val  ec = ExecutionContext.fromExecutor(as.dispatcher)
        val chatServerAddress = "akka://ClusterSystem@127.0.0.1:25254/user/cache"
        as.actorSelection(chatServerAddress).resolveOne(3 seconds).onComplete{
        case Success(journal : ActorRef )=> {
           val client = as.actorOf(Props(new ClientAuto(journal,request, stored, message)) , "client2")
           client ! "Start"
        }
        case Failure(exception) => { 
            println("Not found")
    }
    }
        
        val endTimeMillis = System.currentTimeMillis()
        val durationSeconds = (endTimeMillis - startTimeMillis) 
        var file  = new File("Data.txt")
        val bw =  new BufferedWriter(new FileWriter(file, false))
        bw.write("")
        bw.close()
        runtime = runtime + (durationSeconds -runtime)/(run+1)

    }
    println(s"runtime $runtime   ms")
}

object DistributedClientAutoJournalStoreSystem extends App{
    import Messages._
    var runs = 20
    val size:Int = 5
    val request: Int = 1000
    val stored: Int = 500
    var runtime: Float = 0 
    val message:Int = 2000 
    for(run <- 0 to runs){
        val startTimeMillis = System.currentTimeMillis()
        val as = ActorSystem("ClusterSystem")
        val cluster = Cluster(as)
        implicit lazy val  ec = ExecutionContext.fromExecutor(as.dispatcher)
        val chatServerAddress = "akka://ClusterSystem@127.0.0.1:25251/user/journal"
        as.actorSelection(chatServerAddress).resolveOne(3 seconds).onComplete{
        case Success(journal : ActorRef )=> {
           val client = as.actorOf(Props(new ClientAuto(journal,request, stored, message)), "client3")
           client ! "Start"
        }
        case Failure(exception) => { 
            println("Not found")
    }
    }
        
        val endTimeMillis = System.currentTimeMillis()
        val durationSeconds = (endTimeMillis - startTimeMillis) 
        var file  = new File("Data.txt")
        val bw =  new BufferedWriter(new FileWriter(file, false))
        bw.write("")
        bw.close()
        runtime = runtime + (durationSeconds -runtime)/(run+1)

    }
    println(s"runtime $runtime   ms")
}