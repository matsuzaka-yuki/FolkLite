package me.bmax.apatch.ui.component

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import com.topjohnwu.superuser.Shell
import me.bmax.apatch.util.getRootShell
import org.json.JSONObject
import java.io.File
import java.io.FileReader
import java.io.FileWriter

data class UmountConfig(
    val enabled: Boolean = false,
    val paths: String = ""
)

object UmountConfigManager {
    private const val TAG = "UmountConfigManager"
    private const val CONFIG_FILE_NAME = "umount_config.json"
    private const val UMOUNT_PATH_FILE = "/data/adb/fp/UmountPATH"

    var isEnabled = mutableStateOf(false)
        private set
    var paths = mutableStateOf("")
        private set

    fun loadConfig(context: Context): UmountConfig {
        return try {
            val configFile = File(context.filesDir, CONFIG_FILE_NAME)
            if (!configFile.exists()) {
                Log.d(TAG, "Config file not found, using defaults")
                return UmountConfig()
            }

            val reader = FileReader(configFile)
            val jsonContent = reader.readText()
            reader.close()

            val config = parseConfigFromJson(jsonContent) ?: UmountConfig()
            isEnabled.value = config.enabled
            paths.value = config.paths
            Log.d(TAG, "Config loaded: enabled=${config.enabled}, paths length=${config.paths.length}")
            config
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load config: ${e.message}", e)
            val defaultConfig = UmountConfig()
            isEnabled.value = defaultConfig.enabled
            paths.value = defaultConfig.paths
            defaultConfig
        }
    }

    fun saveConfig(context: Context, config: UmountConfig): Boolean {
        Log.d(TAG, "=== saveConfig start ===")
        Log.d(TAG, "enabled: ${config.enabled}, paths: '${config.paths}'")
        return try {
            val configFile = File(context.filesDir, CONFIG_FILE_NAME)
            val jsonContent = getConfigJson(config)
            val writer = FileWriter(configFile)
            writer.write(jsonContent)
            writer.close()

            isEnabled.value = config.enabled
            paths.value = config.paths
            Log.d(TAG, "Config saved: enabled=${config.enabled}")

            if (config.paths.isNotBlank()) {
                Log.d(TAG, "paths not empty, calling createUmountPathFile")
                val result = createUmountPathFile(context, config.paths)
                Log.d(TAG, "createUmountPathFile returned: $result")
            } else {
                Log.d(TAG, "paths empty, calling deleteUmountPathFile")
                deleteUmountPathFile(context)
            }

            Log.d(TAG, "Calling setUmountServiceEnabled(${config.enabled})")
            me.bmax.apatch.util.setUmountServiceEnabled(config.enabled)

            Log.d(TAG, "=== saveConfig done ===")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save config: ${e.message}", e)
            false
        }
    }

    fun getConfigJson(): String {
        return getConfigJson(UmountConfig(isEnabled.value, paths.value))
    }

    fun getConfigJson(config: UmountConfig): String {
        val jsonObject = JSONObject()
        jsonObject.put("enabled", config.enabled)
        jsonObject.put("paths", config.paths)
        return jsonObject.toString(2)
    }

    fun parseConfigFromJson(jsonString: String): UmountConfig? {
        return try {
            val jsonObject = JSONObject(jsonString)
            val enabled = jsonObject.optBoolean("enabled", false)
            val paths = jsonObject.optString("paths", "")
            UmountConfig(enabled, paths)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse JSON: ${e.message}", e)
            null
        }
    }

    private fun createUmountPathFile(context: Context, paths: String): Boolean {
        return try {
            val shell = getRootShell()

            shell.newJob().add("mkdir -p /data/adb/fp/bin").exec()

            val tempFile = File(context.cacheDir, "UmountPATH_temp")
            tempFile.writeText(paths)

            val result = shell.newJob().add(
                "cp ${tempFile.absolutePath} $UMOUNT_PATH_FILE",
                "chmod 644 $UMOUNT_PATH_FILE",
                "restorecon $UMOUNT_PATH_FILE"
            ).exec()

            tempFile.delete()

            if (result.isSuccess) {
                Log.d(TAG, "UmountPATH file created/updated successfully")
                true
            } else {
                Log.e(TAG, "Failed to create UmountPATH file: ${result.err.joinToString()}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create UmountPATH file: ${e.message}", e)
            false
        }
    }

    private fun deleteUmountPathFile(context: Context): Boolean {
        return try {
            val shell = getRootShell()
            val result = shell.newJob().add("rm -f $UMOUNT_PATH_FILE").exec()

            if (result.isSuccess) {
                Log.d(TAG, "UmountPATH file deleted successfully")
                true
            } else {
                Log.e(TAG, "Failed to delete UmountPATH file: ${result.err.joinToString()}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete UmountPATH file: ${e.message}", e)
            false
        }
    }
}
