plugins {
    `java-library`
    `maven-publish`
}



dependencies {
    compileOnly("com.google.common:google-collect:1.0")
    compileOnly("org.spigotmc:spigot:1.16.5-R0.1-SNAPSHOT")
    implementation("com.github.RenanPlay13013:SidebarLib-Coffee:master-SNAPSHOT")
    compileOnly("net.md-5:bungeecord-chat:1.21-R0.2")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
