import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

val packageName = project.extensions.findByName("android")?.let { androidExt ->
    androidExt::class.members.find { it.name == "namespace" }?.call(androidExt) as? String
} ?: throw GradleException("❌ Android packagename not found!")

fun extractTopLevelKeys(json: String): List<String> {
    val keys = mutableListOf<String>()
    var i = 0
    var depth = 0
    while (i < json.length) {
        when (json[i]) {
            '{' -> {
                depth++
                i++
            }

            '}' -> {
                depth--
                i++
            }

            '"' -> {
                if (depth == 1) {
                    val start = i + 1
                    val end = json.indexOf('"', start)
                    if (end > start) {
                        val key = json.substring(start, end)
                        // look ahead for :
                        var j = end + 1
                        while (j < json.length && json[j].isWhitespace()) j++
                        if (j < json.length && json[j] == ':') {
                            keys.add(key)
                        }
                        i = end + 1
                        continue
                    }
                }
                i++
            }

            else -> i++
        }
    }
    return keys
}

val script by tasks.registering {
    val xmlFile = file("src/main/res/xml/remote_config_defaults.xml")
    val outputDir = file("build/generated/source/remoteConfig/")
    val outputFile = File(outputDir, "RemoteConfig.kt")

    doLast {
        if (!xmlFile.exists()) throw GradleException("❌ RemoteConfig XML not found!")
        outputDir.mkdirs()
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlFile)
        val entries = doc.getElementsByTagName("entry")

        var adConfigJson = ""
        for (i in 0 until entries.length) {
            val entry = entries.item(i) as? Element ?: continue
            val key = entry.getElementsByTagName("key").item(0)?.textContent?.trim()
            if (key?.startsWith("ad_config") == true) {
                val valueElement = entry.getElementsByTagName("value").item(0)
                adConfigJson = valueElement?.textContent?.trim() ?: ""
                if (adConfigJson.startsWith("<value>")) {
                    adConfigJson = adConfigJson.removePrefix("<value>").removeSuffix("</value>").trim()
                }
                break
            }
        }

        if (adConfigJson.isBlank()) throw GradleException("❌ ad_config JSON not found in remote_config_defaults.xml!")

        // --- Only top-level keys ---
        val topLevelKeys = extractTopLevelKeys(adConfigJson)

        val builder = StringBuilder()
        builder.appendLine("package $packageName")
        builder.appendLine("import com.cyber.ads.remote.*")
        builder.appendLine()
        builder.appendLine("object RemoteConfig {")

        for (key in topLevelKeys) {
            val constName = key.uppercase()
            when {
                key == "ads_splash" -> {
                    builder.appendLine("    var $constName = SplashHolder(\"$key\")")
                }

                key == "native_language" || key == "native_intro" -> {
                    builder.appendLine("    var $constName = NativeMultiHolder(\"$key\")")
                }

                key.startsWith("banner_") -> {
                    builder.appendLine("    var $constName = BannerHolder(\"$key\")")
                }

                key.startsWith("native_") -> {
                    builder.appendLine("    var $constName = NativeHolder(\"$key\")")
                }

                key.startsWith("inter_") -> {
                    builder.appendLine("    var $constName = InterHolder(\"$key\")")
                }

                key.startsWith("reward_") -> {
                    builder.appendLine("    var $constName = RewardHolder(\"$key\")")
                }
            }
        }

        builder.appendLine("}")

        outputFile.writeText(builder.toString())
        println("✅ Generated: ${outputFile.absolutePath}")
    }
}

tasks.named("preBuild") {
    dependsOn("script")
}
