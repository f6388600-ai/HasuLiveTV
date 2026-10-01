package com.hasu.livetv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.hasu.livetv.data.*
import com.hasu.livetv.model.*
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { HasuTheme { App((application as HasuLiveTvApplication).repo) } }
    }
}

@Composable private fun App(repo: LiveTvRepository) {
    var splash by remember { mutableStateOf(true) }
    var page by remember { mutableStateOf<Page>(Page.Home) }
    var selected by remember { mutableStateOf<Channel?>(null) }
    LaunchedEffect(Unit) { delay(900); splash = false }
    if (splash) return Splash()
    if (BuildConfig.EDITION == "admin") return AdminApp(repo)
    AnimatedContent(page, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "navigation") { p ->
        when (p) {
            Page.Home -> Home(repo, BuildConfig.EDITION == "tv", { selected = it; page = Page.Player }, { page = Page.About })
            Page.About -> About { page = Page.Home }
            Page.Player -> selected?.let { Player(it) { selected = null; page = Page.Home } } ?: Home(repo, BuildConfig.EDITION == "tv", { selected = it; page = Page.Player }, { page = Page.About })
        }
    }
}

private sealed interface Page { data object Home: Page; data object About: Page; data object Player: Page }

@Composable private fun Splash() {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF090A10), Color(0xFF151127)))), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.scale(animateFloatAsState(if (shown) 1f else .75f, label="splash").value).alpha(animateFloatAsState(if (shown) 1f else 0f, label="fade").value)) {
            Logo(86.dp); Text("HASU LIVE TV", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold); Text("LIVE • FAST • SIMPLE", color = MaterialTheme.colorScheme.primary)
            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable private fun Home(repo: LiveTvRepository, tv: Boolean, play: (Channel)->Unit, about: ()->Unit) {
    val channels by repo.channels.collectAsStateWithLifecycle()
    val cats by repo.categories.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf(0) }
    var search by remember { mutableStateOf("") }
    var favorites by remember { mutableStateOf(setOf<String>()) }
    var history by remember { mutableStateOf(listOf<String>()) }
    val filtered = channels.filter { it.enabled && (search.isBlank() || listOf(it.name,it.category,it.country,it.language).any { x -> x.contains(search,true) }) }
    if (tv) TvHome(channels, cats, filtered, search, { search=it }, play, favorites, { id -> favorites = if(id in favorites) favorites-id else favorites+id })
    else MobileHome(channels, cats, filtered, search, { search=it }, tab, { tab=it }, play, favorites, { id -> favorites = if(id in favorites) favorites-id else favorites+id }, history, about)
}

@Composable private fun TvHome(channels: List<Channel>, cats: List<Category>, filtered: List<Channel>, search: String, setSearch:(String)->Unit, play:(Channel)->Unit, fav:Set<String>, toggle:(String)->Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding=PaddingValues(bottom=48.dp)) {
        item { Hero(channels.firstOrNull { it.featured } ?: channels.firstOrNull(), play, tv=true) }
        item { TvTopBar(search,setSearch) }
        item { Section("🔴 Live Now", "Watch channels currently available") { ChannelRow(filtered, play, fav, toggle, 245.dp, true) } }
        item { Section("Categories", "Explore by genre") { ChipRow(cats.map { it.name }) } }
        item { Section("⭐ Featured", "Hand-picked channels") { ChannelRow(channels.filter { it.enabled && it.featured }, play, fav, toggle, 245.dp, true) } }
        item { Section("🌍 Countries", "More ways to discover") { ChipRow(channels.map { it.country }.distinct().take(12)) } }
    }
}

@Composable private fun TvTopBar(search:String,setSearch:(String)->Unit) { Row(Modifier.fillMaxWidth().padding(horizontal=40.dp, vertical=18.dp), verticalAlignment=Alignment.CenterVertically) { Logo(42.dp); Spacer(Modifier.width(14.dp)); Text("HASU LIVE TV", fontWeight=FontWeight.Bold, fontSize=20.sp); Spacer(Modifier.weight(1f)); OutlinedTextField(search,setSearch,label={Text("Search")},singleLine=true,modifier=Modifier.width(330.dp)) } }

@Composable private fun MobileHome(channels:List<Channel>, cats:List<Category>, filtered:List<Channel>, search:String,setSearch:(String)->Unit, tab:Int,setTab:(Int)->Unit,play:(Channel)->Unit,fav:Set<String>,toggle:(String)->Unit,history:List<String>,about:()->Unit) {
    Scaffold(bottomBar={ NavigationBar { listOf(Icons.Default.Home to "Home",Icons.Default.LiveTv to "Live",Icons.Default.Favorite to "Favorites",Icons.Default.Info to "About").forEachIndexed { i,(icon,label)-> NavigationBarItem(tab==i,{setTab(i)},icon={Icon(icon,null)},label={Text(label)}) } } }) { pad ->
        when(tab) {
            0 -> LazyColumn(Modifier.fillMaxSize().padding(pad),contentPadding=PaddingValues(bottom=30.dp)) {
                item { MobileHeader(search,setSearch) }
                item { Hero(channels.firstOrNull { it.featured } ?: channels.firstOrNull(),play,false) }
                item { Section("🔴 Live Now","Start watching instantly") { ChannelRow(filtered,play,fav,toggle,175.dp,false) } }
                item { Section("Categories","Browse your way") { ChipRow(cats.map{it.name}) } }
                item { Section("⭐ Featured","Popular picks") { ChannelRow(channels.filter{it.enabled&&it.featured},play,fav,toggle,175.dp,false) } }
                item { Section("Recently Added","New channels") { ChannelColumn(channels.take(6),play,fav,toggle) } }
            }
            1 -> LazyColumn(Modifier.fillMaxSize().padding(pad),contentPadding=PaddingValues(16.dp)) { item { Text("Live TV",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold); Spacer(Modifier.height(14.dp)); Search(search,setSearch); Spacer(Modifier.height(16.dp)) }; items(filtered,key={it.id}) { ChannelListItem(it,play,it.id in fav){toggle(it.id)} } }
            2 -> LazyColumn(Modifier.fillMaxSize().padding(pad),contentPadding=PaddingValues(16.dp)) { item { Text("Favorites",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold); Spacer(Modifier.height(14.dp)) }; items(channels.filter{it.id in fav}) { ChannelListItem(it,play,true){toggle(it.id)} }; if(fav.isEmpty()) item{Empty("No favorites yet","Tap the heart on a channel to save it.")} }
            else -> About(about)
        }
    }
}

@Composable private fun MobileHeader(search:String,setSearch:(String)->Unit) { Row(Modifier.fillMaxWidth().padding(18.dp),verticalAlignment=Alignment.CenterVertically){Logo(44.dp);Spacer(Modifier.width(12.dp));Column{Text("Hasu Live TV",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold);Text("Live entertainment",color=MaterialTheme.colorScheme.onSurfaceVariant)}}; Search(search,setSearch) }
@Composable private fun Search(v:String,on:(String)->Unit){OutlinedTextField(v,on,modifier=Modifier.fillMaxWidth().padding(horizontal=16.dp),singleLine=true,leadingIcon={Icon(Icons.Default.Search,null)},placeholder={Text("Search channels, countries, languages...")})}

@Composable private fun Hero(channel:Channel?,play:(Channel)->Unit,tv:Boolean){ Box(Modifier.fillMaxWidth().height(if(tv)300.dp else 220.dp).padding(if(tv)0.dp else 16.dp).clip(RoundedCornerShape(if(tv)0.dp else 26.dp)).background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary.copy(.65f),Color(0xFF12131B),MaterialTheme.colorScheme.secondary.copy(.18f))))) { if(channel!=null) Column(Modifier.align(Alignment.CenterStart).padding(if(tv)48.dp else 24.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text("FEATURED • LIVE",color=MaterialTheme.colorScheme.secondary,fontWeight=FontWeight.Bold);Text(channel.name,style=if(tv)MaterialTheme.typography.displaySmall else MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.ExtraBold);Text(channel.description.ifBlank{"Watch live TV with Hasu Live TV."},color=Color.White.copy(.72f),maxLines=2);Button(onClick={play(channel)}){Icon(Icons.Default.PlayArrow,null);Spacer(Modifier.width(6.dp));Text("Watch Live")}} } }

@Composable private fun Section(title:String,subtitle:String="",content: @Composable () -> Unit){ Column(Modifier.padding(top=22.dp,bottom=4.dp)){ Column(Modifier.padding(horizontal=16.dp)){Text(title,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold);if(subtitle.isNotBlank())Text(subtitle,color=MaterialTheme.colorScheme.onSurfaceVariant,fontSize=13.sp)};content() } }
@Composable private fun ChannelRow(list:List<Channel>,play:(Channel)->Unit,fav:Set<String>,toggle:(String)->Unit,w:androidx.compose.ui.unit.Dp,tv:Boolean){LazyRow(contentPadding=PaddingValues(horizontal=16.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)){items(list,key={it.id}){ChannelCard(it,play,w,tv,it.id in fav){toggle(it.id)}}}}
@Composable private fun ChannelColumn(list:List<Channel>,play:(Channel)->Unit,fav:Set<String>,toggle:(String)->Unit){Column(Modifier.padding(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){list.forEach{ChannelListItem(it,play,it.id in fav){toggle(it.id)}}}}
@Composable private fun ChannelCard(c:Channel,play:(Channel)->Unit,w:androidx.compose.ui.unit.Dp,tv:Boolean,isFav:Boolean,toggle:()->Unit){Card(Modifier.width(w).height(if(tv)155.dp else 142.dp).clickable{play(c)},shape=RoundedCornerShape(18.dp)){Column(Modifier.fillMaxSize().padding(14.dp)){Row(verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(if(tv)54.dp else 44.dp).background(MaterialTheme.colorScheme.surfaceVariant,RoundedCornerShape(12.dp)),Alignment.Center){Text(c.channelNumber.toString(),fontWeight=FontWeight.Bold)};Spacer(Modifier.width(10.dp));Column(Modifier.weight(1f)){Text(c.name,fontWeight=FontWeight.Bold,maxLines=1);Text(c.category,color=MaterialTheme.colorScheme.onSurfaceVariant,fontSize=12.sp)}};Spacer(Modifier.weight(1f));Row(verticalAlignment=Alignment.CenterVertically){AssistChip(onClick=toggle,label={Text(if(isFav)"♥" else "♡")});Spacer(Modifier.weight(1f));Text("LIVE",color=MaterialTheme.colorScheme.secondary,fontSize=11.sp,fontWeight=FontWeight.Bold)}}}}
@Composable private fun ChannelListItem(c:Channel,play:(Channel)->Unit,isFav:Boolean,toggle:()->Unit){Card(Modifier.fillMaxWidth().clickable{play(c)},shape=RoundedCornerShape(18.dp)){Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(50.dp).background(MaterialTheme.colorScheme.surfaceVariant,RoundedCornerShape(12.dp)),Alignment.Center){Text(c.channelNumber.toString(),fontWeight=FontWeight.Bold)};Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text(c.name,fontWeight=FontWeight.Bold);Text("${c.category} • ${c.country} • ${c.language}",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};IconButton(toggle){Icon(if(isFav)Icons.Default.Favorite else Icons.Default.FavoriteBorder,null)}}}}
@Composable private fun ChipRow(values:List<String>){LazyRow(contentPadding=PaddingValues(horizontal=16.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){items(values.distinct()){FilterChip(false,{},label={Text(it)})}}}

@Composable private fun About(back:()->Unit){Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally){IconButton(back,Modifier.align(Alignment.Start)){Icon(Icons.Default.ArrowBack,null)};Spacer(Modifier.height(20.dp));Logo(90.dp);Spacer(Modifier.height(16.dp));Text("Hasu Live TV",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.ExtraBold);Text("Premium live streaming experience",color=MaterialTheme.colorScheme.primary);Spacer(Modifier.height(24.dp));Card{Column(Modifier.padding(20.dp)){Text("About",fontWeight=FontWeight.Bold);Spacer(Modifier.height(8.dp));Text("A clean, fast Live TV application designed for Android mobile and TV. Firebase integration is intentionally prepared as a repository layer and can be connected later.");Spacer(Modifier.height(16.dp));Text("Version 1.0.0 • Demo Mode",color=MaterialTheme.colorScheme.onSurfaceVariant)}}}

@Composable private fun Player(c:Channel,back:()->Unit){val context=LocalContext.current;val player=remember(c.streamUrl){ExoPlayer.Builder(context).build().apply{setMediaItem(MediaItem.fromUri(c.streamUrl));prepare();playWhenReady=true}};DisposableEffect(player){onDispose{player.release()}};BackHandlerCompat(back);Column(Modifier.fillMaxSize().background(Color.Black)){AndroidView({PlayerView(it).apply{this.player=player;useController=true}},Modifier.fillMaxWidth().weight(1f));Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically){IconButton(back){Icon(Icons.Default.ArrowBack,null,tint=Color.White)};Column(Modifier.weight(1f)){Text(c.name,color=Color.White,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold);Text("${c.category} • ${c.language}",color=Color.White.copy(.65f))};AssistChip(onClick={},label={Text("LIVE")})}}}
@Composable private fun BackHandlerCompat(on:()->Unit){androidx.activity.compose.BackHandler{on()}}

@Composable private fun AdminApp(repo:LiveTvRepository){var logged by remember{mutableStateOf(false)};if(!logged) AdminLogin{logged=true}else AdminDashboard(repo){logged=false}}
@Composable private fun AdminLogin(done:()->Unit){var email by remember{mutableStateOf("")};var pass by remember{mutableStateOf("")};Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF08090D),Color(0xFF17122A)))),Alignment.Center){Card(Modifier.widthIn(max=430.dp).padding(24.dp)){Column(Modifier.padding(26.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){Logo(58.dp);Text("Admin Console",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Text("Firebase Authentication placeholder",color=MaterialTheme.colorScheme.onSurfaceVariant);OutlinedTextField(email,{email=it},label={Text("Email")},singleLine=true);OutlinedTextField(pass,{pass=it},label={Text("Password")},singleLine=true);Button({if(email.isNotBlank()&&pass.isNotBlank())done()},Modifier.fillMaxWidth()){Text("Continue in Demo Mode")};Text("Production login is enforced through Firebase Auth + Firestore rules after integration.",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}}

@Composable private fun AdminDashboard(repo:LiveTvRepository,logout:()->Unit){val channels by repo.channels.collectAsStateWithLifecycle();var showAdd by remember{mutableStateOf(false)};var editing by remember{mutableStateOf<Channel?>(null)};var deleting by remember{mutableStateOf<Channel?>(null)};Scaffold(topBar={TopAppBar(title={Row(verticalAlignment=Alignment.CenterVertically){Logo(34.dp);Spacer(Modifier.width(10.dp));Text("Hasu Admin")}},actions={IconButton(logout){Icon(Icons.Default.Logout,null)}})},floatingActionButton={FloatingActionButton({showAdd=true}){Icon(Icons.Default.Add,null)}}){pad->LazyColumn(Modifier.fillMaxSize().padding(pad),contentPadding=PaddingValues(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){item{Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){Stat("Channels",channels.size.toString());Stat("Active",channels.count{it.enabled}.toString());Stat("Featured",channels.count{it.featured}.toString())}};item{Text("Channel Management",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)};items(channels,key={it.id}){c->Card{Row(Modifier.fillMaxWidth().padding(14.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(c.name,fontWeight=FontWeight.Bold);Text("#${c.channelNumber} • ${c.category} • ${if(c.enabled)"Enabled" else "Disabled"}",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};IconButton({editing=c}){Icon(Icons.Default.Edit,null)};IconButton({deleting=c}){Icon(Icons.Default.Delete,null)}}}}}};if(showAdd)ChannelEditor(null,{repo.upsertChannel(it);showAdd=false},{showAdd=false});editing?.let{ChannelEditor(it,{repo.upsertChannel(it);editing=null},{editing=null})};deleting?.let{c->AlertDialog(onDismissRequest={deleting=null},title={Text("Delete channel?")},text={Text("Remove ${c.name} from the demo catalog?")},confirmButton={Button({deleting=null;repo.deleteChannel(c.id)}){Text("Delete")}},dismissButton={TextButton({deleting=null}){Text("Cancel")}})}}}
@Composable private fun Stat(name:String,value:String){Card(Modifier.weight(1f)){Column(Modifier.padding(14.dp)){Text(value,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Text(name,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}
@Composable private fun ChannelEditor(existing:Channel?,save:(Channel)->Unit,cancel:()->Unit){var name by remember{mutableStateOf(existing?.name?:"")};var url by remember{mutableStateOf(existing?.streamUrl?:"https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8")};var category by remember{mutableStateOf(existing?.category?:"News")};var number by remember{mutableStateOf((existing?.channelNumber?:1).toString())};AlertDialog(onDismissRequest=cancel,title={Text(if(existing==null)"Add Channel" else "Edit Channel")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){OutlinedTextField(name,{name=it},label={Text("Name")});OutlinedTextField(url,{url=it},label={Text("Stream URL")});OutlinedTextField(category,{category=it},label={Text("Category")});OutlinedTextField(number,{number=it},label={Text("Channel Number")})}},confirmButton={Button({if(name.isNotBlank()&&url.isNotBlank())save((existing?:Channel(java.util.UUID.randomUUID().toString(),"",url,category,"Global","English",number.toIntOrNull()?:1)).copy(name=name,streamUrl=url,category=category,channelNumber=number.toIntOrNull()?:1))}){Text("Save")}},dismissButton={TextButton(cancel){Text("Cancel")}})}

@Composable private fun Empty(title:String,text:String){Box(Modifier.fillMaxWidth().padding(50.dp),Alignment.Center){Column(horizontalAlignment=Alignment.CenterHorizontally){Icon(Icons.Default.LiveTv,null,Modifier.size(48.dp),tint=MaterialTheme.colorScheme.primary);Spacer(Modifier.height(12.dp));Text(title,fontWeight=FontWeight.Bold);Text(text,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}
@Composable private fun Logo(size:androidx.compose.ui.unit.Dp){Box(Modifier.size(size).background(Brush.linearGradient(listOf(Color(0xFF9B7BFF),Color(0xFF5ED7D0))),RoundedCornerShape(size/4)),Alignment.Center){Text("H",fontSize=(size.value*.42f).sp,fontWeight=FontWeight.ExtraBold,color=Color.White)}}
@Composable private fun HasuTheme(content: @Composable () -> Unit){MaterialTheme(colorScheme=darkColorScheme(primary=Color(0xFF9B7BFF),secondary=Color(0xFF5ED7D0),background=Color(0xFF08090D),surface=Color(0xFF11131A),surfaceVariant=Color(0xFF1B1E27),onBackground=Color(0xFFF5F5F7),onSurface=Color(0xFFF5F5F7)),content=content)}
