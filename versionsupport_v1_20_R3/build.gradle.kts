dependencies {
    compileOnly(project(":bedwars-api"))
    compileOnly(project(":versionsupport_common"))
    compileOnly("org.spigotmc:spigot:1.20.3-R0.1-SNAPSHOT") {
        exclude("commons-codec", "commons-codec")
    }
    implementation("com.github.RenanPlay13013:SidebarLib-Coffee:master-SNAPSHOT")
}
