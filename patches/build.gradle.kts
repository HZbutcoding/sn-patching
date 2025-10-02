group = "app.revanced"

patches {
    about {
        name = "Stick Nodes Filter Addition"
        description = "an attempt to add more filters"
        source = "https://github.com/HZbutcoding/sn-patching"
        author = "ReVanced + HZ"
        contact = "HZbutcoding"
        website = "https://revanced.app"
        license = "GNU General Public License v3.0"
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs = listOf("-Xcontext-receivers")
    }
}

val localProperties = File(rootDir, "local.properties")
if (localProperties.exists()) {
    localProperties.forEachLine {
        val (key, value) = it.split("=", limit = 2)
        project.extra[key.trim()] = value.trim()
    }
}
