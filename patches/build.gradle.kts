group = "app.revanced"

patches {
    about {
        name = "Stick Nodes Filter Addition"
        description = "an attempt to add more filters"
        source = "git@github.com/HZbutcoding/sn-patching.git"
        author = "HZ"
        contact = "HZbutcoding"
        website = "https://revanced.app"
        license = "GNU General Public License v3.0"
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-receivers")
    }
}
