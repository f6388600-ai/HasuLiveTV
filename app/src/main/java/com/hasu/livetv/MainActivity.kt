@file:OptIn(ExperimentalMaterial3Api::class)

package com.hasu.livetv

import com.hasu.livetv.auth.LoginScreen
import com.hasu.livetv.auth.SignupScreen
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.google.firebase.auth.FirebaseAuth
import com.hasu.livetv.auth.AuthViewModel
import com.hasu.livetv.data.LiveTvRepository
import com.hasu.livetv.data.UserRepository
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
private fun App(
    repo: LiveTvRepository
) {
    var splash by remember {
        mutableStateOf(true)
    }

    LaunchedEffect(Unit) {
        delay(900)
        splash = false
    }

    if (splash) {
        Splash()
        return
    }

    /*
     * Admin gets its own authenticated flow.
     */
    if (BuildConfig.EDITION == "admin") {
        AdminApp(repo)
        return
    }

    /*
     * Mobile + TV use normal user authentication.
     */
    UserApp(repo)
}

/* =========================================================
   USER APP AUTH GATE
   ========================================================= */

@Composable
private fun UserApp(
    repo: LiveTvRepository,
    authViewModel: AuthViewModel = viewModel()
) {
    val authState by authViewModel.uiState
        .collectAsStateWithLifecycle()

    var authPage by remember {
        mutableStateOf("login")
    }

    if (!authState.isLoggedIn) {

        AnimatedContent(
            targetState = authPage,
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            label = "auth"
        ) { page ->

            when (page) {

                "signup" -> {
                    SignupScreen(
                        viewModel = authViewModel,
                        onLogin = {
                            authPage = "login"
                        },
                        onSuccess = {
                            authPage = "login"
                        }
                    )
                }

                else -> {
                    LoginScreen(
                        viewModel = authViewModel,
                        onSignup = {
                            authPage = "signup"
                        },
                        onSuccess = {
                            authPage = "login"
                        }
                    )
                }
            }
        }

        return
    }

    /*
     * Existing application starts here after Firebase login.
     */
    MainUserApp(
        repo = repo,
        authViewModel = authViewModel
    )
}

/* =========================================================
   EXISTING USER APP
   ========================================================= */

@Composable
private fun MainUserApp(
    repo: LiveTvRepository,
    authViewModel: AuthViewModel
) {
    var page by remember {
        mutableStateOf<Page>(Page.Home)
    }

    var selected by remember {
        mutableStateOf<Channel?>(null)
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
                    },

                    logout = {
                        authViewModel.logout()
                    }
                )
            }

            Page.About -> {

                About(
                    back = {
                        page = Page.Home
                    },
                    logout = {
                        authViewModel.logout()
                    }
                )
            }

            Page.Player -> {

                selected?.let {

                    Player(it) {

                        selected = null
                        page = Page.Home
                    }
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
    about: () -> Unit,
    logout: () -> Unit
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
                favorites =
                    if (id in favorites) {
                        favorites - id
                    } else {
                        favorites + id
                    }
            },
            logout = logout
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
    toggle: (String) -> Unit,
    logout: () -> Unit
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

            TvTopBar(
                search,
                setSearch,
                logout
            )
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
    setSearch: (String) -> Unit,
    logout: () -> Unit
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

        Spacer(
            Modifier.width(12.dp)
        )

        IconButton(
            onClick = logout
        ) {
            Icon(
                Icons.Default.Logout,
                "Logout"
            )
        }
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

                About(
                    back = about,
                    logout = {
                        FirebaseAuth
                            .getInstance()
                            .signOut()

                        /*
                         * App root observes Firebase state
                         * through AuthViewModel.
                         *
                         * We trigger activity recreation so
                         * auth state is shown immediately.
                         */
                        about()
                    }
                )
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
            .height(
                if (tv) 300.dp
                else 220.dp
            )
            .padding(
                if (tv) 0.dp
                else 16.dp
            )
            .clip(
                RoundedCornerShape(
                    if (tv) 0.dp
                    else 26.dp
                )
            )
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme
                            .primary
                            .copy(.65f),
                        Color(0xFF12131B),
                        MaterialTheme.colorScheme
                            .secondary
                            .copy(.18f)
                    )
                )
            )
    ) {

        if (channel != null) {

            Column(
                Modifier
                    .align(
                        Alignment.CenterStart
                    )
                    .padding(
                        if (tv) 48.dp
                        else 24.dp
                    ),
                verticalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {

                Text(
                    "FEATURED • LIVE",
                    color =
                        MaterialTheme.colorScheme
                            .secondary,
                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    channel.name,
                    style =
                        if (tv)
                            MaterialTheme.typography
                                .displaySmall
                        else
                            MaterialTheme.typography
                                .headlineMedium,
                    fontWeight =
                        FontWeight.ExtraBold
                )

                Text(
                    channel.description.ifBlank {
                        "Watch live TV with Hasu Live TV."
                    },
                    color =
                        Color.White.copy(.72f),
                    maxLines = 2
                )

                Button(
                    onClick = {
                        play(channel)
                    }
                ) {

                    Icon(
                        Icons.Default.PlayArrow,
                        null
                    )

                    Spacer(
                        Modifier.width(6.dp)
                    )

                    Text("Watch Live")
                }
            }
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
            .height(
                if (tv) 155.dp
                else 142.dp
            )
            .clickable {
                play(c)
            },
        shape =
            RoundedCornerShape(18.dp)
    ) {

        Column(
            Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Box(
                    Modifier
                        .size(
                            if (tv) 54.dp
                            else 44.dp
                        )
                        .background(
                            MaterialTheme.colorScheme
                                .surfaceVariant,
                            RoundedCornerShape(12.dp)
                        ),
                    Alignment.Center
                ) {

                    Text(
                        c.channelNumber.toString(),
                        fontWeight =
                            FontWeight.Bold
                    )
                }

                Spacer(
                    Modifier.width(10.dp)
                )

                Column(
                    Modifier.weight(1f)
                ) {

                    Text(
                        c.name,
                        fontWeight =
                            FontWeight.Bold,
                        maxLines = 1
                    )

                    Text(
                        c.category,
                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(
                Modifier.weight(1f)
            )

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                AssistChip(
                    onClick = toggle,
                    label = {
                        Text(
                            if (isFav)
                                "♥"
                            else
                                "♡"
                        )
                    }
                )

                Spacer(
                    Modifier.weight(1f)
                )

                Text(
                    "LIVE",
                    color =
                        MaterialTheme.colorScheme
                            .secondary,
                    fontSize = 11.sp,
                    fontWeight =
                        FontWeight.Bold
                )
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
        Modifier
            .fillMaxWidth()
            .clickable {
                play(c)
            },
        shape =
            RoundedCornerShape(18.dp)
    ) {

        Row(
            Modifier.padding(14.dp),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Box(
                Modifier
                    .size(50.dp)
                    .background(
                        MaterialTheme.colorScheme
                            .surfaceVariant,
                        RoundedCornerShape(12.dp)
                    ),
                Alignment.Center
            ) {

                Text(
                    c.channelNumber.toString(),
                    fontWeight =
                        FontWeight.Bold
                )
            }

            Spacer(
                Modifier.width(12.dp)
            )

            Column(
                Modifier.weight(1f)
            ) {

                Text(
                    c.name,
                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    "${c.category} • ${c.country} • ${c.language}",
                    fontSize = 12.sp,
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                )
            }

            IconButton(
                onClick = toggle
            ) {

                Icon(
                    if (isFav)
                        Icons.Default.Favorite
                    else
                        Icons.Default.FavoriteBorder,
                    "Favorite"
                )
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
    back: () -> Unit,
    logout: () -> Unit
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

            Spacer(
                Modifier.weight(1f)
            )

            IconButton(
                onClick = logout
            ) {

                Icon(
                    Icons.Default.Logout,
                    "Logout"
                )
            }
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
                    "Version 1.0.0 • Firebase Connected",
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
   ADMIN APP
   ========================================================= */

@Composable
private fun AdminApp(
    repo: LiveTvRepository,
    authViewModel: AuthViewModel = viewModel()
) {

    val state by authViewModel.uiState
        .collectAsStateWithLifecycle()

    if (!state.isLoggedIn) {

        AdminLogin(
            viewModel = authViewModel
        )

        return
    }

    AdminRoleGate(
        repo = repo,
        authViewModel = authViewModel
    )
}

/* =========================================================
   ADMIN ROLE CHECK
   ========================================================= */

@Composable
private fun AdminRoleGate(
    repo: LiveTvRepository,
    authViewModel: AuthViewModel
) {

    val context =
        LocalContext.current

    var checking by remember {
        mutableStateOf(true)
    }

    var isAdmin by remember {
        mutableStateOf(false)
    }

    var error by remember {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(Unit) {

        try {

            val role =
                UserRepository()
                    .getUserRole()

            isAdmin =
                role.equals(
                    "admin",
                    ignoreCase = true
                )

        } catch (e: Exception) {

            error =
                e.message
                    ?: "Unable to verify admin role."
        }

        checking = false
    }

    if (checking) {

        Box(
            Modifier.fillMaxSize(),
            Alignment.Center
        ) {

            CircularProgressIndicator()
        }

        return
    }

    if (!isAdmin) {

        Box(
            Modifier.fillMaxSize(),
            Alignment.Center
        ) {

            Card(
                Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {

                Column(
                    Modifier.padding(24.dp),
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    Icon(
                        Icons.Default.Lock,
                        null,
                        Modifier.size(48.dp),
                        tint =
                            MaterialTheme.colorScheme
                                .error
                    )

                    Spacer(
                        Modifier.height(14.dp)
                    )

                    Text(
                        "Admin Access Required",
                        style =
                            MaterialTheme.typography
                                .headlineSmall,
                        fontWeight =
                            FontWeight.Bold
                    )

                    Spacer(
                        Modifier.height(8.dp)
                    )

                    Text(
                        error
                            ?: "This account does not have admin permission."
                    )

                    Spacer(
                        Modifier.height(18.dp)
                    )

                    Button(
                        onClick = {
                            authViewModel.logout()
                        }
                    ) {

                        Text("Logout")
                    }
                }
            }
        }

        return
    }

    AdminDashboard(
        repo = repo,
        logout = {
            authViewModel.logout()
        }
    )
}

/* =========================================================
   ADMIN LOGIN
   ========================================================= */

@Composable
private fun AdminLogin(
    viewModel: AuthViewModel
) {

    val state by viewModel.uiState
        .collectAsStateWithLifecycle()

    var email by remember {
        mutableStateOf("")
    }

    var password by remember {
        mutableStateOf("")
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF08090D),
                        Color(0xFF17122A)
                    )
                )
            ),
        Alignment.Center
    ) {

        Card(
            Modifier
                .widthIn(
                    max = 430.dp
                )
                .padding(24.dp)
        ) {

            Column(
                Modifier.padding(26.dp),
                verticalArrangement =
                    Arrangement.spacedBy(14.dp)
            ) {

                Logo(58.dp)

                Text(
                    "Admin Console",
                    style =
                        MaterialTheme.typography
                            .headlineSmall,
                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    "Sign in with Firebase Authentication",
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                    },
                    label = {
                        Text("Email")
                    },
                    singleLine = true,
                    modifier =
                        Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                    },
                    label = {
                        Text("Password")
                    },
                    singleLine = true,
                    modifier =
                        Modifier.fillMaxWidth()
                )

                state.error?.let {

                    Text(
                        it,
                        color =
                            MaterialTheme.colorScheme
                                .error,
                        fontSize = 12.sp
                    )
                }

                Button(
                    onClick = {

                        viewModel.login(
                            email,
                            password
                        )
                    },
                    enabled =
                        !state.isLoading,
                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    if (state.isLoading) {

                        CircularProgressIndicator(
                            Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )

                    } else {

                        Text("Sign In")
                    }
                }

                Text(
                    "Only users with role = admin can access this console.",
                    fontSize = 12.sp,
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                )
            }
        }
    }
}

/* =========================================================
   ADMIN DASHBOARD
   ========================================================= */

@Composable
private fun AdminDashboard(
    repo: LiveTvRepository,
    logout: () -> Unit
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

                        Text("Hasu Admin")
                    }
                },

                actions = {

                    IconButton(
                        onClick = logout
                    ) {

                        Icon(
                            Icons.Default.Logout,
                            "Logout"
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

    var name by remember(
        existing?.id
    ) {
        mutableStateOf(
            existing?.name ?: ""
        )
    }

    var url by remember(
        existing?.id
    ) {

        mutableStateOf(
            existing?.streamUrl
                ?: "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"
        )
    }

    var category by remember(
        existing?.id
    ) {

        mutableStateOf(
            existing?.category ?: "News"
        )
    }

    var number by remember(
        existing?.id
    ) {

        mutableStateOf(
            (
                    existing?.channelNumber
                        ?: 1
                    ).toString()
        )
    }

    AlertDialog(

        onDismissRequest = cancel,

        title = {

            Text(
                if (existing == null)
                    "Add Channel"
                else
                    "Edit Channel"
            )
        },

        text = {

            Column(
                verticalArrangement =
                    Arrangement.spacedBy(8.dp)
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
                    value = url,
                    onValueChange = {
                        url = it
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
                        number =
                            it.filter(
                                Char::isDigit
                            )
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
                        url.isNotBlank()
                    ) {

                        val channel =

                            if (existing != null) {

                                existing.copy(
                                    name = name,
                                    streamUrl = url,
                                    category = category,
                                    channelNumber =
                                        number.toIntOrNull()
                                            ?: existing.channelNumber
                                )

                            } else {

                                Channel(
                                    UUID.randomUUID()
                                        .toString(),
                                    name,
                                    url,
                                    category,
                                    "Global",
                                    "English",
                                    number.toIntOrNull()
                                        ?: 1
                                )
                            }

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