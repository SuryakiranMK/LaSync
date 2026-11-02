package com.lasync.audio.core.jukebox

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.lasync.audio.core.audio.AudioConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * AudioFileDecoder:
 * Decodes local MP3, WAV, FLAC, AAC, and M4A audio files from Android storage
 * into raw 16-bit stereo PCM byte buffers for RAMJukebox offline playback.
 */
object AudioFileDecoder {
    private const val TAG = "AudioFileDecoder"

    suspend fun decodeTrack(
        context: Context,
        uri: Uri,
        onProgress: (Float) -> Unit = {}
    ): JukeboxTrack? = withContext(Dispatchers.IO) {
        try {
            // 1. Extract Metadata
            var title = "Local Audio Track"
            var artist = "Unknown Artist"
            var durationMs = 0L

            try {
                val mmr = MediaMetadataRetriever()
                mmr.setDataSource(context, uri)
                val extractedTitle = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                val extractedArtist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                val extractedDuration = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)

                if (!extractedTitle.isNullOrBlank()) title = extractedTitle
                else title = getFileName(context, uri)

                if (!extractedArtist.isNullOrBlank()) artist = extractedArtist
                if (!extractedDuration.isNullOrBlank()) durationMs = extractedDuration.toLongOrNull() ?: 0L
                mmr.release()
            } catch (e: Exception) {
                Log.w(TAG, "Error extracting metadata: ${e.message}")
                title = getFileName(context, uri)
            }

            // 2. Setup Extractor & Codec
            val extractor = MediaExtractor()
            extractor.setDataSource(context, uri, null)

            var audioTrackIndex = -1
            var format: MediaFormat? = null

            for (i in 0 until extractor.trackCount) {
                val f = extractor.getTrackFormat(i)
                val mime = f.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    format = f
                    break
                }
            }

            if (audioTrackIndex < 0 || format == null) {
                Log.e(TAG, "No audio track found in media file: $uri")
                extractor.release()
                return@withContext null
            }

            extractor.selectTrack(audioTrackIndex)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
            val codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            val sampleRate = if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            } else 44100

            val channelCount = if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            } else 2

            val pcmOut = ByteArrayOutputStream(4 * 1024 * 1024)
            val bufferInfo = MediaCodec.BufferInfo()
            var isInputEOS = false
            var isOutputEOS = false
            val timeoutUs = 5000L
            val totalUs = if (durationMs > 0) durationMs * 1000L else 180_000_000L

            while (!isOutputEOS) {
                if (!isInputEOS) {
                    val inputIndex = codec.dequeueInputBuffer(timeoutUs)
                    if (inputIndex >= 0) {
                        val inputBuffer = codec.getInputBuffer(inputIndex)
                        if (inputBuffer != null) {
                            val sampleSize = extractor.readSampleData(inputBuffer, 0)
                            if (sampleSize < 0) {
                                codec.queueInputBuffer(
                                    inputIndex,
                                    0,
                                    0,
                                    0L,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                )
                                isInputEOS = true
                            } else {
                                val sampleTime = extractor.sampleTime
                                codec.queueInputBuffer(inputIndex, 0, sampleSize, sampleTime, 0)
                                extractor.advance()
                                if (totalUs > 0) {
                                    val progress = (sampleTime.toFloat() / totalUs.toFloat()).coerceIn(0f, 1f)
                                    onProgress(progress * 0.9f) // 0% to 90% decoding progress
                                }
                            }
                        }
                    }
                }

                val outputIndex = codec.dequeueOutputBuffer(bufferInfo, timeoutUs)
                if (outputIndex >= 0) {
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        isOutputEOS = true
                    }
                    val outputBuffer = codec.getOutputBuffer(outputIndex)
                    if (outputBuffer != null && bufferInfo.size > 0) {
                        outputBuffer.position(bufferInfo.offset)
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                        val chunk = ByteArray(bufferInfo.size)
                        outputBuffer.get(chunk)
                        pcmOut.write(chunk)
                    }
                    codec.releaseOutputBuffer(outputIndex, false)
                }
            }

            codec.stop()
            codec.release()
            extractor.release()

            val rawBytes = pcmOut.toByteArray()
            val stereoBytes = if (channelCount == 1) {
                AudioConstants.monoToStereo(rawBytes)
            } else {
                rawBytes
            }

            val finalDurationMs = if (durationMs > 0) {
                durationMs
            } else {
                val bytesPerMs = (sampleRate * 2 * 2L) / 1000.0
                if (bytesPerMs > 0) (stereoBytes.size / bytesPerMs).toLong() else 0L
            }

            onProgress(1.0f)

            Log.i(TAG, "Decoded '$title': ${stereoBytes.size} bytes PCM, $sampleRate Hz, duration=${finalDurationMs}ms")

            JukeboxTrack(
                title = title,
                artist = artist,
                durationMs = finalDurationMs,
                sampleRate = sampleRate,
                channelCount = 2,
                pcmBytes = stereoBytes
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode track: ${e.message}", e)
            null
        }
    }

    private fun getFileName(context: Context, uri: Uri): String {
        var name: String? = null
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        name = cursor.getString(nameIndex)
                    }
                }
            } catch (_: Exception) {}
        }
        if (name == null) {
            name = uri.lastPathSegment
        }
        return name ?: "Track.mp3"
    }
}

