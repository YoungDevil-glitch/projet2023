package com.project

import scala.collection.mutable.HashMap
import scala.io.StdIn.readLine


object SimpleStoreSplit extends App{
    var store = new HashMap[String, String]()
    var destroy: Boolean = true
    println("Welcome in Junior data store, We accept three operation store, lookup , delete, stop ")
    println("command should be of the form store_key_value or lookup_key or delete_key or stop ")
    def Store( key:String , value:String):Unit ={
        store += (key -> value)
    }
    def  Lookup(key:String): String = {
        return store.get(key).getOrElse(" ") 
    }
    def Delete(key:String){
        store -= key
    }
    while (destroy) {
        println("enter operation")
        var com :String=  readLine()
        var command= com.split("_")
        command(0) match{
            case "store" => {
                
                if (command.length == 3){

                var key:String = command(1)
                var value:String = command(2)
                if ( value != " "){
                    Store(key = key , value =value)
                }
                else
                    {
                    println("Incorect value")
                }
            }
            else
                {
                    println("invalid syntax store_key_value")
                }
                
            }
            case "lookup" => {
                if (command.length == 2){
                    var key:String = command(1)
                    var value:String = Lookup{key}
                    println(s"the value for key is $value")
                }
                else{
                    println("invalid syntax lookup_key")
                }
            }
            case "delete" =>{
                if (command.length == 2){
                    var key:String = command(1)
                    Delete(key)
                    println(s"deleted entry with key $key")
                }
                else{
                    println("invalid syntax delete_key")
                }
            }
            case "stop" =>{
                destroy= false
            }
            case _ =>{
                println("invalid operation")
            }
        }

    }
}
