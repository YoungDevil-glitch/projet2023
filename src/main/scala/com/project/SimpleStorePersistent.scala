package com.project

import akka.actor.{Actor, ActorRef, ActorSystem, Props,Address, Deploy}
import scala.collection.mutable.HashMap
import java.io._
import scala.util.control.Breaks._
import scala.io.Source
import scala.io.StdIn.readLine
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Success
import scala.util.Failure
import com.project.Messages
import com.project.Client


class JournalActor(filename:String) extends Actor {
    import Messages._
    
    def Store( key:String , value:String):  Unit = {
        var file  = new File(filename)
        val bw =  new BufferedWriter(new FileWriter(file, true))
        bw.write(s"$key,$value\n")
        bw.close()
    }
    def  Lookup(key:String): LookupResponse = {
        var value = " "
        var isdeleted = false 
         breakable
        {
        for (line <- Source.fromFile(filename).getLines.toList.reverse) {
            if(line.contains(",")){
                var lines = line.split(",")
                if(lines.length >=2){
                    var key1 = lines(0)
                    var key2 = lines(1)
                    if(key1 == key && (key2 != "deleted" || key2 != " ")  ){
                        value = key2
                        break
                    }
                    else{
                        if(key1 == key && key2 == "deleted" && isdeleted == false){
                            isdeleted = true
                        }
                    }
            }
            }
        }
        }
        return LookupResponse(key, value , isdeleted, self) 
    }

    def Delete(key:String):  Unit = {
        var file  = new File(filename)
        val bw =  new BufferedWriter(new FileWriter(file, true))
        bw.write(s"$key,deleted\n")
        bw.close()
    }
    override def receive: Receive = {
        case LookupMessage(key, receiver) =>{
             var value:LookupResponse = Lookup(key)
            if (value.value == " "){
                sender ! LookupResponse(key," ", false, receiver)
            }
            else{
                sender ! LookupResponse(key, value.value , value.isdeleted, receiver)
            }
        }
        case StoreMessage(key,value, receiver)=>{
            if ( value != " "){
                Store(key = key , value =value)
                sender ! StoreResponse("Value Stored", receiver)
            }
            else
                {
                sender ! StoreResponse("Incorect value sent", receiver)
            }
        }
        case DeleteMessage(key,receiver)=>{
            Delete(key)
            sender ! DeleteResponse("Value has been deleted", receiver)
        }
        case Start() =>{
            sender ! Response("Welcome in Junior data store, We accept three operation store, lookup , delete, stop ")
            
        }
        case Stop() =>{
            sender ! Response("Server shutting down ")
            context.stop(self)
        }
        case _ =>{
            sender ! ("Erroneous message")}
        }
}




object StoreActorsPers extends App{
    import Messages._
    val as = ActorSystem("Store")
    val serv = as.actorOf(Props(new JournalActor("Data.txt")), "storeserver")    
    val client = as.actorOf(Props(new Client(serv )), "client")

}
