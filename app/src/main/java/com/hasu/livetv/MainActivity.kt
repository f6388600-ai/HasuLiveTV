package com.hasu.livetv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.lazy.itemsIndexed
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

@file:OptIn(ExperimentalMaterial3Api::class)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            HasuTheme {
                App(
                    repo = (application as HasuLiveTvApplication).repo
                )
            }
        }
    }
}

/* -------------------------------------------------------
   APP
------------------------------------------------------- */

@Composable
private fun App(repo: LiveTvRepository) {

    var splash by remember {
        mutableStateOf(true)
    }

    var page by remember {
        mutableStateOf<Page>(Page.Home)
    }

    var selected by remember {
        mutableStateOf<Channel?>(null)
    }

    LaunchedEffect(Unit) {
        delay(900)
        splash = false
    }

    if (splash) {
        Splash()
        return
    }

    if (BuildConfig.EDITION == "admin") {
        AdminApp(repo)
        return
    }

    AnimatedContent(
        targetState = page,
        transitionSpec = {
            fadeIn() togetherWith fadeOut()
        },
        label = "navigation"
    ) { currentPage ->

        when (currentPage) {

            Page.Home -> {
                Home(
                    repo = repo,
                    tv = BuildConfig.EDITION == "tv",
                    play = {
                        selected = it
                        page = Page.Player
                    },
                    about = {
                        page = Page.About
                    }
                )
            }

            Page.About -> {
                About {
                    page = Page.Home
                }
            }

            Page.Player -> {
                val channel = selected

                if (channel != null) {
                    Player(
                        channel = channel,
                        back = {
                            selected = null
                            page = Page.Home
                        }
                    )
                } else {
                    Home(
                        repo = repo,
                        tv = BuildConfig.EDITION == "tv",
                        play = {
                            selected = it
                            page = Page.Player
                        },
                        about = {
                            page = Page.About
                        }
                    )
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

/* -------------------------------------------------------
   SPLASH
------------------------------------------------------- */

@Composable
private fun Splash() {

    var shown by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(Unit) {
        shown = true
    }

    val scale by animateFloatAsState(
        targetValue = if (shown) 1f else 0.75f,
        label = "splashScale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        label = "splashAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF090A10),
                        Color(0xFF151127)
                    )
                )
            ),
        contentAlignment = Alignment.Center
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
                text = "HASU LIVE TV",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Text(
                text = "LIVE • FAST • SIMPLE",
                color = MaterialTheme.colorScheme.primary
            )

            CircularProgressIndicator(
                strokeWidth = 2.dp,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/* -------------------------------------------------------
   HOME
------------------------------------------------------- */

@Composable
private fun Home(
    repo: LiveTvRepository,
    tv: Boolean,
    play: (Channel) -> Unit,
    about: () -> Unit
) {

    val channels by repo.channels.collectAsStateWithLifecycle()

    val categories by repo.categories.collectAsStateWithLifecycle()

    var tab by remember {
        mutableIntStateOf(0)
    }

    var search by remember {
        mutableStateOf("")
    }

    var favorites by remember {
        mutableStateOf(setOf<String>())
    }

    var history by remember {
        mutableStateOf(listOf<String>())
    }

    val filtered = channels.filter { channel ->

        channel.enabled &&
                (
                        search.isBlank() ||
                                listOf(
                                    channel.name,
                                    channel.category,
                                    channel.country,
                                    channel.language
                                ).any {
                                    it.contains(
                                        search,
                                        ignoreCase = true
                                    )
                                }
                        )
    }

    if (tv) {

        TvHome(
            channels = channels,
            categories = categories,
            filtered = filtered,
            search = search,
            setSearch = {
                search = it
            },
            play = play,
            favorites = favorites,
            toggleFavorite = { id ->

                favorites =
                    if (id in favorites) {
                        favorites - id
                    } else {
                        favorites + id
                    }
            }
        )

    } else {

        MobileHome(
            channels = channels,
            categories = categories,
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
            favorites = favorites,
            toggleFavorite = { id ->

                favorites =
                    if (id in favorites) {
                        favorites - id
                    } else {
                        favorites + id
                    }
            },
            history = history,
            about = about
        )
    }
}

/* -------------------------------------------------------
   TV HOME
------------------------------------------------------- */

@Composable
private fun TvHome(
    channels: List<Channel>,
    categories: List<Category>,
    filtered: List<Channel>,
    search: String,
    setSearch: (String) -> Unit,
    play: (Channel) -> Unit,
    favorites: Set<String>,
    toggleFavorite: (String) -> Unit
) {

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 48.dp)
    ) {

        item {

            Hero(
                channel = channels.firstOrNull { it.featured }
                    ?: channels.firstOrNull(),
                play = play,
                tv = true
            )
        }

        item {

            TvTopBar(
                search = search,
                setSearch = setSearch
            )
        }

        item {

            Section(
                title = "🔴 Live Now",
                subtitle = "Watch channels currently available"
            ) {

                ChannelRow(
                    list = filtered,
                    play = play,
                    favorites = favorites,
                    toggleFavorite = toggleFavorite,
                    width = 245.dp,
                    tv = true
                )
            }
        }

        item {

            Section(
                title = "Categories",
                subtitle = "Explore by genre"
            ) {

                ChipRow(
                    values = categories.map { it.name }
                )
            }
        }

        item {

            Section(
                title = "⭐ Featured",
                subtitle = "Hand-picked channels"
            ) {

                ChannelRow(
                    list = channels.filter {
                        it.enabled && it.featured
                    },
                    play = play,
                    favorites = favorites,
                    toggleFavorite = toggleFavorite,
                    width = 245.dp,
                    tv = true
                )
            }
        }

        item {

            Section(
                title = "🌍 Countries",
                subtitle = "More ways to discover"
            ) {

                ChipRow(
                    values = channels
                        .map { it.country }
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 40.dp,
                vertical = 18.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Logo(42.dp)

        Spacer(
            modifier = Modifier.width(14.dp)
        )

        Text(
            text = "HASU LIVE TV",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )

        Spacer(
            modifier = Modifier.weight(1f)
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
    }
}

/* -------------------------------------------------------
   MOBILE HOME
------------------------------------------------------- */

@Composable
private fun MobileHome(
    channels: List<Channel>,
    categories: List<Category>,
    filtered: List<Channel>,
    search: String,
    setSearch: (String) -> Unit,
    tab: Int,
    setTab: (Int) -> Unit,
    play: (Channel) -> Unit,
    favorites: Set<String>,
    toggleFavorite: (String) -> Unit,
    history: List<String>,
    about: () -> Unit
) {

    Scaffold(

        bottomBar = {

            NavigationBar {

                val items = listOf(
                    Icons.Default.Home to "Home",
                    Icons.Default.LiveTv to "Live",
                    Icons.Default.Favorite to "Favorites",
                    Icons.Default.Info to "About"
                )

                items.forEachIndexed { index, item ->

                    NavigationBarItem(
                        selected = tab == index,
                        onClick = {
                            setTab(index)
                        },
                        icon = {
                            Icon(
                                imageVector = item.first,
                                contentDescription = item.second
                            )
                        },
                        label = {
                            Text(item.second)
                        }
                    )
                }
            }
        }

    ) { padding ->

        when (tab) {

            0 -> {

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(
                        bottom = 30.dp
                    )
                ) {

                    item {

                        MobileHeader(
                            search = search,
                            setSearch = setSearch
                        )
                    }

                    item {

                        Hero(
                            channel = channels.firstOrNull {
                                it.featured
                            } ?: channels.firstOrNull(),
                            play = play,
                            tv = false
                        )
                    }

                    item {

                        Section(
                            title = "🔴 Live Now",
                            subtitle = "Start watching instantly"
                        ) {

                            ChannelRow(
                                list = filtered,
                                play = play,
                                favorites = favorites,
                                toggleFavorite = toggleFavorite,
                                width = 175.dp,
                                tv = false
                            )
                        }
                    }

                    item {

                        Section(
                            title = "Categories",
                            subtitle = "Browse your way"
                        ) {

                            ChipRow(
                                values = categories.map {
                                    it.name
                                }
                            )
                        }
                    }

                    item {

                        Section(
                            title = "⭐ Featured",
                            subtitle = "Popular picks"
                        ) {

                            ChannelRow(
                                list = channels.filter {
                                    it.enabled && it.featured
                                },
                                play = play,
                                favorites = favorites,
                                toggleFavorite = toggleFavorite,
                                width = 175.dp,
                                tv = false
                            )
                        }
                    }

                    item {

                        Section(
                            title = "Recently Added",
                            subtitle = "New channels"
                        ) {

                            ChannelColumn(
                                list = channels.take(6),
                                play = play,
                                favorites = favorites,
                                toggleFavorite = toggleFavorite
                            )
                        }
                    }
                }
            }

            1 -> {

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    item {

                        Text(
                            text = "Live TV",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(
                            modifier = Modifier.height(14.dp)
                        )

                        Search(
                            value = search,
                            onValueChange = setSearch
                        )

                        Spacer(
                            modifier = Modifier.height(16.dp)
                        )
                    }

                    items(
                        items = filtered,
                        key = {
                            it.id
                        }
                    ) { channel ->

                        ChannelListItem(
                            channel = channel,
                            play = play,
                            isFavorite = channel.id in favorites,
                            toggleFavorite = {
                                toggleFavorite(channel.id)
                            }
                        )
                    }

                    if (filtered.isEmpty()) {

                        item {

                            Empty(
                                title = "No channels found",
                                text = "Try another search."
                            )
                        }
                    }
                }
            }

            2 -> {

                val favoriteChannels =
                    channels.filter {
                        it.id in favorites
                    }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    item {

                        Text(
                            text = "Favorites",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(
                            modifier = Modifier.height(14.dp)
                        )
                    }

                    items(
                        items = favoriteChannels,
                        key = {
                            it.id
                        }
                    ) { channel ->

                        ChannelListItem(
                            channel = channel,
                            play = play,
                            isFavorite = true,
                            toggleFavorite = {
                                toggleFavorite(channel.id)
                            }
                        )
                    }

                    if (favoriteChannels.isEmpty()) {

                        item {

                            Empty(
                                title = "No favorites yet",
                                text = "Tap the heart on a channel to save it."
                            )
                        }
                    }
                }
            }

            else -> {

                About(about)
            }
        }
    }
}

@Composable
private fun MobileHeader(
    search: String,
    setSearch: (String) -> Unit
) {

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Logo(44.dp)

            Spacer(
                modifier = Modifier.width(12.dp)
            )

            Column {

                Text(
                    text = "Hasu Live TV",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Live entertainment",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Search(
            value = search,
            onValueChange = setSearch
        )
    }
}

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
                contentDescription = "Search"
            )
        },
        placeholder = {
            Text(
                "Search channels, countries, languages..."
            )
        }
    )
}

/* -------------------------------------------------------
   HERO
------------------------------------------------------- */

@Composable
private fun Hero(
    channel: Channel?,
    play: (Channel) -> Unit,
    tv: Boolean
) {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(
                if (tv) 300.dp else 220.dp
            )
            .padding(
                if (tv) 0.dp else 16.dp
            )
            .clip(
                RoundedCornerShape(
                    if (tv) 0.dp else 26.dp
                )
            )
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(
                            alpha = .65f
                        ),
                        Color(0xFF12131B),
                        MaterialTheme.colorScheme.secondary.copy(
                            alpha = .18f
                        )
                    )
                )
            )
    ) {

        if (channel != null) {

            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(
                        if (tv) 48.dp else 24.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                Text(
                    text = "FEATURED • LIVE",
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = channel.name,
                    style = if (tv) {
                        MaterialTheme.typography.displaySmall
                    } else {
                        MaterialTheme.typography.headlineMedium
                    },
                    fontWeight = FontWeight.ExtraBold
                )

                Text(
                    text = channel.description.ifBlank {
                        "Watch live TV with Hasu Live TV."
                    },
                    color = Color.White.copy(alpha = .72f),
                    maxLines = 2
                )

                Button(
                    onClick = {
                        play(channel)
                    }
                ) {

                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null
                    )

                    Spacer(
                        modifier = Modifier.width(6.dp)
                    )

                    Text("Watch Live")
                }
            }
        }
    }
}

/* -------------------------------------------------------
   SECTIONS
------------------------------------------------------- */

@Composable
private fun Section(
    title: String,
    subtitle: String = "",
    content: @Composable () -> Unit
) {

    Column(
        modifier = Modifier
            .padding(
                top = 22.dp,
                bottom = 4.dp
            )
    ) {

        Column(
            modifier = Modifier.padding(
                horizontal = 16.dp
            )
        ) {

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            if (subtitle.isNotBlank()) {

                Text(
                    text = subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }
        }

        content()
    }
}

/* -------------------------------------------------------
   CHANNEL ROW
------------------------------------------------------- */

@Composable
private fun ChannelRow(
    list: List<Channel>,
    play: (Channel) -> Unit,
    favorites: Set<String>,
    toggleFavorite: (String) -> Unit,
    width: Dp,
    tv: Boolean
) {

    LazyRow(
        contentPadding = PaddingValues(
            horizontal = 16.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        items(
            items = list,
            key = {
                it.id
            }
        ) { channel ->

            ChannelCard(
                channel = channel,
                play = play,
                width = width,
                tv = tv,
                isFavorite = channel.id in favorites,
                toggleFavorite = {
                    toggleFavorite(channel.id)
                }
            )
        }
    }
}

@Composable
private fun ChannelColumn(
    list: List<Channel>,
    play: (Channel) -> Unit,
    favorites: Set<String>,
    toggleFavorite: (String) -> Unit
) {

    Column(
        modifier = Modifier.padding(
            horizontal = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        list.forEach { channel ->

            ChannelListItem(
                channel = channel,
                play = play,
                isFavorite = channel.id in favorites,
                toggleFavorite = {
                    toggleFavorite(channel.id)
                }
            )
        }
    }
}

/* -------------------------------------------------------
   CHANNEL CARD
------------------------------------------------------- */

@Composable
private fun ChannelCard(
    channel: Channel,
    play: (Channel) -> Unit,
    width: Dp,
    tv: Boolean,
    isFavorite: Boolean,
    toggleFavorite: () -> Unit
) {

    Card(
        modifier = Modifier
            .width(width)
            .height(
                if (tv) 155.dp else 142.dp
            )
            .clickable {
                play(channel)
            },
        shape = RoundedCornerShape(18.dp)
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(
                    modifier = Modifier
                        .size(
                            if (tv) 54.dp else 44.dp
                        )
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {

                    Text(
                        text = channel.channelNumber.toString(),
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(
                    modifier = Modifier.width(10.dp)
                )

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = channel.name,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )

                    Text(
                        text = channel.category,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(
                modifier = Modifier.weight(1f)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {

                AssistChip(
                    onClick = toggleFavorite,
                    label = {
                        Text(
                            if (isFavorite) "♥" else "♡"
                        )
                    }
                )

                Spacer(
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "LIVE",
                    color = MaterialTheme.colorScheme.secondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/* -------------------------------------------------------
   CHANNEL LIST
------------------------------------------------------- */

@Composable
private fun ChannelListItem(
    channel: Channel,
    play: (Channel) -> Unit,
    isFavorite: Boolean,
    toggleFavorite: () -> Unit
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                play(channel)
            },
        shape = RoundedCornerShape(18.dp)
    ) {

        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {

                Text(
                    text = channel.channelNumber.toString(),
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(
                modifier = Modifier.width(12.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = channel.name,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "${channel.category} • ${channel.country} • ${channel.language}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = toggleFavorite
            ) {

                Icon(
                    imageVector =
                        if (isFavorite) {
                            Icons.Default.Favorite
                        } else {
                            Icons.Default.FavoriteBorder
                        },
                    contentDescription = "Favorite"
                )
            }
        }
    }
}

/* -------------------------------------------------------
   CHIPS
------------------------------------------------------- */

@Composable
private fun ChipRow(
    values: List<String>
) {

    LazyRow(
        contentPadding = PaddingValues(
            horizontal = 16.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        items(
            items = values.distinct()
        ) { value ->

            FilterChip(
                selected = false,
                onClick = {},
                label = {
                    Text(value)
                }
            )
        }
    }
}

/* -------------------------------------------------------
   ABOUT
------------------------------------------------------- */

@Composable
private fun About(
    back: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(
                rememberScrollState()
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        IconButton(
            onClick = back,
            modifier = Modifier.align(
                Alignment.Start
            )
        ) {

            Icon(
                Icons.Default.ArrowBack,
                contentDescription = "Back"
            )
        }

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        Logo(90.dp)

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        Text(
            text = "Hasu Live TV",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold
        )

        Text(
            text = "Premium live streaming experience",
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(
            modifier = Modifier.height(24.dp)
        )

        Card {

            Column(
                modifier = Modifier.padding(20.dp)
            ) {

                Text(
                    text = "About",
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text(
                    text = "A clean, fast Live TV application designed for Android mobile and TV. Firebase integration is prepared through the repository layer and can be connected later."
                )

                Spacer(
                    modifier = Modifier.height(16.dp)
                )

                Text(
                    text = "Version 1.0.0 • Demo Mode",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/* -------------------------------------------------------
   PLAYER
------------------------------------------------------- */

@Composable
private fun Player(
    channel: Channel,
    back: () -> Unit
) {

    val context = LocalContext.current

    val player = remember(channel.streamUrl) {

        ExoPlayer.Builder(context)
            .build()
            .apply {

                setMediaItem(
                    MediaItem.fromUri(
                        channel.streamUrl
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

    BackHandler {
        back()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {

        AndroidView(
            factory = { context ->

                PlayerView(context).apply {
                    this.player = player
                    useController = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            IconButton(
                onClick = back
            ) {

                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = channel.name,
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "${channel.category} • ${channel.language}",
                    color = Color.White.copy(alpha = .65f)
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

/* -------------------------------------------------------
   ADMIN
------------------------------------------------------- */

@Composable
private fun AdminApp(
    repo: LiveTvRepository
) {

    var loggedIn by remember {
        mutableStateOf(false)
    }

    if (!loggedIn) {

        AdminLogin {
            loggedIn = true
        }

    } else {

        AdminDashboard(repo) {
            loggedIn = false
        }
    }
}

/* -------------------------------------------------------
   ADMIN LOGIN
------------------------------------------------------- */

@Composable
private fun AdminLogin(
    done: () -> Unit
) {

    var email by remember {
        mutableStateOf("")
    }

    var password by remember {
        mutableStateOf("")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF08090D),
                        Color(0xFF17122A)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {

        Card(
            modifier = Modifier
                .widthIn(max = 430.dp)
                .padding(24.dp)
        ) {

            Column(
                modifier = Modifier.padding(26.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {

                Logo(58.dp)

                Text(
                    text = "Admin Console",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Firebase Authentication placeholder",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                    },
                    label = {
                        Text("Email")
                    },
                    singleLine = true
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                    },
                    label = {
                        Text("Password")
                    },
                    singleLine = true
                )

                Button(
                    onClick = {
                        if (
                            email.isNotBlank() &&
                            password.isNotBlank()
                        ) {
                            done()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Text(
                        "Continue in Demo Mode"
                    )
                }

                Text(
                    text = "Production login will use Firebase Authentication and Firestore security rules.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/* -------------------------------------------------------
   ADMIN DASHBOARD
------------------------------------------------------- */

@Composable
private fun AdminDashboard(
    repo: LiveTvRepository,
    logout: () -> Unit
) {

    val scope = rememberCoroutineScope()

    val channels by repo.channels.collectAsStateWithLifecycle()

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
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Logo(34.dp)

                        Spacer(
                            modifier = Modifier.width(10.dp)
                        )

                        Text("Hasu Admin")
                    }
                },

                actions = {

                    IconButton(
                        onClick = logout
                    ) {

                        Icon(
                            Icons.Default.Logout,
                            contentDescription = "Logout"
                        )
                    }
                }
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
                    contentDescription = "Add"
                )
            }
        }

    ) { padding ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            item {

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    Stat(
                        name = "Channels",
                        value = channels.size.toString()
                    )

                    Stat(
                        name = "Active",
                        value = channels.count {
                            it.enabled
                        }.toString()
                    )

                    Stat(
                        name = "Featured",
                        value = channels.count {
                            it.featured
                        }.toString()
                    )
                }
            }

            item {

                Text(
                    text = "Channel Management",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            items(
                items = channels,
                key = {
                    it.id
                }
            ) { channel ->

                Card {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {

                            Text(
                                text = channel.name,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = "#${channel.channelNumber} • ${channel.category} • ${
                                    if (channel.enabled) {
                                        "Enabled"
                                    } else {
                                        "Disabled"
                                    }
                                }",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = {
                                editing = channel
                            }
                        ) {

                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit"
                            )
                        }

                        IconButton(
                            onClick = {
                                deleting = channel
                            }
                        ) {

                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete"
                            )
                        }
                    }
                }
            }

            if (channels.isEmpty()) {

                item {

                    Empty(
                        title = "No channels",
                        text = "Add your first channel."
                    )
                }
            }
        }
    }

    /* ADD */

    if (showAdd) {

        ChannelEditor(
            existing = null,

            save = { channel ->

                scope.launch {

                    repo.upsertChannel(channel)

                    showAdd = false
                }
            },

            cancel = {
                showAdd = false
            }
        )
    }

    /* EDIT */

    editing?.let { channel ->

        ChannelEditor(
            existing = channel,

            save = { updated ->

                scope.launch {

                    repo.upsertChannel(updated)

                    editing = null
                }
            },

            cancel = {
                editing = null
            }
        )
    }

    /* DELETE */

    deleting?.let { channel ->

        AlertDialog(

            onDismissRequest = {
                deleting = null
            },

            title = {
                Text("Delete channel?")
            },

            text = {
                Text(
                    "Remove ${channel.name} from the demo catalog?"
                )
            },

            confirmButton = {

                Button(
                    onClick = {

                        scope.launch {

                            repo.deleteChannel(
                                channel.id
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

/* -------------------------------------------------------
   ADMIN STAT
------------------------------------------------------- */

@Composable
private fun RowScope.Stat(
    name: String,
    value: String
) {

    Card(
        modifier = Modifier.weight(1f)
    ) {

        Column(
            modifier = Modifier.padding(14.dp)
        ) {

            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = name,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/* -------------------------------------------------------
   CHANNEL EDITOR
------------------------------------------------------- */

@Composable
private fun ChannelEditor(
    existing: Channel?,
    save: (Channel) -> Unit,
    cancel: () -> Unit
) {

    var name by remember(existing?.id) {
        mutableStateOf(
            existing?.name ?: ""
        )
    }

    var streamUrl by remember(existing?.id) {
        mutableStateOf(
            existing?.streamUrl
                ?: "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
        )
    }

    var category by remember(existing?.id) {
        mutableStateOf(
            existing?.category ?: "News"
        )
    }

    var number by remember(existing?.id) {
        mutableStateOf(
            (existing?.channelNumber ?: 1).toString()
        )
    }

    AlertDialog(

        onDismissRequest = cancel,

        title = {

            Text(
                if (existing == null) {
                    "Add Channel"
                } else {
                    "Edit Channel"
                }
            )
        },

        text = {

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                    },
                    label = {
                        Text("Name")
                    },
                    singleLine = true
                )

                OutlinedTextField(
                    value = streamUrl,
                    onValueChange = {
                        streamUrl = it
                    },
                    label = {
                        Text("Stream URL")
                    },
                    singleLine = true
                )

                OutlinedTextField(
                    value = category,
                    onValueChange = {
                        category = it
                    },
                    label = {
                        Text("Category")
                    },
                    singleLine = true
                )

                OutlinedTextField(
                    value = number,
                    onValueChange = {
                        number = it.filter(Char::isDigit)
                    },
                    label = {
                        Text("Channel Number")
                    },
                    singleLine = true
                )
            }
        },

        confirmButton = {

            Button(
                onClick = {

                    if (
                        name.isNotBlank() &&
                        streamUrl.isNotBlank()
                    ) {

                        val channel =
                            existing?.copy(
                                name = name,
                                streamUrl = streamUrl,
                                category = category,
                                channelNumber =
                                    number.toIntOrNull()
                                        ?: existing.channelNumber
                            )
                                ?: Channel(
                                    id = UUID.randomUUID().toString(),
                                    name = name,
                                    streamUrl = streamUrl,
                                    category = category,
                                    country = "Global",
                                    language = "English",
                                    channelNumber =
                                        number.toIntOrNull()
                                            ?: 1
                                )

                        save(channel)
                    }
                }
            ) {

                Text("Save")
            }
        },

        dismissButton = {

            TextButton(
                onClick = cancel
            ) {

                Text("Cancel")
            }
        }
    )
}

/* -------------------------------------------------------
   EMPTY
------------------------------------------------------- */

@Composable
private fun Empty(
    title: String,
    text: String
) {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(50.dp),
        contentAlignment = Alignment.Center
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Icon(
                Icons.Default.LiveTv,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Text(
                text = title,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = text,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/* -------------------------------------------------------
   LOGO
------------------------------------------------------- */

@Composable
private fun Logo(
    size: Dp
) {

    Box(
        modifier = Modifier
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
        contentAlignment = Alignment.Center
    ) {

        Text(
            text = "H",
            fontSize = (size.value * .42f).sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )
    }
}

/* -------------------------------------------------------
   THEME
------------------------------------------------------- */

@Composable
private fun HasuTheme(
    content: @Composable () -> Unit
) {

    val colors = darkColorScheme(
        primary = Color(0xFF9B7BFF),
        secondary = Color(0xFF5ED7D0),
        background = Color(0xFF090A10),
        surface = Color(0xFF11131B),
        surfaceVariant = Color(0xFF1B1E28)
    )

    MaterialTheme(
        colorScheme = colors,
        typography = Typography(),
        content = content
    )
}