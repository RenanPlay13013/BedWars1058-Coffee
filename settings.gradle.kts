import org.gradle.kotlin.dsl.mavenCentral

rootProject.name = "BedWars1058"

include(
    "bedwars-api",
    "bedwars-plugin",
    "resetadapter_aswm",
    "resetadapter_slime",
    "resetadapter_slimepaper",
    "versionsupport_1_8_R3",
    "versionsupport_1_12_R1",
    "versionsupport_common",
    "versionsupport_v1_16_R3",
    "versionsupport_v1_17_R1",
    "versionsupport_v1_18_R2",
    "versionsupport_v1_19_R2",
    "versionsupport_v1_19_R3",
    "versionsupport_v1_20_R1",
    "versionsupport_v1_20_R2",
    "versionsupport_v1_20_R3",
    "versionsupport_v1_20_R4",
    "versionsupport_v1_21_R3"
)


dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        mavenLocal()

        maven("https://oss.sonatype.org/content/repositories/snapshots")
        maven("https://repo.codemc.io/repository/nms/")
        maven("https://repo.andrei1058.com/snapshots")
        maven("https://repo.andrei1058.com/releases")

        maven("https://repo.fusesource.com/nexus/content/repositories/releases-3rd-party/")

        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
        maven("https://hub.spigotmc.org/nexus/repository/public/")
        maven("https://repo.papermc.io/repository/maven-public/")

        maven("https://jitpack.io")


        maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
        maven("https://repo.codemc.io/repository/maven-public/")
        maven("https://repo.codemc.io/repository/maven-releases/")
        maven("https://repo.codemc.io/repository/maven-snapshots/")
        maven("https://simonsator.de/repo/")
        maven("https://maven.citizensnpcs.co/repo")
        maven("https://repo.alessiodp.com/releases/")
        maven("https://repo.titanvale.net/releases/")

        maven("https://repo.glaremasters.me/repository/concuncan/")


        maven("https://repo.infernalsuite.com/repository/maven-snapshots/")


    }
}