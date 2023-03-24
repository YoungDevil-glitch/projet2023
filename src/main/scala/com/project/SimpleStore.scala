package com.project

import scala.collection.mutable.HashMap
import scala.io.StdIn.readLine


object SimpleStore extends App{
    var store = new HashMap[String, String]()
    var destroy: Boolean = true
    println("Welcome in Junior data store, We accept three operation store, lookup , delete, stop ")
    def Store(key:String , value:String): Unit ={
        store += (key -> value)
    }
    def  Lookup(key:String): String = {
        return store.get(key).getOrElse(" ") 
    }
    def Delete(key:String):  Unit = {
        store -= key
    }
    while (destroy) {
        println("enter operation")
        val command: String = readLine()
        command match{
            case "store" => {
                println("enter key")
                var key:String = readLine()
                println("enter value")
                var value:String = readLine()
                while ( value == " "){
                    println("enter value (can't be a single space)")
                    var value:String = readLine()
                }
                Store(key = key , value =value)
            }
            case "lookup" => {
                println("enter key")
                var key:String = readLine()
                var value:String = Lookup{key}
                println(s"the value for key is $value")
            }
            case "delete" =>{
                println("enter key")
                var key:String = readLine()
                Delete(key)
                println(s"deleted entry with key $key")
            }
            case "stop" =>{
                destroy= false
            }
        }

    }
}
