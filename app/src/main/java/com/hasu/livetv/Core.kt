package com.hasu.livetv.model

data class Channel(val id:String,val name:String,val streamUrl:String,val category:String,val country:String,val language:String,val channelNumber:Int,val enabled:Boolean=true,val featured:Boolean=false,val description:String="",val logoUrl:String="")
data class Category(val id:String,val name:String,val enabled:Boolean=true,val order:Int=0)
