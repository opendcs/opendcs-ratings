import jetbrains.buildServer.configs.kotlin.v2019_2.*
import jetbrains.buildServer.configs.kotlin.v2019_2.buildFeatures.commitStatusPublisher
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.ant
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.script
import jetbrains.buildServer.configs.kotlin.v2019_2.buildSteps.gradle
import jetbrains.buildServer.configs.kotlin.v2019_2.failureConditions.BuildFailureOnMetric
import jetbrains.buildServer.configs.kotlin.v2019_2.failureConditions.failOnMetricChange
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.vcs
import jetbrains.buildServer.configs.kotlin.v2019_2.triggers.finishBuildTrigger
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

project {

    params {
        param("teamcity.ui.settings.readOnly", "true")
    }

    sequential {
        buildType(Build)
        // you could also have a parallel section if there is more to run
        buildType(Deploy)
    }.buildTypes().forEach { buildType(it) }

}


object Build : BuildType({
    name = "Build and Test"

    // artifacts rules place your generated output in a place it can be downloaded or sent to another Build configuration
    artifactRules = """
        build/libs/*.jar => /
        build/sonar/report-task.txt =>
    """.trimIndent()


    params {

    }

    vcs {
        root(DslContext.settingsRoot)
    }

    steps {
        gradle {
            tasks = ":build"
            name = "build Project"
        }
        gradle {
            tasks = ":test"
            name = "test Project"
        }
        script {
            name = "read in coverage"
            scriptContent = """
                # the *Test.* is there in addition to *Test as the teamcity reading is also reading in the callbacks that were made for the unit tests.
                echo "##teamcity[jacocoReport dataPath='build/jacoco/test.exec' includes='hec.*' classpath='build/classes/main' sources='src' exclude='*Test']"
            """.trimIndent()
        }
        gradle {
            tasks = ":sonarqube"
            name = "SonarQube Analysis"
            gradleParams = "-Dsonar.login=%system.SONAR_TOKEN% -Dsonar.host.url=https://sonarqube.hecdev.net"
        }
    }

    triggers {
        vcs {
        }
    }

    failureConditions {
        executionTimeoutMin = 15
        failOnMetricChange {
            metric = BuildFailureOnMetric.MetricType.ARTIFACT_SIZE
            units = BuildFailureOnMetric.MetricUnit.DEFAULT_UNIT
            comparison = BuildFailureOnMetric.MetricComparison.MORE
            compareTo = value()
            stopBuildOnFailure = true
            param("metricThreshold", "1MB")
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
    }

    requirements {
        // not needed for a simple java project, but left here as example
        //contains("docker.server.osType", "linux")
    }
})


object Deploy : BuildType({
    name = "Deploy to Nexus"

    artifactRules = """

    """.trimIndent()

    vcs {
        root(DslContext.settingsRoot)
    }

    steps {
        gradle {
            tasks = ":publish"
            gradleParams = "-DmavenUser=%env.NEXUS_USER% -DmavenPassword=%env.NEXUS_PASSWORD%"
        }
    }

    // for this example deployed releases will always from from master
    // you could choose any other valid branch filter.
    triggers {
        finishBuildTrigger {
            buildType = "${Build.id}"
            successfulOnly = true
            branchFilter = """
                +:main
            """.trimIndent()

        }

    }

    requirements {
    }


})