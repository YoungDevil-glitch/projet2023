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
import com.project.Messages
import com.project.JournalActor
import com.project.Client


class  CacheActor(journal : ActorRef) extends Actor{
    import Messages._
    override def preStart(): Unit = {
        journal ! Init()
    }
    var store = new HashMap[String, String]()
    
    def Store( key:String , value:String):  Unit = {
        store += (key -> value)
    }
    def  Lookup(key:String): String = {
        return store.get(key).getOrElse(" ") 
    }
    def Delete(key:String):  Unit = {
        store -= key
    }
    override def receive: Receive = {
        case LookupMessage(key, receiver) =>{
            val client: ActorRef = sender
            var value:String = Lookup{key}
            if (value == " "){
                journal ! LookupMessage(key, receiver)
            }
            else{
                sender ! LookupResponse(key,value , false, sender)
            }
        }
        case StoreMessage(key,value, receiver)=>{
            val client: ActorRef = sender
            journal ! StoreMessage(key,value,receiver)
        }
        case DeleteMessage(key, receiver)=>{
            val client: ActorRef = sender
            
            journal ! DeleteMessage(key, receiver)
            Delete(key)
            client ! DeleteResponse(s"Deleted value in cache  for key $key",client)
        }
        case LookupResponse(key,result , isdeleted,receiver)=>{     
            if (result != " " && isdeleted !=true){
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
            journal ! Stop()
            context.stop(self)
        }
        case value =>{
            println(sender)
            println(value)
            sender ! Response("Wrong Message")
        }
}
}
 
object CacheJournal extends App{
    import Messages._
    val as = ActorSystem("Store")
    val journal = as.actorOf(Props(new JournalActor("Data.txt")), "journal")    
    val cache = as.actorOf(Props(new CacheActor(journal)), "cache")
    val client = as.actorOf(Props(new Client(cache)), "client")

}



