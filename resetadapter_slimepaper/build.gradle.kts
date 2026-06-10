base {
    archivesName = "resetadapter-slimepaper"
}

repositories {
    maven("https://repo.glaremasters.me/repository/concuncan/")
    maven("https://repo.infernalsuite.com/repository/maven-snapshots/")
    maven("https://repo.titanvale.net/releases/")
}

dependencies {
    compileOnly(project(":bedwars-api"))
    compileOnly("org.spigotmc:spigot-api:1.8.8-R0.1-SNAPSHOT")
    compileOnly("com.infernalsuite.aswm:api:1.20-R0.1-SNAPSHOT")
    implementation("com.flowpowered:flow-nbt:2.0.2")
    compileOnly("commons-io:commons-io:2.13.0")
    compileOnly("org.jetbrains:annotations:24.0.1")
}
