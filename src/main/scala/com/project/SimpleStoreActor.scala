package com.project

import akka.actor.{Actor, ActorRef, ActorSystem, Props,Address, Deploy}
import scala.util.Random._
import scala.collection.mutable.HashMap
import scala.io.StdIn.readLine
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Success
import scala.util.Failure


object Messages{
    case class StoreMessage(key : String, value : String , sender: ActorRef)
    case class StoreResponse(result : String, receiver : ActorRef)
    case class LookupMessage(key: String, sender: ActorRef)
    case class LookupResponse(key: String, value: String, isdeleted : Boolean , receiver : ActorRef)
    case class DeleteMessage(key : String, sender: ActorRef )
    case class DeleteResponse(result : String, receiver : ActorRef)
    case class Response( result: String)
    case class Init()
    case class Start()
    case class Stop()
    case class Check() 
    case class Child(key :Int)
}

class StoreServer extends Actor {
    import Messages._
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
        case LookupMessage(key,receiver) =>{
             var value:String = Lookup{key}
            if (value == " "){
                sender ! LookupResponse(key, "value not found" , false,receiver)
            }
            else{
                sender ! LookupResponse(key, value , false,receiver)
            }
        }
    case StoreMessage(key,value, receiver)=>{
        if ( value != " "){
            Store(key = key , value =value)
            sender ! StoreResponse("Value Stored",receiver)
        }
        else
            {
            sender ! StoreResponse("Incorect value sent",receiver)
        }
    }
    case DeleteMessage(key,receiver)=>{
        Delete(key)
        sender ! DeleteResponse("Value has been deleted",receiver)
    }
    case Start() =>{
        sender ! Response("Welcome in Junior data store, We accept three operation store, lookup , delete, stop ")
        
    }
    case Stop() =>{
        sender ! Response("Server shutting down ")
        context.stop(self)
    }
    case _ =>{
        sender ! Response("Erroneous message")}
    }
}

class Client(server: ActorRef) extends Actor{ 
    import Messages._
    override def preStart() = {
    println("Welcome, to connect to the store type connect , to send command type command, to stop connection type stop ")
    println(self.path)
    println(server.path)
    server ! Start()
    self ! Check()
  }
    context.watch(server)
    override def receive: Receive = {
        case LookupResponse(key,result , isdeleted,receiver)=>{
            println(s"$result")
            if (isdeleted){
                println("key appear to have  been deleted")
            }
            
            println("Welcome, to connect to the store type connect , to send command type command, to stop connection type stop ")
            if (receiver != self){
                receiver ! LookupResponse(key, result , isdeleted,receiver)}
            self ! Check() 
        }
        case StoreResponse(result, receiver)=>{
            println(result)
            if (receiver != self){
                receiver ! StoreResponse(result, receiver)}
            println("Welcome, to connect to the store type connect , to send command type command, to stop connection type stop ")
            self ! Check()
        }
        case DeleteResponse(result, receiver)=>{
            println(result)
            if (receiver != self){
                receiver ! DeleteResponse(result, receiver)}
            println("Welcome, to connect to the store type connect , to send command type command, to stop connection type stop ")
            self ! Check()
        }
        case Response(result) =>{
            println(result)
            println("Welcome, to connect to the store type connect , to send command type command, to stop connection type stop ")
            self ! Check() 
        }
        case Check() => {
            var x = readLine() 
            x match{
            case "connect" =>{
                server ! Start()

            }            
            case "command" =>{
                println("command should be of the form store_key_value or lookup_key or delete_key or stop ")
                makecommand()
            }
            case "stop" => {
                context.unwatch(server)
                server ! Stop()
            }
            case _ =>{
                println("Unknown Command")
                self ! Check()
            }
        } 
    }
        
        case _ =>{
            sender ! "wrong message"
            self ! Check()
        }

    }
    def makecommand(): Unit ={
        println("enter command")
        var command:String= readLine()
        var input = command.split("_")
        input(0) match{
                case "store" => {
                    
                    if (input.length == 3){

                        var key:String = input(1)
                        var value:String = input(2)
                        server ! StoreMessage(key, value, self)
                }
                else
                    {
                        println("invalid syntax store_key_value")
                    }
                    
                }
                case "lookup" => {
                    if (input.length == 2){
                        var key:String = input(1)
                        
                        server ! LookupMessage(key, self)
                    }
                    else{
                        println("invalid syntax lookup_key")
                        self ! Check()
                    }
                }
                case "delete" =>{
                    if (input.length == 2){
                        var key:String = input(1)
                        server ! DeleteMessage(key,self)
                    }
                    else{
                        println("invalid syntax delete_key")
                        self ! Check()
                    }
                }
                case _ =>{
                    println("unknown input")
                }
            }
    }


}


object SimpleStoreActors extends App{
    import Messages._
    val as = ActorSystem("Store")
    val serv = as.actorOf(Props(new StoreServer()), "storeserver")    
    val client = as.actorOf(Props(new Client(serv )), "client")

}