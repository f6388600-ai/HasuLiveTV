package com.hasu.livetv.data

import com.hasu.livetv.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface LiveTvRepository {
    val channels: StateFlow<List<Channel>>
    val categories: StateFlow<List<Category>>
    suspend fun upsertChannel(channel: Channel)
    suspend fun deleteChannel(id:String)
}

class DemoRepository : LiveTvRepository {
    private val _channels = MutableStateFlow(listOf(
        Channel("news","Hasu News","https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8","News","Global","English",1,featured=true,description="Demo live news channel."),
        Channel("sports","Hasu Sports","https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8","Sports","Global","English",2,featured=true,description="Demo sports channel."),
        Channel("music","Hasu Music","https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8","Music","Global","English",3,description="Demo music channel."),
        Channel("kids","Hasu Kids","https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8","Kids","Global","English",4,description="Demo family channel."),
        Channel("movie","Hasu Movies","https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8","Movies","Global","English",5,description="Demo movie channel.")
    ))
    override val channels:StateFlow<List<Channel>> = _channels
    override val categories:StateFlow<List<Category>> = MutableStateFlow(listOf(Category("news","News",order=1),Category("sports","Sports",order=2),Category("movies","Movies",order=3),Category("music","Music",order=4),Category("kids","Kids",order=5)))
    override suspend fun upsertChannel(channel:Channel){_channels.value=(_channels.value.filterNot{it.id==channel.id}+channel).sortedBy{it.channelNumber}}
    override suspend fun deleteChannel(id:String){_channels.value=_channels.value.filterNot{it.id==id}}
}

/** Firebase-ready boundary. Add Firebase SDKs later and implement this interface without changing UI. */
class FirebaseRepositoryPlaceholder : LiveTvRepository {
    override val channels = MutableStateFlow<List<Channel>>(emptyList())
    override val categories = MutableStateFlow<List<Category>>(emptyList())
    override suspend fun upsertChannel(channel:Channel)=Unit
    override suspend fun deleteChannel(id:String)=Unit
}
