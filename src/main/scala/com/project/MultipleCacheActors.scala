package com.project

import akka.actor.{Actor, ActorRef, ActorSystem, Props,Address, Deploy, OneForOneStrategy,AllForOneStrategy}
import akka.actor.SupervisorStrategy._
import scala.util.Random._
import akka.actor.typed.{Terminated}
import scala.collection.mutable.{HashMap}
import scala.util.control.Breaks._
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
import com.project.Messages 
import com.project.ClientAuto 
import com.project.LimitedCacheActor



class ManagerActor(journal : ActorRef , size : Int, delay : Int, load : Int, init:Int =5) extends  Actor{
    import Messages._
    context.watch(journal)
    var caches = scala.collection.mutable.Map[Int,ActorRef]() 
    var busy = scala.collection.mutable.Map[Int, Int]()
    var valid:Boolean = true 
    override val supervisorStrategy = OneForOneStrategy(maxNrOfRetries = 10 , withinTimeRange = 1 minute) {
        case _ : Throwable => Restart 
        case t => super.supervisorStrategy.decider.applyOrElse(t,(_: Any) => Escalate)
    }
    override def preStart(): Unit = {
        for (i <- 1 to init){
            var child = context.actorOf(Props(new LimitedCacheActorLoad(journal , size)), s"cache$i")
            context.watch(child)
            caches += (i -> child) 
            busy += (i -> 0) 
        }
    }
    def update_Keys(): Unit = {
        valid =false
        var temp = scala.collection.mutable.Map[Int,ActorRef]() 
        for((key,value)<- caches ){
            temp += (key%caches.size+1 -> value)
        }
        caches = temp.clone()
        var temp1 = scala.collection.mutable.Map[Int,Int]() 
        for((key,value)<- busy ){
            temp1 += (key%caches.size+1 -> value)
        }
        busy = temp1.clone()
        valid = true
    }
    def verify_state(): Unit={
        valid = false
        var temp = caches.clone()
        breakable
        {
        for ((key, value) <- temp) {
            if (caches.size == init){
                break
            }
            else{ if(busy(key) ==0){
                context.unwatch(value)
                value ! Stop()
                caches -= key
                busy -= key 
            }
            }
        }
        }

        valid =true
    }
    
    override def receive: Receive = {
        case LookupMessage(key, receiver) =>{
            while(valid == false){} 
            var h = nextInt(caches.size)+ 1
            var start = System.currentTimeMillis()
            var delay_2 = System.currentTimeMillis() -start
            while( busy(h)>= load && delay_2 < delay){
                h = nextInt(caches.size) + 1 
                delay_2 = System.currentTimeMillis() -start
            }
            if (delay_2 >= delay){
                h = caches.size+1 
                var child = context.actorOf(Props(new LimitedCacheActorLoad(journal , size)), s"cache$h")
                context.watch(child)
                caches += (h -> child) 
                busy += (h -> 1) 
                child ! LookupMessage(key, receiver)
            }
            else{  if(busy(h) < 20 ){
                caches(h) ! LookupMessage(key, receiver)
            }
            }
            self ! Check()
        }
        case StoreMessage(key,value,receiver)=>{
            for ((key_1, child) <- caches){
                while(busy(key_1) >= 20){}
                child ! StoreMessage(key,value,receiver) 
                busy(key_1) +=1  
            }
            self ! Check()
        }
        case DeleteMessage(key, receiver)=>{
            
            for ((key_1, child) <- caches){
                while(busy(key_1) >= 20){}
                child ! DeleteMessage(key, receiver) 
                busy(key_1) +=1  
            }
            self ! Check()
        }
        case LookupResponse(key,result , isdeleted,receiver)=>{     
            if (receiver != self){
                receiver ! LookupResponse(key, result , isdeleted,receiver)}
        }
        case StoreResponse(result, receiver)=>{
            if (receiver != self){
                receiver ! StoreResponse(result, receiver)}
        }
        case DeleteResponse(result, receiver)=>{
            if (receiver != self){
                receiver ! DeleteResponse(result, receiver)}
        }
        case Child(key)=>{
            busy(key) = List(busy(key)-1 , 0).max
        }
        case Start() =>{
            sender ! Response("Welcome in Junior data store, We accept three operation store, lookup , delete, stop ")
            
        }
        case Stop() =>{
            sender ! Response("Cache Server shutting down ")
            context.stop(self)
        }
        case _=>{
            sender ! Response("Wrong Message")
        }
    }
}

class LimitedCacheActorLoad(journal: ActorRef , size: Int) extends LimitedCacheActor(journal, size){
    import Messages._
    var index =0
    override def receive: Receive ={ 
        case Child(key) => {
            var index = key 
        }
        case t =>   {super.receive
            if (index !=0){
                context.parent ! Child(index)
        }
        }
    }   
}

object LimitedCacheJournalLoadbalancer_Auto extends App{
    import Messages._
    
    var runs = 20
    val size:Int = 5
    val init = 5
    val load : Int = 20
    val delay : Int = 1000
    val request: Int = 1000
    val stored: Int = 500
    var runtime: Float = 0 
    val message:Int = 2000 
    for(run <- 0 to runs){

        val startTimeMillis = System.currentTimeMillis()
        val as = ActorSystem("Store")
        val journal = as.actorOf(Props(new JournalActor("Data.txt")), "journal")    
        val cache = as.actorOf(Props(new ManagerActor(journal  , size , delay , load , init)), "cache")
        val client = as.actorOf(Props(new ClientAuto(cache,request, stored, message)))
        client ! "Start"
        var file  = new File("Data.txt")
        val bw =  new BufferedWriter(new FileWriter(file, false))
        bw.write("")
        bw.close()
        val endTimeMillis = System.currentTimeMillis()
        val durationSeconds = (endTimeMillis - startTimeMillis) 
        runtime = runtime + (durationSeconds -runtime)/(run+1)
    }
    println(s"rruntime $runtime   ms")
}