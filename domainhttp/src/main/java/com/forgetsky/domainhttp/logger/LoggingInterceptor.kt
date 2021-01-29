package com.forgetsky.domainhttp.logger

import com.forgetsky.domainhttp.utils.isPost
import com.forgetsky.domainhttp.utils.isProbablyUtf8
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.internal.http.promisesBody
import okio.Buffer
import okio.GzipSource
import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets.UTF_8
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * An OkHttp interceptor which logs request and response information. Can be applied as an
 * [application interceptor][OkHttpClient.interceptors] or as a [OkHttpClient.networkInterceptors].
 *
 * The format of the logs created by this class should not be considered stable and may
 * change slightly between releases. If you need a stable logging format, use your own interceptor.
 */
class LoggingInterceptor @JvmOverloads constructor(
    private val logger: L.Logger = L.Logger.DEFAULT,
    private var decryptAdapter: DecryptAdapter? = null
) : Interceptor {

    init {
        L.setLogger(logger)
    }

    @Volatile
    private var headersToRedact = emptySet<String>()

    @set:JvmName("level")
    @Volatile
    var level = Level.NONE

    enum class Level {
        /** No logs. */
        NONE,

        /**
         * Logs request and response lines.
         *
         * Example:
         * ```
         * --> POST /greeting http/1.1 (3-byte body)
         *
         * <-- 200 OK (22ms, 6-byte body)
         * ```
         */
        BASIC,

        /**
         * Logs request and response lines and their respective headers.
         *
         * Example:
         * ```
         * --> POST /greeting http/1.1
         * Host: example.com
         * Content-Type: plain/text
         * Content-Length: 3
         * --> END POST
         *
         * <-- 200 OK (22ms)
         * Content-Type: plain/text
         * Content-Length: 6
         * <-- END HTTP
         * ```
         */
        HEADERS,

        /**
         * Logs request and response lines and their respective headers and bodies (if present).
         *
         * Example:
         * ```
         * --> POST /greeting http/1.1
         * Host: example.com
         * Content-Type: plain/text
         * Content-Length: 3
         *
         * Hi?
         * --> END POST
         *
         * <-- 200 OK (22ms)
         * Content-Type: plain/text
         * Content-Length: 6
         *
         * Hello!
         * <-- END HTTP
         * ```
         */
        BODY,

        /**
         * Logs request and response lines and their respective headers and bodies when error occurs.
         */
        ERROR
    }

    interface DecryptAdapter {
        fun decrypt(chain: Interceptor.Chain, inputStream: InputStream): String
    }

    fun redactHeader(name: String) {
        val newHeadersToRedact = TreeSet(String.CASE_INSENSITIVE_ORDER)
        newHeadersToRedact += headersToRedact
        newHeadersToRedact += name
        headersToRedact = newHeadersToRedact
    }

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val level = this.level

        val request = chain.request()
        if (level == Level.NONE) {
            return chain.proceed(request)
        }

        val logError = level == Level.ERROR
        val logBody = logError || level == Level.BODY
        val logHeaders = logBody || level == Level.HEADERS
        var errorBuffer: StringBuilder? = null
        if (logError) {
            errorBuffer = StringBuilder()
        }

        val requestBody = request.body

        val connection = chain.connection()
        var requestStartMessage =
            ("--> ${request.method} ${request.url}${if (connection != null) " " + connection.protocol() else ""}")
        if (!logHeaders && requestBody != null) {
            requestStartMessage += " (${requestBody.contentLength()}-byte body)"
        }
        logOrCache(errorBuffer, requestStartMessage)

        if (logHeaders) {
            val headers = request.headers

            if (requestBody != null) {
                // Request body headers are only present when installed as a network interceptor. When not
                // already present, force them to be included (if available) so their values are known.
                requestBody.contentType()?.let {
                    if (headers["Content-Type"] == null) {
                        logOrCache(errorBuffer, "Content-Type: $it")
                    }
                }
                if (requestBody.contentLength() != -1L) {
                    if (headers["Content-Length"] == null) {
                        logOrCache(errorBuffer, "Content-Length: ${requestBody.contentLength()}")
                    }
                }
            }

            for (i in 0 until headers.size) {
                logHeader(errorBuffer, headers, i)
            }

            if (!logBody || requestBody == null) {
                logOrCache(errorBuffer, "--> END ${request.method}")
            } else if (bodyHasUnknownEncoding(request.headers)) {
                logOrCache(errorBuffer, "--> END ${request.method} (encoded body omitted)")
            } else if (requestBody.isDuplex()) {
                logOrCache(errorBuffer, "--> END ${request.method} (duplex request body omitted)")
            } else if (requestBody.isOneShot()) {
                logOrCache(errorBuffer, "--> END ${request.method} (one-shot body omitted)")
            } else {
                val buffer = Buffer()
                requestBody.writeTo(buffer)

                val contentType = requestBody.contentType()
                if (decryptAdapter != null && contentType != null
                    && "application" == contentType.type && "json" == contentType.subtype && UTF_8 == contentType.charset()
                ) {
                    val msg = decryptAdapter?.decrypt(chain, buffer.inputStream()) ?: ""
                    logOrCache(errorBuffer, "requestBody: $msg")
                    logOrCache(
                        errorBuffer,
                        "--> END ${request.method} (${requestBody.contentLength()}-byte body)"
                    )
                } else {
                    val charset: Charset = contentType?.charset(UTF_8) ?: UTF_8
                    if (buffer.isProbablyUtf8()) {
                        logOrCache(errorBuffer, "requestBody: ${buffer.readString(charset)}")
                        logOrCache(
                            errorBuffer,
                            "--> END ${request.method} (${requestBody.contentLength()}-byte body)"
                        )
                    } else {
                        logOrCache(
                            errorBuffer,
                            "--> END ${request.method} (binary ${requestBody.contentLength()}-byte body omitted)"
                        )
                    }
                }
            }
        }

        val startNs = System.nanoTime()
        val response: Response
        try {
            response = chain.proceed(request)
        } catch (e: Exception) {
            logAllError(errorBuffer, "<-- HTTP FAILED: $e")
            throw e
        }

        val tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs)

        val responseBody = response.body!!
        val contentLength = responseBody.contentLength()
        val bodySize = if (contentLength != -1L) "$contentLength-byte" else "unknown-length"
        val res =
            "<-- ${response.code}${if (response.message.isEmpty()) "" else ' ' + response.message} ${response.request.url} (${tookMs}ms${if (!logHeaders) ", $bodySize body" else ""})"
        if (logError) {
            if (!response.isSuccessful) {
                logAllError(errorBuffer, res)
            } else {
                errorBuffer?.clear()
                errorBuffer = null
                return response
            }
        } else {
            logger.log(res)
        }

        if (logHeaders) {
            val headers = response.headers
            for (i in 0 until headers.size) {
                logHeader(null, headers, i)
            }

            if (!logBody || !response.promisesBody()) {
                logger.log("<-- END HTTP")
            } else if (bodyHasUnknownEncoding(response.headers)) {
                logger.log("<-- END HTTP (encoded body omitted)")
            } else {
                val source = responseBody.source()
                source.request(Long.MAX_VALUE) // Buffer the entire body.
                var buffer = source.buffer

                var gzippedLength: Long? = null
                if ("gzip".equals(headers["Content-Encoding"], ignoreCase = true)) {
                    gzippedLength = buffer.size
                    GzipSource(buffer.clone()).use { gzippedResponseBody ->
                        buffer = Buffer()
                        buffer.writeAll(gzippedResponseBody)
                    }
                }

                val contentType = responseBody.contentType()
                if (response.request.isPost() && decryptAdapter != null && contentType != null
                    && "application" == contentType.type && "json" == contentType.subtype && UTF_8 == contentType.charset()
                ) {
                    val msg = decryptAdapter?.decrypt(chain, buffer.clone().inputStream()) ?: ""
                    logger.log("responseBody: $msg")

                } else {
                    val charset: Charset = contentType?.charset(UTF_8) ?: UTF_8
                    if (!buffer.isProbablyUtf8()) {
                        logger.log("")
                        logger.log("<-- END HTTP (binary ${buffer.size}-byte body omitted)")
                        return response
                    }

                    if (contentLength != 0L) {
                        logger.log("")
                        logger.log("responseBody: ${buffer.clone().readString(charset)}")
                    }
                }

                if (gzippedLength != null) {
                    logger.log("<-- END HTTP (${buffer.size}-byte, $gzippedLength-gzipped-byte body)")
                } else {
                    logger.log("<-- END HTTP (${buffer.size}-byte body)")
                }
            }
        }

        return response
    }

    private fun logAllError(errorBuffer: StringBuilder?, msg: String) {
        val errorMsg = errorBuffer?.append("$msg\n")?.toString() ?: msg
        errorBuffer?.clear()
        logger.log(errorMsg)
    }

    private fun logOrCache(errorBuffer: StringBuilder?, msg: String) {
        errorBuffer?.append("$msg\n") ?: logger.log(msg)
    }

    private fun logHeader(errorBuffer: StringBuilder?, headers: Headers, i: Int) {
        val value = if (headers.name(i) in headersToRedact) "██" else headers.value(i)
        logOrCache(errorBuffer, headers.name(i) + ": " + value)
    }

    private fun bodyHasUnknownEncoding(headers: Headers): Boolean {
        val contentEncoding = headers["Content-Encoding"] ?: return false
        return !contentEncoding.equals("identity", ignoreCase = true) &&
                !contentEncoding.equals("gzip", ignoreCase = true)
    }

}
