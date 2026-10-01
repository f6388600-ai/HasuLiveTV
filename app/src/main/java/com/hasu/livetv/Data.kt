package com.hasu.livetv.data

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.hasu.livetv.model.Category
import com.hasu.livetv.model.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

interface LiveTvRepository {
    val channels: StateFlow<List<Channel>>
    val categories: StateFlow<List<Category>>
    val syncError: StateFlow<String?>
    suspend fun upsertChannel(channel: Channel)
    suspend fun deleteChannel(id: String)
}

/**
 * Shared catalog repository.
 *
 * Local SharedPreferences is used as a cache so the app still opens offline.
 * Firestore is the shared source of truth, so changes made from the Admin
 * edition are pushed to every Mobile/TV installation in real time.
 */
class LocalLiveTvRepository(
    context: Context,
    private val seedCloudIfEmpty: Boolean = false
) : LiveTvRepository {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(
        "hasu_live_tv_catalog",
        Context.MODE_PRIVATE
    )
    private val firestore = FirebaseFirestore.getInstance()
    private val channelsCollection = firestore.collection("channels")

    private val _channels = MutableStateFlow(loadChannels())
    override val channels: StateFlow<List<Channel>> = _channels

    private val _categories = MutableStateFlow(buildCategories(_channels.value))
    override val categories: StateFlow<List<Category>> = _categories

    private val _syncError = MutableStateFlow<String?>(null)
    override val syncError: StateFlow<String?> = _syncError

    private var listener: ListenerRegistration? = null

    init {
        startCloudListener()
    }

    private fun startCloudListener() {
        listener = channelsCollection
            .orderBy("sortOrder")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _syncError.value = error.message ?: "Cloud catalog unavailable"
                    return@addSnapshotListener
                }

                _syncError.value = null
                val documents = snapshot?.documents.orEmpty()

                if (documents.isEmpty()) {
                    if (seedCloudIfEmpty) {
                        seedDemoCatalog()
                    }
                    return@addSnapshotListener
                }

                val remote = documents.mapNotNull { document ->
                    document.toChannel()
                }

                if (remote.isNotEmpty()) {
                    applyChannels(remote)
                }
            }
    }

    private fun seedDemoCatalog() {
        val batch = firestore.batch()
        demoChannels().forEach { channel ->
            batch.set(
                channelsCollection.document(channel.id),
                channel.toMap()
            )
        }
        batch.commit()
            .addOnFailureListener { e ->
                _syncError.value = e.message ?: "Could not publish starter catalog"
            }
    }

    override suspend fun upsertChannel(channel: Channel) {
        channelsCollection
            .document(channel.id)
            .set(channel.toMap())
            .await()

        // Immediate local update; the snapshot listener will reconcile it.
        applyChannels(
            (_channels.value.filterNot { it.id == channel.id } + channel)
                .sortedWith(channelComparator)
        )
    }

    override suspend fun deleteChannel(id: String) {
        channelsCollection
            .document(id)
            .delete()
            .await()

        applyChannels(_channels.value.filterNot { it.id == id })
    }

    private fun applyChannels(items: List<Channel>) {
        val sorted = items.sortedWith(channelComparator)
        _channels.value = sorted
        _categories.value = buildCategories(sorted)
        saveChannels(sorted)
    }

    private fun loadChannels(): List<Channel> {
        val raw = prefs.getString(KEY_CHANNELS, null)
        if (raw.isNullOrBlank()) return demoChannels()

        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    add(
                        Channel(
                            id = o.optString("id", UUID.randomUUID().toString()),
                            name = o.optString("name"),
                            streamUrl = o.optString("streamUrl"),
                            category = o.optString("category", "General"),
                            country = o.optString("country", "Global"),
                            language = o.optString("language", "English"),
                            channelNumber = o.optInt("channelNumber", i + 1),
                            enabled = o.optBoolean("enabled", true),
                            featured = o.optBoolean("featured", false),
                            popular = o.optBoolean("popular", false),
                            description = o.optString("description"),
                            logoUrl = o.optString("logoUrl"),
                            bannerUrl = o.optString("bannerUrl"),
                            sortOrder = o.optInt("sortOrder", i)
                        )
                    )
                }
            }.ifEmpty { demoChannels() }
        }.getOrElse { demoChannels() }
    }

    private fun saveChannels(items: List<Channel>) {
        val array = JSONArray()
        items.forEach { c ->
            array.put(JSONObject().apply {
                put("id", c.id)
                put("name", c.name)
                put("streamUrl", c.streamUrl)
                put("category", c.category)
                put("country", c.country)
                put("language", c.language)
                put("channelNumber", c.channelNumber)
                put("enabled", c.enabled)
                put("featured", c.featured)
                put("popular", c.popular)
                put("description", c.description)
                put("logoUrl", c.logoUrl)
                put("bannerUrl", c.bannerUrl)
                put("sortOrder", c.sortOrder)
            })
        }
        prefs.edit().putString(KEY_CHANNELS, array.toString()).apply()
    }

    private fun buildCategories(items: List<Channel>): List<Category> =
        items.filter { it.enabled && it.category.isNotBlank() }
            .map { it.category.trim() }
            .distinctBy { it.lowercase() }
            .mapIndexed { index, name ->
                Category(
                    name.lowercase().replace(" ", "_"),
                    name,
                    order = index
                )
            }


    private fun com.google.firebase.firestore.DocumentSnapshot.toChannel(): Channel? {
        return runCatching {
            Channel(
                id = id,
                name = getString("name").orEmpty(),
                streamUrl = getString("streamUrl").orEmpty(),
                category = getString("category") ?: "General",
                country = getString("country") ?: "Global",
                language = getString("language") ?: "English",
                channelNumber = (getLong("channelNumber") ?: 0L).toInt(),
                enabled = getBoolean("enabled") ?: true,
                featured = getBoolean("featured") ?: false,
                popular = getBoolean("popular") ?: false,
                description = getString("description").orEmpty(),
                logoUrl = getString("logoUrl").orEmpty(),
                bannerUrl = getString("bannerUrl").orEmpty(),
                sortOrder = (getLong("sortOrder") ?: 0L).toInt()
            ).takeIf { it.name.isNotBlank() && it.streamUrl.isNotBlank() }
        }.getOrNull()
    }

    private fun Channel.toMap(): Map<String, Any> = mapOf(
        "name" to name,
        "streamUrl" to streamUrl,
        "category" to category,
        "country" to country,
        "language" to language,
        "channelNumber" to channelNumber,
        "enabled" to enabled,
        "featured" to featured,
        "popular" to popular,
        "description" to description,
        "logoUrl" to logoUrl,
        "bannerUrl" to bannerUrl,
        "sortOrder" to sortOrder
    )

    companion object {
        private const val KEY_CHANNELS = "channels"

        private val channelComparator = compareBy<Channel> {
            it.sortOrder
        }.thenBy {
            it.channelNumber
        }.thenBy {
            it.name.lowercase()
        }

        private fun demoChannels() = listOf(
            Channel("news", "Hasu News", "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8", "News", "Global", "English", 1, featured = true, popular = true, description = "Live news and current affairs.", sortOrder = 1),
            Channel("sports", "Hasu Sports", "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8", "Sports", "Global", "English", 2, featured = true, popular = true, description = "Sports entertainment channel.", sortOrder = 2),
            Channel("music", "Hasu Music", "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8", "Music", "Global", "English", 3, popular = true, description = "Music and entertainment.", sortOrder = 3),
            Channel("kids", "Hasu Kids", "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8", "Kids", "Global", "English", 4, description = "Family friendly entertainment.", sortOrder = 4),
            Channel("movies", "Hasu Movies", "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8", "Movies", "Global", "English", 5, description = "Movies and entertainment.", sortOrder = 5)
        )
    }
}
