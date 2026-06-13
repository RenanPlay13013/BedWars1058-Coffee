base {
    archivesName = "resetadapter-slimepaper"
}


dependencies {
    compileOnly(project(":bedwars-api"))
    compileOnly("org.spigotmc:spigot-api:1.8.8-R0.1-SNAPSHOT")
    compileOnly("com.infernalsuite.aswm:api:1.20-R0.1-SNAPSHOT")
    implementation("com.flowpowered:flow-nbt:2.0.2")
    compileOnly("commons-io:commons-io:2.13.0")
    compileOnly("org.jetbrains:annotations:24.0.1")
}
