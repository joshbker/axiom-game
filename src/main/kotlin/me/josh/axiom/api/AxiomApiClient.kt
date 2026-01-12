package me.josh.axiom.api

import com.badlogic.gdx.Gdx
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ktx.async.AsyncExecutorDispatcher
import ktx.async.KtxAsync
import ktx.async.newSingleThreadAsyncContext
import me.josh.axiom.api.models.*
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Persistent cookie jar that stores session cookies to disk using LibGDX Preferences.
 * Cookies survive game restarts.
 */
class SimpleCookieJar : CookieJar {
    private val cookieStore = mutableMapOf<String, MutableList<Cookie>>()
    private val prefs = Gdx.app.getPreferences("axiom_session")

    init {
        loadFromDisk()
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val host = url.host
        cookieStore[host] = cookies.toMutableList()
        saveToDisk()
        Gdx.app.log("CookieJar", "Saved ${cookies.size} cookies from $host: ${cookies.map { it.name }}")
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val host = url.host
        val cookies = cookieStore[host]?.filter { !it.expiresAt.let { exp -> exp < System.currentTimeMillis() } } ?: emptyList()
        Gdx.app.log("CookieJar", "Loading ${cookies.size} cookies for $host: ${cookies.map { it.name }}")
        return cookies
    }

    fun clear() {
        cookieStore.clear()
        prefs.clear()
        prefs.flush()
        Gdx.app.log("CookieJar", "All cookies cleared")
    }

    private fun saveToDisk() {
        prefs.clear()
        for ((host, cookies) in cookieStore) {
            cookies.forEachIndexed { index, cookie ->
                val prefix = "${host}_$index"
                prefs.putString("${prefix}_name", cookie.name)
                prefs.putString("${prefix}_value", cookie.value)
                prefs.putString("${prefix}_domain", cookie.domain)
                prefs.putString("${prefix}_path", cookie.path)
                prefs.putLong("${prefix}_expiresAt", cookie.expiresAt)
                prefs.putBoolean("${prefix}_secure", cookie.secure)
                prefs.putBoolean("${prefix}_httpOnly", cookie.httpOnly)
            }
            prefs.putInteger("${host}_count", cookies.size)
        }
        prefs.flush()
        Gdx.app.log("CookieJar", "Cookies saved to disk")
    }

    private fun loadFromDisk() {
        val allKeys = prefs.get().keys
        val hosts = allKeys.filter { it.endsWith("_count") }.map { it.removeSuffix("_count") }

        for (host in hosts) {
            val count = prefs.getInteger("${host}_count", 0)
            val cookies = mutableListOf<Cookie>()

            for (i in 0 until count) {
                val prefix = "${host}_$i"
                val name = prefs.getString("${prefix}_name", "") ?: ""
                val value = prefs.getString("${prefix}_value", "") ?: ""
                val domain = prefs.getString("${prefix}_domain", "") ?: ""
                val path = prefs.getString("${prefix}_path", "/") ?: "/"
                val expiresAt = prefs.getLong("${prefix}_expiresAt", 0L)
                val secure = prefs.getBoolean("${prefix}_secure", false)
                val httpOnly = prefs.getBoolean("${prefix}_httpOnly", false)

                if (name.isNotEmpty() && expiresAt > System.currentTimeMillis()) {
                    val cookie = Cookie.Builder()
                        .name(name)
                        .value(value)
                        .domain(domain)
                        .path(path)
                        .expiresAt(expiresAt)
                        .apply {
                            if (secure) secure()
                            if (httpOnly) httpOnly()
                        }
                        .build()
                    cookies.add(cookie)
                }
            }

            if (cookies.isNotEmpty()) {
                cookieStore[host] = cookies
                Gdx.app.log("CookieJar", "Loaded ${cookies.size} cookies for $host from disk")
            }
        }
    }
}

/**
 * REST API client for communicating with the Axiom backend.
 *
 * Design Pattern: Singleton (via Kotlin object)
 * - Manages HTTP client and session cookies
 * - Provides async API methods for authentication and leaderboard
 * - All operations run on a dedicated IO thread to avoid blocking the render thread
 *
 * Handles:
 * - Player authentication (register/login)
 * - Leaderboard operations (fetch top scores, player scores)
 * - Score submission
 */
object AxiomApiClient {

    private const val API_BASE_URL = "https://axiom.joshbaker.gg"

    private val cookieJar = SimpleCookieJar()
    private val userPrefs = Gdx.app.getPreferences("axiom_user")

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .cookieJar(cookieJar)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    // Dedicated thread for API operations
    private lateinit var apiContext: AsyncExecutorDispatcher
    private val activeJobs = mutableListOf<Job>()

    /**
     * Initialize the async system. Call this in AxiomGame.create()
     */
    fun initialize() {
        apiContext = newSingleThreadAsyncContext("ApiThread")
        KtxAsync.initiate()
        Gdx.app.log("AxiomApiClient", "API client initialized")
    }

    /**
     * Shutdown and cancel all pending operations
     */
    fun shutdown() {
        activeJobs.forEach { it.cancel() }
        activeJobs.clear()
        if (::apiContext.isInitialized) {
            apiContext.dispose()
        }
        client.dispatcher.executorService.shutdown()
        client.connectionPool.evictAll()
    }

    /**
     * Clear all session cookies and user data (call on logout)
     */
    fun clearSession() {
        cookieJar.clear()
        userPrefs.clear()
        userPrefs.flush()
        Gdx.app.log("AxiomApiClient", "Session cleared")
    }

    /**
     * Save user credentials to disk
     */
    fun saveUserData(userId: String, username: String) {
        userPrefs.putString("userId", userId)
        userPrefs.putString("username", username)
        userPrefs.flush()
        Gdx.app.log("AxiomApiClient", "User data saved")
    }

    /**
     * Load stored user credentials
     */
    fun getSavedUser(): Pair<String, String>? {
        val userId = userPrefs.getString("userId", null)
        val username = userPrefs.getString("username", null)
        return if (userId != null && username != null) {
            Pair(userId, username)
        } else {
            null
        }
    }

    // ============================================
    // Async helpers
    // ============================================

    private fun launchAsync(block: suspend () -> Unit): Job {
        val job = CoroutineScope(apiContext).launch { block() }
        activeJobs.add(job)
        job.invokeOnCompletion { activeJobs.remove(job) }
        return job
    }

    private fun onMainThread(block: () -> Unit) {
        Gdx.app.postRunnable(block)
    }

    // ============================================
    // Authentication API
    // ============================================

    /**
     * Register a new player account and automatically establish a session.
     */
    fun register(
        username: String,
        password: String,
        onSuccess: (userId: String, username: String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        launchAsync {
            try {
                // Better-auth requires email, so we create a fake one
                val signUpRequest = SignUpRequest(
                    email = "${username}@axiom.local",
                    password = password,
                    name = username,
                    username = username
                )

                val requestBody = json.encodeToString(signUpRequest)
                val request = Request.Builder()
                    .url("$API_BASE_URL/api/auth/sign-up/email")
                    .post(requestBody.toRequestBody(jsonMediaType))
                    .build()

                client.newCall(request).execute().use { response ->
                    val body = response.body?.string() ?: ""

                    if (response.isSuccessful) {
                        try {
                            val authResponse = json.decodeFromString<AuthResponse>(body)
                            // Session is stored in cookies automatically

                            // Save user data to disk for auto-login
                            saveUserData(authResponse.user.id, authResponse.user.username)

                            onMainThread {
                                onSuccess(authResponse.user.id, authResponse.user.username)
                            }
                        } catch (e: Exception) {
                            Gdx.app.error("AxiomApiClient", "Failed to parse registration response", e)
                            onMainThread { onFailure("Invalid server response") }
                        }
                    } else {
                        val errorMsg = try {
                            val errorResponse = json.decodeFromString<ApiResponse<Unit>>(body)
                            errorResponse.error ?: "Registration failed"
                        } catch (e: Exception) {
                            "Registration failed: ${response.code}"
                        }
                        onMainThread { onFailure(errorMsg) }
                    }
                }
            } catch (e: Exception) {
                Gdx.app.error("AxiomApiClient", "Registration error", e)
                onMainThread { onFailure("Connection error: ${e.message}") }
            }
        }
    }

    /**
     * Authenticate a player and establish a session.
     * Clears any existing session before attempting login.
     */
    fun login(
        username: String,
        password: String,
        onSuccess: (userId: String, username: String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        launchAsync {
            try {
                // Clear old session before new login to avoid conflicts
                cookieJar.clear()

                val signInRequest = SignInRequest(username, password)
                val requestBody = json.encodeToString(signInRequest)
                val request = Request.Builder()
                    .url("$API_BASE_URL/api/auth/sign-in/username")
                    .post(requestBody.toRequestBody(jsonMediaType))
                    .build()

                client.newCall(request).execute().use { response ->
                    val body = response.body?.string() ?: ""

                    if (response.isSuccessful) {
                        try {
                            val authResponse = json.decodeFromString<AuthResponse>(body)
                            // Session is stored in cookies automatically

                            // Save user data to disk for auto-login
                            saveUserData(authResponse.user.id, authResponse.user.username)

                            onMainThread {
                                onSuccess(authResponse.user.id, authResponse.user.username)
                            }
                        } catch (e: Exception) {
                            Gdx.app.error("AxiomApiClient", "Failed to parse auth response", e)
                            onMainThread { onFailure("Invalid server response") }
                        }
                    } else {
                        val errorMsg = try {
                            val errorResponse = json.decodeFromString<ApiResponse<Unit>>(body)
                            errorResponse.error ?: "Invalid credentials"
                        } catch (e: Exception) {
                            "Login failed: ${response.code}"
                        }
                        onMainThread { onFailure(errorMsg) }
                    }
                }
            } catch (e: Exception) {
                Gdx.app.error("AxiomApiClient", "Authentication error", e)
                onMainThread { onFailure("Connection error: ${e.message}") }
            }
        }
    }

    // ============================================
    // Leaderboard API
    // ============================================

    /**
     * Submit a score to the leaderboard.
     */
    fun submitScore(
        kills: Int,
        survivalTime: Float,
        score: Int,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        launchAsync {
            try {
                val requestBody = json.encodeToString(
                    ScoreSubmission(kills, survivalTime, score)
                )

                val request = Request.Builder()
                    .url("$API_BASE_URL/api/scores")
                    .post(requestBody.toRequestBody(jsonMediaType))
                    .build()

                // Cookies are sent automatically by the CookieJar
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string() ?: ""
                    val success = response.isSuccessful

                    if (!success) {
                        Gdx.app.error("AxiomApiClient", "Score save failed: ${response.code} - $body")
                    } else {
                        Gdx.app.log("AxiomApiClient", "Score saved successfully")
                    }

                    onComplete?.let { callback ->
                        onMainThread { callback(success) }
                    }
                }
            } catch (e: Exception) {
                Gdx.app.error("AxiomApiClient", "Failed to save score", e)
                onComplete?.let { callback ->
                    onMainThread { callback(false) }
                }
            }
        }
    }

    /**
     * Get top scores from the leaderboard.
     */
    fun getTopScores(
        limit: Int = 10,
        onSuccess: (List<LeaderboardEntry>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        launchAsync {
            try {
                val request = Request.Builder()
                    .url("$API_BASE_URL/api/leaderboard?limit=$limit")
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    val body = response.body?.string() ?: ""

                    if (response.isSuccessful) {
                        try {
                            val apiResponse = json.decodeFromString<ApiResponse<List<LeaderboardEntry>>>(body)
                            val entries = apiResponse.data ?: emptyList()
                            onMainThread { onSuccess(entries) }
                        } catch (e: Exception) {
                            Gdx.app.error("AxiomApiClient", "Failed to parse leaderboard", e)
                            onMainThread { onFailure("Invalid server response") }
                        }
                    } else {
                        onMainThread { onFailure("Failed to load leaderboard") }
                    }
                }
            } catch (e: Exception) {
                Gdx.app.error("AxiomApiClient", "Failed to load leaderboard", e)
                onMainThread { onFailure("Connection error: ${e.message}") }
            }
        }
    }

    /**
     * Get a player's best scores.
     */
    fun getPlayerScores(
        playerId: String,
        limit: Int = 10,
        onSuccess: (List<LeaderboardEntry>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        launchAsync {
            try {
                val request = Request.Builder()
                    .url("$API_BASE_URL/api/player/$playerId/scores?limit=$limit")
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    val body = response.body?.string() ?: ""

                    if (response.isSuccessful) {
                        try {
                            val apiResponse = json.decodeFromString<ApiResponse<List<LeaderboardEntry>>>(body)
                            val entries = apiResponse.data ?: emptyList()
                            onMainThread { onSuccess(entries) }
                        } catch (e: Exception) {
                            Gdx.app.error("AxiomApiClient", "Failed to parse player scores", e)
                            onMainThread { onFailure("Invalid server response") }
                        }
                    } else {
                        onMainThread { onFailure("Failed to load player scores") }
                    }
                }
            } catch (e: Exception) {
                Gdx.app.error("AxiomApiClient", "Failed to load player scores", e)
                onMainThread { onFailure("Connection error: ${e.message}") }
            }
        }
    }
}
