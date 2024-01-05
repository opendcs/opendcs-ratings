import jetbrains.buildServer.configs.kotlin.v2019_2.*
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.commitStatusPublisher
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.dockerSupport
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.ant
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.script
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.v2019_2.failureConditions.BuildFailureOnMetric
import jetbrains.buildServer.configs.kotlin.v2019_2.failureConditions.failOnMetricChange
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.vcs
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.finishBuildTrigger
import jetbrains.buildServer.configs.kotlin.v2019_2.vcs.GitVcsRoot
import java.io.BufferedReader;
import java.io.File;

/*
The settings script is an entry point for defining a TeamCity
project hierarchy. The script should contain a single call to the
project() function with a Project instance or an init function as
an argument.

VcsRoots, BuildTypes, Templates, and subprojects can be
registered inside the project using the vcsRoot(), buildType(),
template(), and subProject() methods respectively.

To debug settings scripts in command-line, run the

    mvnDebug org.jetbrains.teamcity:teamcity-configs-maven-plugin:generate

command and attach your debugger to the port 8000.

To debug in IntelliJ Idea, open the 'Maven Projects' tool window (View
-> Tool Windows -> Maven Projects), find the generate task node
(Plugins -> teamcity-configs -> teamcity-configs:generate), the
'Debug' option is available in the context menu for the task.
*/

version = "2021.1"

object RatingsRepo : GitVcsRoot( {
    name = "MonolithRepo"
    url = "https://bitbucket.hecdev.net/scm/cwms/hec-cwms-ratings.git"
    branch = "refs/heads/main"
    branchSpec = """+:*
-:refs/pull-requests/*
    """.trim() // prevent PR/from builds until reporting to bitbucket works correctly.
    useTagsAsBranches = true
    authMethod = password {
        userName = "builduser"
        password = "credentialsJSON:stashPassword"
    }
})

project {

    params {
        param("teamcity.ui.settings.readOnly", "true")
    }

    sequential {
        buildType(Build)
    }.buildTypes().forEach { buildType(it) }
    vcsRoot(RatingsRepo)
}


object Build : BuildType({
    name = "Build, Test, and Deploy"

    // artifacts rules place your generated output in a place it can be downloaded or sent to another Build configuration
    artifactRules = """
        **/build/libs/*.jar => /
        build/sonar/report-task.txt =>
    """.trimIndent()


    params {

    }

    vcs {
        root(RatingsRepo)
    }

    steps {
        gradle {
            tasks = "build"
            name = "build and test Project"
            jdkHome = "%env.JDK_17_0_x64%"
            gradleParams = "-Pcwms.image=registry.hecdev.net/cwms_schema_installer:22.1.1-SNAPSHOT -Poracle.version=registry.hecdev.net/oracle/database:19.3.0-ee -Pteamcity.build.branch=%teamcity.build.branch%-%teamcity.agent.name%"
        }
        gradle {
            tasks = "sonarqube"
            name = "SonarQube Analysis"
            gradleParams = "-x test -Dsonar.login=%system.SONAR_TOKEN% -Dsonar.host.url=https://sonarqube.hecdev.net"
            jdkHome = "%env.JDK_17_0_x64%"
        }
        gradle {
            tasks = "publish"
            name = "Publish Artifacts"
            jdkHome = "%env.JDK_17_0_x64%"
            gradleParams = "-PmavenUser=%env.NEXUS_USER% -PmavenPassword=%env.NEXUS_PASSWORD%"
            conditions {
                matches("teamcity.build.branch", "(refs/tags/.*|refs/heads/main)")
            }
        }
            
    }

    triggers {
        vcs {
        }
    }

    failureConditions {
        executionTimeoutMin = 180
        failOnMetricChange {
            metric = BuildFailureOnMetric.MetricType.ARTIFACT_SIZE
            units = BuildFailureOnMetric.MetricUnit.DEFAULT_UNIT
            comparison = BuildFailureOnMetric.MetricComparison.MORE
            compareTo = value()
            stopBuildOnFailure = true
            param("metricThreshold", "3MB")
        }
    }

    features {
        commitStatusPublisher {
            publisher = bitbucketServer {
                url = "https://bitbucket.hecdev.net"
                userName = "builduser"
                password = "credentialsJSON:stashPassword"
            }
        }
        feature {
            type = "halfbaked-sonarqube-report-plugin"
        }
        dockerSupport {
            loginToRegistry = on {
                dockerRegistryId = "PROJECT_EXT_17_0"
            }
        }
    }

    requirements {
        contains("docker.server.osType", "linux")
    }
})
