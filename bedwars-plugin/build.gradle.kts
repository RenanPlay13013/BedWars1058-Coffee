plugins {
    java
    id("com.github.johnrengelman.shadow")
}

repositories {
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://repo.codemc.io/repository/maven-public/")
    maven("https://repo.codemc.io/repository/maven-releases/")
    maven("https://repo.codemc.io/repository/maven-snapshots/")
    maven("https://simonsator.de/repo/")
    maven("https://maven.citizensnpcs.co/repo")
    maven("https://repo.alessiodp.com/releases/")
    maven("https://repo.titanvale.net/releases/")
}

dependencies {
    implementation("io.papermc:paperlib:1.0.8")
    implementation("org.bstats:bstats-bukkit:3.0.2")
    implementation("com.zaxxer:HikariCP:5.0.1") {
        exclude("org.slf4j", "slf4j-api")
    }
    implementation("org.slf4j:slf4j-simple:2.0.7")
    implementation("com.andrei1058.spigot.sidebar:sidebar-dist:25.2.2-SNAPSHOT")
    implementation("commons-io:commons-io:2.13.0")

    compileOnly("de.simonsator:Party-and-Friends-MySQL-Edition-Spigot-API:1.5.4-RELEASE")
    compileOnly("de.simonsator:Spigot-Party-API-For-RedisBungee:1.0.3-SNAPSHOT")
    compileOnly("de.simonsator:DevelopmentPAFSpigot:1.0.239")
    compileOnly("com.alessiodp.parties:parties-api:3.2.9")
    compileOnly("net.citizensnpcs:citizens-main:2.0.30-SNAPSHOT") {
        exclude("junit", "junit")
        exclude("org.bstats", "bstats-bukkit")
    }
    compileOnly("net.milkbowl.vault:VaultAPI:1.7") {
        exclude("org.bukkit", "bukkit")
    }
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("com.andrei1058.vipfeatures:vipfeatures-api:[1.0,)")
    compileOnly("org.spigotmc:spigot:1.8.8-R0.1-SNAPSHOT")
    compileOnly("net.md-5:bungeecord-chat:1.18-R0.1-SNAPSHOT")

    implementation(project(":bedwars-api"))
    implementation(project(":versionsupport_common"))
    implementation(project(":versionsupport_1_8_R3"))
    implementation(project(":versionsupport_1_12_R1"))
    implementation(project(":versionsupport_v1_16_R3"))
    implementation(project(":versionsupport_v1_17_R1"))
    implementation(project(":versionsupport_v1_18_R2"))
    implementation(project(":versionsupport_v1_19_R2"))
    implementation(project(":versionsupport_v1_19_R3"))
    implementation(project(":versionsupport_v1_20_R1"))
    implementation(project(":versionsupport_v1_20_R2"))
    implementation(project(":versionsupport_v1_20_R3"))
    implementation(project(":versionsupport_v1_20_R4"))
    implementation(project(":versionsupport_v1_21_R3"))
    implementation(project(":resetadapter_aswm"))
    implementation(project(":resetadapter_slime"))
    implementation(project(":resetadapter_slimepaper"))
}

tasks {
    shadowJar {
        relocate("org.bstats", "com.andrei1058.bedwars.libs.bstats")
        relocate("com.zaxxer.hikari", "com.andrei1058.bedwars.libs.hikari")
        relocate("org.slf4j", "com.andrei1058.bedwars.libs.slf4j")
        relocate("com.andrei1058.spigot.sidebar", "com.andrei1058.bedwars.libs.sidebar")
        relocate("com.andrei1058.spigot.sidebarutils", "com.andrei1058.bedwars.libs.updater")

        archiveClassifier.set("")
    }

    processResources {
        filesMatching("plugin.yml") {
            expand(mapOf("project" to mapOf("version" to project.version)))
        }
    }
}
