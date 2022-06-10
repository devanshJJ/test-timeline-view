package timeline.org.example

import org.gradle.api.Plugin
import org.gradle.api.Project
import timeline.org.example.timelineViewTask;

public class timelineViewPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {

        // configuration for adding RunListener
        project.tasks.named('test'){
            maxParallelForks 3
            jvmArgs "-javaagent:${classpath.find { it.name.contains('junit-foundation') }.absolutePath}"
            testLogging.showStandardStreams = true
        }
        project.ext{
            junitFoundation = project.configurations.runtimeClasspath.resolvedConfiguration.resolvedArtifacts.find { it.name == 'junit-foundation' }

        }
        project.dependencies {

            testImplementation 'com.nordstrom.tools:junit-foundation:16.0.2'

        }



        project.tasks.create('createHtmlReport',timelineViewTask)
    }
}
