dependencies {
    compileOnly(project(":bedwars-api"))
    compileOnly(project(":versionsupport_common"))
    compileOnly("commons-io:commons-io:2.13.0")
    compileOnly("org.jetbrains:annotations:24.0.1")
    compileOnly("org.spigotmc:spigot:1.20.2-R0.1-SNAPSHOT") {
        exclude("commons-codec", "commons-codec")
    }
}
