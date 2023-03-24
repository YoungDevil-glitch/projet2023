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

class ClientAuto(cache : ActorRef, request : Int , stored : Int , message: Int) extends Client(cache){
    import Messages._
    var count:Int = 0
    def run_experiment():Unit = {
        var prevkey = 0
        for(w <- 1 to stored){
            
            cache ! StoreMessage(s"key$w", s"value$w",self)
            count +=1

        }
        for (w <-1 to message){
            nextInt(3) match{
                case 0=>{
                    var key = nextInt(request)+1
                    while(key <= stored){
                        key = nextInt(request)+1
                }
                    cache ! StoreMessage(s"key$key",s"value$key",self)
                }
                case 1=>{
                    var newkey = 0
                    if (prevkey == 0){
                        var h = nextInt(100) 
                        if (h <= 50){
                            newkey = nextInt(stored)+1
                        }
                        else{
                            newkey = nextInt(request)+1
                        }
                    }
                    else{
                        var h = nextInt(100) 
                        if (h <= 50){
                            newkey = prevkey
                        }
                        else{
                            var h = nextInt(100) 
                            if (h < 50){
                                newkey = nextInt(stored)+1
                            }
                            else{
                                newkey = nextInt(request)+1
                            }
                        }
                       

                    }
                    cache ! LookupMessage(s"key$newkey",self)
                    prevkey = newkey
                }
                case 2=>{
                    var key = nextInt(request)+1
                    while(key <= stored){
                        key = nextInt(request)+1
                    }
                    cache ! DeleteMessage(s"key$key", self)
                    
                }
            }
        count +=1
        }
        
        while(count!=0){

        }
        self ! "Stop"
    }
    override def receive: Receive = {
        case LookupResponse(key,result , isdeleted,receiver)=>{
            println(s"$result")
            if (isdeleted){
                println("key appear to have  been deleted")
            }
            
            if (receiver != self){
                receiver ! LookupResponse(key, result , isdeleted,receiver)}
            count-=1 
        }
        case StoreResponse(result, receiver)=>{
            println(result)
            if (receiver != self){
                receiver ! StoreResponse(result, receiver)}
            count-=1
        }
        case DeleteResponse(result, receiver)=>{
            println(result)
            if (receiver != self){
                receiver ! DeleteResponse(result, receiver)}
            count-=1
    
        }
        case Response(result) =>{
            println(result)
        }
        case Check() => {
            var x = readLine() 
            x match{
            case "connect" =>{
                
                cache ! Start()
            }            
            case "command" =>{
                println("command should be of the form store_key_value or lookup_key or delete_key or stop ")
                makecommand()
            }
            case "stop" => {
                cache ! Stop()
            }
            case _ =>{
                println("I don't recognise this command")
                self ! Check()
            }
        
        } 
    }
        case "  Stop" => {
            cache ! Stop()
            }
        case Stop() =>{
        context.stop(self)
     }
        case "Start" =>{    
            run_experiment()
        }
        case _ =>{
            println("don't know what to do")
        }
        
    }
}




object LimitedCacheJournalAuto extends App{
    import Messages._
    
    var runs = 20
    val size:Int = 5
    val request: Int = 1000
    val stored: Int = 500
    var runtime: Float = 0 
    val message:Int = 2000 
    for(run <- 0 to runs){
        val startTimeMillis = System.currentTimeMillis()
        val as = ActorSystem("Store")
        val journal = as.actorOf(Props(new JournalActor("Data.txt")), "journal")    
        val cache = as.actorOf(Props(new LimitedCacheActor(journal , size)), "cache")
        val client = as.actorOf(Props(new ClientAuto(cache,request, stored, message)))
        client ! "Start"
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