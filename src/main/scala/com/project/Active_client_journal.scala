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
import com.project.Messages 
import com.project.ClientAuto




object Journal_Auto extends App{
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
        val client = as.actorOf(Props(new ClientAuto(journal,request, stored, message)))
        client ! "Start"
        var file  = new File("Data.txt")
        val bw =  new BufferedWriter(new FileWriter(file, false))
        bw.write("")
        bw.close()
        val endTimeMillis = System.currentTimeMillis()
        val durationSeconds = (endTimeMillis - startTimeMillis) 
        runtime = runtime + (durationSeconds -runtime)/(run+1)
    }
    println(s"runtime $runtime   ms")
}