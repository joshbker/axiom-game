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
 * Simple in-memory cookie jar for storing session cookies
 */
class SimpleCookieJar : CookieJar {
    private val cookieStore = mutableMapOf<String, MutableList<Cookie>>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val host = url.host
        cookieStore[host] = cookies.toMutableList()
        Gdx.app.log("CookieJar", "Saved ${cookies.size} cookies from $host: ${cookies.map { it.name }}")
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val host = url.host
        val cookies = cookieStore[host]?.filter { !it.expiresAt.let { exp -> exp < System.currentTimeMillis() } } ?: emptyList()
        Gdx.app.log("CookieJar", "Loading ${cookies.size} cookies for $host: ${cookies.map { it.name }}")
        return cookies
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

    private const val API_BASE_URL = "http://localhost:5173"

    private val cookieJar = SimpleCookieJar()

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
     * Register a new player account.
     */
    fun register(
        username: String,
        password: String,
        onSuccess: () -> Unit,
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
                        onMainThread { onSuccess() }
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
     */
    fun login(
        username: String,
        password: String,
        onSuccess: (userId: String, username: String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        launchAsync {
            try {
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
