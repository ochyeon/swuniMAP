pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()

        // 카카오 벡터맵 SDK 전용 저장소 추가
        maven{
            url = uri("https://devrepo.kakao.com/nexus/content/groups/public/")
        }
    }
}

rootProject.name = "swuniMAP"
include(":app")
 