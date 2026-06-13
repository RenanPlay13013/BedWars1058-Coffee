base {
    archivesName = "resetadapter-aswm"
}



dependencies {
    compileOnly(project(":bedwars-api"))
    compileOnly("org.spigotmc:spigot-api:1.8.8-R0.1-SNAPSHOT")
    compileOnly("com.grinderwolf:slimeworldmanager-api:2.2.1")
    implementation("com.flowpowered:flow-nbt:2.0.0")
    compileOnly("commons-io:commons-io:2.13.0")
    compileOnly("org.jetbrains:annotations:24.0.1")
}
