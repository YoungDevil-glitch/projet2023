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
import com.project.Client
import com.project.Messages 
import com.project.CacheActor


class  LimitedCacheActor(journal : ActorRef , size : Int) extends CacheActor(journal){
    import Messages._
    override def preStart(): Unit = {
        journal ! Init()
    }
    var used  = new HashMap[String, Int]()

    def select_delete():Unit= { 
        var  key = used.minBy(_._2)._1 
        Delete(key)
    }

    override def Store( key:String , value:String):  Unit = {
        store += (key -> value)
        used += (key -> 1)
    }
    override def  Lookup(key:String): String = {
        return store.get(key).getOrElse(" ") 
    }
    override def Delete(key:String):  Unit = {
        store -= key
        used -= key
    }
    override def receive: Receive = {
        case LookupMessage(key, receiver) =>{
            val client: ActorRef = sender
            var value:String = Lookup{key}
            if (value == " "){
                journal ! LookupMessage(key,receiver)
                sender ! Response("Request sent to server")
            }
            else{
                sender ! LookupResponse(key,value , false, receiver)
            }
        }
        case StoreMessage(key,value,receiver)=>{
            Delete(key)
            journal ! StoreMessage(key,value, receiver)
        }
        case DeleteMessage(key, receiver)=>{
            
            journal ! DeleteMessage(key, receiver)
            Delete(key)
            sender ! DeleteResponse(s"Deleted value in cache  for key $key", sender)
        }
        case LookupResponse(key,result , isdeleted,receiver)=>{     
            if (result != " " && isdeleted !=true){
                if (store.size >= size ){
                    select_delete()
                }
                Store(key, result)
            }
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
 


object LimitedCacheJournal extends App{
    import Messages._
    val as = ActorSystem("Store")
    val size:Int = 1
    val journal = as.actorOf(Props(new JournalActor("Data.txt")), "journal")    
    val cache = as.actorOf(Props(new LimitedCacheActor(journal , size)), "cache")
    val client = as.actorOf(Props(new Client(cache)), "client")

}




