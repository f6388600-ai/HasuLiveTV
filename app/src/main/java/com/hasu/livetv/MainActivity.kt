@file:OptIn(ExperimentalMaterial3Api::class)

package com.hasu.livetv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.hasu.livetv.data.LiveTvRepository
import com.hasu.livetv.model.Category
import com.hasu.livetv.model.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            HasuTheme {
                App(
                    (application as HasuLiveTvApplication).repo
                )
            }
        }
    }
}

/* =========================================================
   APP ROOT
   ========================================================= */

@Composable
private fun App(repo: LiveTvRepository) {
    var splash by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(900)
        splash = false
    }
    if (splash) {
        Splash()
        return
    }
    if (BuildConfig.EDITION == "admin") {
        AdminDashboard(repo)
    } else {
        MainUserApp(repo)
    }
}

@Composable
private fun MainUserApp(repo: LiveTvRepository) {
    var page by remember { mutableStateOf<Page>(Page.Home) }
    var selected by remember { mutableStateOf<Channel?>(null) }
    AnimatedContent(
        targetState = page,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "navigation"
    ) { currentPage ->
        when (currentPage) {
            Page.Home -> Home(
                repo = repo,
                tv = BuildConfig.EDITION == "tv",
                play = { selected = it; page = Page.Player },
                about = { page = Page.About }
            )
            Page.About -> About { page = Page.Home }
            Page.Player -> selected?.let { channel ->
                Player(channel) {
                    selected = null
                    page = Page.Home
                }
            }
        }
    }
}

private sealed interface Page {

    data object Home : Page

    data object About : Page

    data object Player : Page
}

/* =========================================================
   SPLASH
   ========================================================= */

@Composable
private fun Splash() {

    var shown by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(Unit) {
        shown = true
    }

    val scale by animateFloatAsState(
        if (shown) 1f else .75f,
        label = "splashScale"
    )

    val alpha by animateFloatAsState(
        if (shown) 1f else 0f,
        label = "splashAlpha"
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF090A10),
                        Color(0xFF151127)
                    )
                )
            ),
        Alignment.Center
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .scale(scale)
                .alpha(alpha)
        ) {

            Logo(86.dp)

            Text(
                "HASU LIVE TV",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Text(
                "LIVE • FAST • SIMPLE",
                color = MaterialTheme.colorScheme.primary
            )

            CircularProgressIndicator(
                strokeWidth = 2.dp,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/* =========================================================
   HOME
   ========================================================= */

@Composable
private fun Home(
    repo: LiveTvRepository,
    tv: Boolean,
    play: (Channel) -> Unit,
    about: () -> Unit
) {

    val channels by repo.channels
        .collectAsStateWithLifecycle()

    val cats by repo.categories
        .collectAsStateWithLifecycle()

    var tab by remember {
        mutableIntStateOf(0)
    }

    var search by remember {
        mutableStateOf("")
    }

    var favorites by remember {
        mutableStateOf(setOf<String>())
    }

    val filtered = channels.filter {

        it.enabled &&
                (
                        search.isBlank() ||
                                listOf(
                                    it.name,
                                    it.category,
                                    it.country,
                                    it.language
                                ).any { value ->
                                    value.contains(
                                        search,
                                        true
                                    )
                                }
                        )
    }

    if (tv) {

        TvHome(
            channels = channels,
            cats = cats,
            filtered = filtered,
            search = search,
            setSearch = {
                search = it
            },
            play = play,
            fav = favorites,
            toggle = { id ->
                favorites = if (id in favorites) favorites - id else favorites + id
            }
        )

    } else {

        MobileHome(
            channels = channels,
            cats = cats,
            filtered = filtered,
            search = search,
            setSearch = {
                search = it
            },
            tab = tab,
            setTab = {
                tab = it
            },
            play = play,
            fav = favorites,
            toggle = { id ->
                favorites =
                    if (id in favorites) {
                        favorites - id
                    } else {
                        favorites + id
                    }
            },
            about = about
        )
    }
}

/* =========================================================
   TV HOME
   ========================================================= */

@Composable
private fun TvHome(
    channels: List<Channel>,
    cats: List<Category>,
    filtered: List<Channel>,
    search: String,
    setSearch: (String) -> Unit,
    play: (Channel) -> Unit,
    fav: Set<String>,
    toggle: (String) -> Unit
) {

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            bottom = 48.dp
        )
    ) {

        item {

            Hero(
                channels.firstOrNull {
                    it.featured
                } ?: channels.firstOrNull(),
                play,
                true
            )
        }

        item {

            TvTopBar(search, setSearch)
        }

        item {

            Section(
                "🔴 Live Now",
                "Watch channels currently available"
            ) {

                ChannelRow(
                    filtered,
                    play,
                    fav,
                    toggle,
                    245.dp,
                    true
                )
            }
        }

        item {

            Section(
                "Categories",
                "Explore by genre"
            ) {

                ChipRow(
                    cats.map {
                        it.name
                    }
                )
            }
        }

        item {

            Section(
                "⭐ Featured",
                "Hand-picked channels"
            ) {

                ChannelRow(
                    channels.filter {
                        it.enabled && it.featured
                    },
                    play,
                    fav,
                    toggle,
                    245.dp,
                    true
                )
            }
        }

        item {

            Section(
                "🌍 Countries",
                "More ways to discover"
            ) {

                ChipRow(
                    channels
                        .map {
                            it.country
                        }
                        .distinct()
                        .take(12)
                )
            }
        }
    }
}

@Composable
private fun TvTopBar(
    search: String,
    setSearch: (String) -> Unit
) {

    Row(
        Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 40.dp,
                vertical = 18.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Logo(42.dp)

        Spacer(
            Modifier.width(14.dp)
        )

        Text(
            "HASU LIVE TV",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )

        Spacer(
            Modifier.weight(1f)
        )

        OutlinedTextField(
            value = search,
            onValueChange = setSearch,
            label = {
                Text("Search")
            },
            singleLine = true,
            modifier = Modifier.width(330.dp)
        )

        Spacer(Modifier.width(12.dp))
    }
}

/* =========================================================
   MOBILE HOME
   ========================================================= */

@Composable
private fun MobileHome(
    channels: List<Channel>,
    cats: List<Category>,
    filtered: List<Channel>,
    search: String,
    setSearch: (String) -> Unit,
    tab: Int,
    setTab: (Int) -> Unit,
    play: (Channel) -> Unit,
    fav: Set<String>,
    toggle: (String) -> Unit,
    about: () -> Unit
) {

    Scaffold(

        bottomBar = {

            NavigationBar {

                listOf(
                    Icons.Default.Home to "Home",
                    Icons.Default.LiveTv to "Live",
                    Icons.Default.Favorite to "Favorites",
                    Icons.Default.Info to "About"
                ).forEachIndexed { index, item ->

                    NavigationBarItem(
                        selected = tab == index,
                        onClick = {
                            setTab(index)
                        },
                        icon = {
                            Icon(
                                item.first,
                                item.second
                            )
                        },
                        label = {
                            Text(item.second)
                        }
                    )
                }
            }
        }

    ) { pad ->

        when (tab) {

            0 -> {

                LazyColumn(
                    Modifier
                        .fillMaxSize()
                        .padding(pad),
                    contentPadding = PaddingValues(
                        bottom = 30.dp
                    )
                ) {

                    item {

                        MobileHeader(
                            search,
                            setSearch
                        )
                    }

                    item {

                        Hero(
                            channels.firstOrNull {
                                it.featured
                            } ?: channels.firstOrNull(),
                            play,
                            false
                        )
                    }

                    item {

                        Section(
                            "🔴 Live Now",
                            "Start watching instantly"
                        ) {

                            ChannelRow(
                                filtered,
                                play,
                                fav,
                                toggle,
                                175.dp,
                                false
                            )
                        }
                    }

                    item {

                        Section(
                            "Categories",
                            "Browse your way"
                        ) {

                            ChipRow(
                                cats.map {
                                    it.name
                                }
                            )
                        }
                    }

                    item {

                        Section(
                            "⭐ Featured",
                            "Popular picks"
                        ) {

                            ChannelRow(
                                channels.filter {
                                    it.enabled &&
                                            it.featured
                                },
                                play,
                                fav,
                                toggle,
                                175.dp,
                                false
                            )
                        }
                    }

                    item {

                        Section(
                            "Recently Added",
                            "New channels"
                        ) {

                            ChannelColumn(
                                channels.take(6),
                                play,
                                fav,
                                toggle
                            )
                        }
                    }
                }
            }

            1 -> {

                LazyColumn(
                    Modifier
                        .fillMaxSize()
                        .padding(pad),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(10.dp)
                ) {

                    item {

                        Text(
                            "Live TV",
                            style =
                                MaterialTheme.typography
                                    .headlineMedium,
                            fontWeight =
                                FontWeight.Bold
                        )

                        Spacer(
                            Modifier.height(14.dp)
                        )

                        Search(
                            search,
                            setSearch
                        )

                        Spacer(
                            Modifier.height(16.dp)
                        )
                    }

                    items(
                        filtered,
                        key = {
                            it.id
                        }
                    ) {

                        ChannelListItem(
                            it,
                            play,
                            it.id in fav
                        ) {
                            toggle(it.id)
                        }
                    }

                    if (filtered.isEmpty()) {

                        item {

                            Empty(
                                "No channels found",
                                "Try another search."
                            )
                        }
                    }
                }
            }

            2 -> {

                val favorites =
                    channels.filter {
                        it.id in fav
                    }

                LazyColumn(
                    Modifier
                        .fillMaxSize()
                        .padding(pad),
                    contentPadding =
                        PaddingValues(16.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(10.dp)
                ) {

                    item {

                        Text(
                            "Favorites",
                            style =
                                MaterialTheme.typography
                                    .headlineMedium,
                            fontWeight =
                                FontWeight.Bold
                        )

                        Spacer(
                            Modifier.height(14.dp)
                        )
                    }

                    items(
                        favorites,
                        key = {
                            it.id
                        }
                    ) {

                        ChannelListItem(
                            it,
                            play,
                            true
                        ) {
                            toggle(it.id)
                        }
                    }

                    if (favorites.isEmpty()) {

                        item {

                            Empty(
                                "No favorites yet",
                                "Tap the heart on a channel to save it."
                            )
                        }
                    }
                }
            }

            else -> {

                About(back = about)
            }
        }
    }
}

/* =========================================================
   MOBILE HEADER
   ========================================================= */

@Composable
private fun MobileHeader(
    search: String,
    setSearch: (String) -> Unit
) {

    Column(
        Modifier.fillMaxWidth()
    ) {

        Row(
            Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Logo(44.dp)

            Spacer(
                Modifier.width(12.dp)
            )

            Column {

                Text(
                    "Hasu Live TV",
                    style =
                        MaterialTheme.typography.titleLarge,
                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    "Live entertainment",
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                )
            }
        }

        Search(
            search,
            setSearch
        )
    }
}

/* =========================================================
   SEARCH
   ========================================================= */

@Composable
private fun Search(
    value: String,
    onValueChange: (String) -> Unit
) {

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        singleLine = true,
        leadingIcon = {
            Icon(
                Icons.Default.Search,
                "Search"
            )
        },
        placeholder = {
            Text(
                "Search channels, countries, languages..."
            )
        }
    )
}

/* =========================================================
   HERO
   ========================================================= */

@Composable
private fun Hero(
    channel: Channel?,
    play: (Channel) -> Unit,
    tv: Boolean
) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(if (tv) 320.dp else 230.dp)
            .padding(if (tv) 0.dp else 16.dp)
            .clip(RoundedCornerShape(if (tv) 0.dp else 26.dp))
            .background(Color(0xFF151722))
    ) {
        if (channel?.bannerUrl?.isNotBlank() == true) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(channel.bannerUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = channel.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.42f
            )
        }

        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Black.copy(alpha = .82f),
                            Color.Black.copy(alpha = .35f),
                            Color.Transparent
                        )
                    )
                )
        )

        if (channel != null) {
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(if (tv) 48.dp else 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ChannelLogo(channel, if (tv) 86.dp else 68.dp)
                Spacer(Modifier.width(18.dp))
                Column(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "FEATURED • LIVE",
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Text(
                        channel.name,
                        style = if (tv) MaterialTheme.typography.displaySmall else MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        channel.description.ifBlank { "Watch live TV with Hasu Live TV." },
                        color = Color.White.copy(.72f),
                        maxLines = 2
                    )
                    Button(onClick = { play(channel) }) {
                        Icon(Icons.Default.PlayArrow, null)
                        Spacer(Modifier.width(6.dp))
                        Text("Watch Live")
                    }
                }
            }
        } else {
            Empty("No featured channel", "Add a channel from the Admin app.")
        }
    }
}

@Composable
private fun ChannelLogo(channel: Channel, size: Dp) {
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (channel.logoUrl.isNotBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(channel.logoUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = channel.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                channel.name.take(1).uppercase(),
                fontWeight = FontWeight.ExtraBold,
                fontSize = (size.value * .36f).sp
            )
        }
    }
}

/* =========================================================
   SECTION
   ========================================================= */

@Composable
private fun Section(
    title: String,
    subtitle: String = "",
    content: @Composable () -> Unit
) {

    Column(
        Modifier.padding(
            top = 22.dp,
            bottom = 4.dp
        )
    ) {

        Column(
            Modifier.padding(
                horizontal = 16.dp
            )
        ) {

            Text(
                title,
                style =
                    MaterialTheme.typography.titleLarge,
                fontWeight =
                    FontWeight.Bold
            )

            if (subtitle.isNotBlank()) {

                Text(
                    subtitle,
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant,
                    fontSize = 13.sp
                )
            }
        }

        content()
    }
}

/* =========================================================
   CHANNEL ROW
   ========================================================= */

@Composable
private fun ChannelRow(
    list: List<Channel>,
    play: (Channel) -> Unit,
    fav: Set<String>,
    toggle: (String) -> Unit,
    width: Dp,
    tv: Boolean
) {

    LazyRow(
        contentPadding =
            PaddingValues(
                horizontal = 16.dp
            ),
        horizontalArrangement =
            Arrangement.spacedBy(12.dp)
    ) {

        items(
            list,
            key = {
                it.id
            }
        ) {

            ChannelCard(
                it,
                play,
                width,
                tv,
                it.id in fav
            ) {

                toggle(it.id)
            }
        }
    }
}

/* =========================================================
   CHANNEL COLUMN
   ========================================================= */

@Composable
private fun ChannelColumn(
    list: List<Channel>,
    play: (Channel) -> Unit,
    fav: Set<String>,
    toggle: (String) -> Unit
) {

    Column(
        Modifier.padding(
            horizontal = 16.dp
        ),
        verticalArrangement =
            Arrangement.spacedBy(10.dp)
    ) {

        list.forEach {

            ChannelListItem(
                it,
                play,
                it.id in fav
            ) {

                toggle(it.id)
            }
        }
    }
}

/* =========================================================
   CHANNEL CARD
   ========================================================= */

@Composable
private fun ChannelCard(
    c: Channel,
    play: (Channel) -> Unit,
    width: Dp,
    tv: Boolean,
    isFav: Boolean,
    toggle: () -> Unit
) {
    Card(
        Modifier
            .width(width)
            .clickable { play(c) },
        shape = RoundedCornerShape(18.dp)
    ) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(if (tv) 118.dp else 96.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (c.bannerUrl.isNotBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data(c.bannerUrl).crossfade(true).build(),
                        contentDescription = c.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        alpha = .82f
                    )
                }
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                ) { ChannelLogo(c, if (tv) 54.dp else 46.dp) }
                Text(
                    "LIVE",
                    modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(Modifier.padding(12.dp)) {
                Text(c.name, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(
                    "#${c.channelNumber} • ${c.category}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    maxLines = 1
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AssistChip(
                        onClick = toggle,
                        label = { Text(if (isFav) "♥" else "♡") }
                    )
                    Spacer(Modifier.weight(1f))
                    Text(c.language, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

/* =========================================================
   CHANNEL LIST ITEM
   ========================================================= */

@Composable
private fun ChannelListItem(
    c: Channel,
    play: (Channel) -> Unit,
    isFav: Boolean,
    toggle: () -> Unit
) {
    Card(
        Modifier.fillMaxWidth().clickable { play(c) },
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            ChannelLogo(c, 54.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(c.name, fontWeight = FontWeight.Bold)
                Text(
                    "#${c.channelNumber} • ${c.category} • ${c.country} • ${c.language}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = toggle) {
                Icon(if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Favorite")
            }
        }
    }
}

/* =========================================================
   CHIPS
   ========================================================= */

@Composable
private fun ChipRow(
    values: List<String>
) {

    LazyRow(
        contentPadding =
            PaddingValues(
                horizontal = 16.dp
            ),
        horizontalArrangement =
            Arrangement.spacedBy(8.dp)
    ) {

        items(
            values.distinct()
        ) {

            FilterChip(
                selected = false,
                onClick = {},
                label = {
                    Text(it)
                }
            )
        }
    }
}

/* =========================================================
   ABOUT
   ========================================================= */

@Composable
private fun About(
    back: () -> Unit
) {

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(
                rememberScrollState()
            )
            .padding(24.dp),
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            IconButton(
                onClick = back
            ) {

                Icon(
                    Icons.Default.ArrowBack,
                    "Back"
                )
            }

            Spacer(Modifier.weight(1f))
        }

        Spacer(
            Modifier.height(20.dp)
        )

        Logo(90.dp)

        Spacer(
            Modifier.height(16.dp)
        )

        Text(
            "Hasu Live TV",
            style =
                MaterialTheme.typography
                    .headlineMedium,
            fontWeight =
                FontWeight.ExtraBold
        )

        Text(
            "Premium live streaming experience",
            color =
                MaterialTheme.colorScheme.primary
        )

        Spacer(
            Modifier.height(24.dp)
        )

        Card {

            Column(
                Modifier.padding(20.dp)
            ) {

                Text(
                    "About",
                    fontWeight =
                        FontWeight.Bold
                )

                Spacer(
                    Modifier.height(8.dp)
                )

                Text(
                    "A clean, fast Live TV application designed for Android mobile and TV."
                )

                Spacer(
                    Modifier.height(16.dp)
                )

                Text(
                    "Version 1.1.0 • Local Catalog",
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                )
            }
        }
    }
}

/* =========================================================
   PLAYER
   ========================================================= */

@Composable
private fun Player(
    c: Channel,
    back: () -> Unit
) {

    val context =
        LocalContext.current

    val player = remember(
        c.streamUrl
    ) {

        ExoPlayer
            .Builder(context)
            .build()
            .apply {

                setMediaItem(
                    MediaItem.fromUri(
                        c.streamUrl
                    )
                )

                prepare()

                playWhenReady = true
            }
    }

    DisposableEffect(player) {

        onDispose {
            player.release()
        }
    }

    BackHandler(
        onBack = back
    )

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {

        AndroidView(

            factory = { contextView ->

                PlayerView(contextView).apply {

                    this.player = player

                    useController = true
                }
            },

            update = { playerView ->

                playerView.player = player
            },

            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            IconButton(
                onClick = back
            ) {

                Icon(
                    Icons.Default.ArrowBack,
                    "Back",
                    tint = Color.White
                )
            }

            Column(
                Modifier.weight(1f)
            ) {

                Text(
                    c.name,
                    color = Color.White,
                    style =
                        MaterialTheme.typography
                            .titleLarge,
                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    "${c.category} • ${c.language}",
                    color =
                        Color.White.copy(
                            alpha = .65f
                        )
                )
            }

            AssistChip(
                onClick = {},
                label = {
                    Text("LIVE")
                }
            )
        }
    }
}

/* =========================================================
   ADMIN CONSOLE — NO LOGIN
   ========================================================= */

@Composable
private fun AdminDashboard(
    repo: LiveTvRepository
) {

    val scope =
        rememberCoroutineScope()

    val channels by repo.channels
        .collectAsStateWithLifecycle()

    var showAdd by remember {
        mutableStateOf(false)
    }

    var editing by remember {
        mutableStateOf<Channel?>(null)
    }

    var deleting by remember {
        mutableStateOf<Channel?>(null)
    }

    Scaffold(

        topBar = {

            TopAppBar(

                title = {

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Logo(34.dp)

                        Spacer(
                            Modifier.width(10.dp)
                        )

                        Text("Hasu Live TV • Admin")
                    }
                },
            )
        },

        floatingActionButton = {

            FloatingActionButton(
                onClick = {
                    showAdd = true
                }
            ) {

                Icon(
                    Icons.Default.Add,
                    "Add"
                )
            }
        }

    ) { pad ->

        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(pad),
            contentPadding =
                PaddingValues(18.dp),
            verticalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {

            item {

                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(10.dp)
                ) {

                    Stat(
                        "Channels",
                        channels.size.toString()
                    )

                    Stat(
                        "Active",
                        channels.count {
                            it.enabled
                        }.toString()
                    )

                    Stat(
                        "Featured",
                        channels.count {
                            it.featured
                        }.toString()
                    )
                }
            }

            item {

                Text(
                    "Channel Management",
                    style =
                        MaterialTheme.typography
                            .headlineSmall,
                    fontWeight =
                        FontWeight.Bold
                )
            }

            items(
                channels,
                key = {
                    it.id
                }
            ) { c ->

                Card {

                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Column(
                            Modifier.weight(1f)
                        ) {

                            Text(
                                c.name,
                                fontWeight =
                                    FontWeight.Bold
                            )

                            Text(
                                "#${c.channelNumber} • ${c.category} • ${
                                    if (c.enabled)
                                        "Enabled"
                                    else
                                        "Disabled"
                                }",
                                fontSize = 12.sp,
                                color =
                                    MaterialTheme.colorScheme
                                        .onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = {
                                editing = c
                            }
                        ) {

                            Icon(
                                Icons.Default.Edit,
                                "Edit"
                            )
                        }

                        IconButton(
                            onClick = {
                                deleting = c
                            }
                        ) {

                            Icon(
                                Icons.Default.Delete,
                                "Delete"
                            )
                        }
                    }
                }
            }

            if (channels.isEmpty()) {

                item {

                    Empty(
                        "No channels",
                        "Add your first channel."
                    )
                }
            }
        }
    }

    if (showAdd) {

        ChannelEditor(
            existing = null,

            save = { channel ->

                scope.launch {

                    repo.upsertChannel(
                        channel
                    )

                    showAdd = false
                }
            },

            cancel = {
                showAdd = false
            }
        )
    }

    editing?.let { channel ->

        ChannelEditor(
            existing = channel,

            save = { updated ->

                scope.launch {

                    repo.upsertChannel(
                        updated
                    )

                    editing = null
                }
            },

            cancel = {
                editing = null
            }
        )
    }

    deleting?.let { c ->

        AlertDialog(

            onDismissRequest = {
                deleting = null
            },

            title = {
                Text("Delete channel?")
            },

            text = {
                Text(
                    "Remove ${c.name} from the catalog?"
                )
            },

            confirmButton = {

                Button(
                    onClick = {

                        scope.launch {

                            repo.deleteChannel(
                                c.id
                            )

                            deleting = null
                        }
                    }
                ) {

                    Text("Delete")
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        deleting = null
                    }
                ) {

                    Text("Cancel")
                }
            }
        )
    }
}

/* =========================================================
   STAT
   ========================================================= */

@Composable
private fun RowScope.Stat(
    name: String,
    value: String
) {

    Card(
        Modifier.weight(1f)
    ) {

        Column(
            Modifier.padding(14.dp)
        ) {

            Text(
                value,
                style =
                    MaterialTheme.typography
                        .headlineSmall,
                fontWeight =
                    FontWeight.Bold
            )

            Text(
                name,
                color =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant
            )
        }
    }
}

/* =========================================================
   CHANNEL EDITOR
   ========================================================= */

@Composable
private fun ChannelEditor(
    existing: Channel?,
    save: (Channel) -> Unit,
    cancel: () -> Unit
) {
    var name by remember(existing?.id) { mutableStateOf(existing?.name ?: "") }
    var url by remember(existing?.id) { mutableStateOf(existing?.streamUrl ?: "") }
    var category by remember(existing?.id) { mutableStateOf(existing?.category ?: "News") }
    var country by remember(existing?.id) { mutableStateOf(existing?.country ?: "Bangladesh") }
    var language by remember(existing?.id) { mutableStateOf(existing?.language ?: "Bangla") }
    var number by remember(existing?.id) { mutableStateOf((existing?.channelNumber ?: 1).toString()) }
    var logoUrl by remember(existing?.id) { mutableStateOf(existing?.logoUrl ?: "") }
    var bannerUrl by remember(existing?.id) { mutableStateOf(existing?.bannerUrl ?: "") }
    var description by remember(existing?.id) { mutableStateOf(existing?.description ?: "") }
    var enabled by remember(existing?.id) { mutableStateOf(existing?.enabled ?: true) }
    var featured by remember(existing?.id) { mutableStateOf(existing?.featured ?: false) }
    var popular by remember(existing?.id) { mutableStateOf(existing?.popular ?: false) }
    var sortOrder by remember(existing?.id) { mutableStateOf((existing?.sortOrder ?: 0).toString()) }

    AlertDialog(
        onDismissRequest = cancel,
        title = { Text(if (existing == null) "Add Channel" else "Edit Channel") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Channel name") }, singleLine = true)
                OutlinedTextField(url, { url = it }, Modifier.fillMaxWidth(), label = { Text("Stream URL") }, singleLine = true)
                OutlinedTextField(logoUrl, { logoUrl = it }, Modifier.fillMaxWidth(), label = { Text("Logo image URL") }, singleLine = true)
                OutlinedTextField(bannerUrl, { bannerUrl = it }, Modifier.fillMaxWidth(), label = { Text("Banner image URL") }, singleLine = true)
                OutlinedTextField(category, { category = it }, Modifier.fillMaxWidth(), label = { Text("Category") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(country, { country = it }, Modifier.weight(1f), label = { Text("Country") }, singleLine = true)
                    OutlinedTextField(language, { language = it }, Modifier.weight(1f), label = { Text("Language") }, singleLine = true)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(number, { number = it.filter(Char::isDigit) }, Modifier.weight(1f), label = { Text("Channel #") }, singleLine = true)
                    OutlinedTextField(sortOrder, { sortOrder = it.filter(Char::isDigit) }, Modifier.weight(1f), label = { Text("Sort order") }, singleLine = true)
                }
                OutlinedTextField(
                    description,
                    { description = it },
                    Modifier.fillMaxWidth().heightIn(min = 80.dp),
                    label = { Text("Description") }
                )
                SettingToggle("Enabled", enabled) { enabled = it }
                SettingToggle("Featured on Home", featured) { featured = it }
                SettingToggle("Popular", popular) { popular = it }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank() || url.isBlank()) return@Button
                    val channel = (existing ?: Channel(
                        id = UUID.randomUUID().toString(),
                        name = name,
                        streamUrl = url,
                        category = category,
                        country = country,
                        language = language,
                        channelNumber = number.toIntOrNull() ?: 1
                    )).copy(
                        name = name.trim(),
                        streamUrl = url.trim(),
                        category = category.trim().ifBlank { "General" },
                        country = country.trim().ifBlank { "Global" },
                        language = language.trim().ifBlank { "English" },
                        channelNumber = number.toIntOrNull() ?: 1,
                        enabled = enabled,
                        featured = featured,
                        popular = popular,
                        description = description.trim(),
                        logoUrl = logoUrl.trim(),
                        bannerUrl = bannerUrl.trim(),
                        sortOrder = sortOrder.toIntOrNull() ?: 0
                    )
                    save(channel)
                }
            ) { Text("Save Channel") }
        },
        dismissButton = { TextButton(onClick = cancel) { Text("Cancel") } }
    )
}

@Composable
private fun SettingToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(label, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
    }
}

/* =========================================================
   EMPTY
   ========================================================= */

@Composable
private fun Empty(
    title: String,
    text: String
) {

    Box(
        Modifier
            .fillMaxWidth()
            .padding(50.dp),
        Alignment.Center
    ) {

        Column(
            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Icon(
                Icons.Default.LiveTv,
                null,
                Modifier.size(48.dp),
                tint =
                    MaterialTheme.colorScheme
                        .primary
            )

            Spacer(
                Modifier.height(12.dp)
            )

            Text(
                title,
                fontWeight =
                    FontWeight.Bold
            )

            Text(
                text,
                color =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant
            )
        }
    }
}

/* =========================================================
   LOGO
   ========================================================= */

@Composable
private fun Logo(
    size: Dp
) {

    Box(

        Modifier
            .size(size)
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF9B7BFF),
                        Color(0xFF5ED7D0)
                    )
                ),
                RoundedCornerShape(
                    size / 4
                )
            ),

        Alignment.Center
    ) {

        Text(
            "H",
            fontSize =
                (size.value * .42f).sp,
            fontWeight =
                FontWeight.ExtraBold,
            color = Color.White
        )
    }
}

/* =========================================================
   THEME
   ========================================================= */

@Composable
private fun HasuTheme(
    content: @Composable () -> Unit
) {

    MaterialTheme(

        colorScheme = darkColorScheme(

            primary =
                Color(0xFF9B7BFF),

            secondary =
                Color(0xFF5ED7D0),

            background =
                Color(0xFF090A10),

            surface =
                Color(0xFF11131B),

            surfaceVariant =
                Color(0xFF1B1E28)
        ),

        typography =
            Typography(),

        content = content
    )
}