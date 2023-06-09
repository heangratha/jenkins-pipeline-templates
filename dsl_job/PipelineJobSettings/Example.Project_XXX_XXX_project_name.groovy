package PipelineJobSettings;

// Rename the file and class to include your project-ID and project name, as defined in the seed job.
// For example, for the following project,
//        [
//                id:             'foo-bar',
//                name:           'foo-bar.com',
//                repository:     'git@foo:bar/Distribution.git',
//                recipient_list: 'devops@foo-bar.co',
//        ],
// The class would be: Project_foo_bar_com
// The filename would be:  Project_foo_bar_com.groovy
class Project_XXX_XXX_project_name {

    // To log to the Jenkins console, use: jenkinsConsole.println
    static java.io.PrintStream jenkinsConsole

    // Local reference to WEPipelineHelper
    static pipelineHelper

    static void customizeJobs(def config) {
        // TODO: Re-use the value from parent class
        def project_name = (config['id'] + '-' + config['name']).replaceAll(' ', '-')

        config['jobs']['build'].with {
            // This block is the same as
            //
            // job {
            //     /* this here */
            // }
            //
            // for the build-job. But everything from the default pipeline
            // is already here. Add/Remove/Modify the job configuration here
            // as you wish.
            //
            // Example 1: Add a HTML report
            // ----------------------------
            // publishers {
            //     project / publishers / 'htmlpublisher.HtmlPublisher' << {
            //         'reportTargets' {
            //             'htmlpublisher.HtmlPublisherTarget' {
            //                 reportName('UAT Report')
            //                 reportDir('build/logs/')
            //                 reportFiles('uat-report.html')
            //                 keepAll(false)
            //                 allowMissing(true)
            //                 wrapperName('htmlpublisher-wrapper.html')
            //             }
            //         }
            //     }
            // }
            //
            // Example 2: Remove all publishers then add Unit Test Report and email notification
            // ---------------------------------------------------------------------------------
            // configure { project ->
            //     project.remove(project / publishers)
            // }
            //
            // publishers {
            //     archiveXUnit {
            //         jUnit {
            //             pattern('build/logs/junit.xml')
            //             skipNoTestFiles(true)
            //             failIfNotNew(true)
            //             deleteOutputFiles(true)
            //             stopProcessingIfError(true)
            //         }
            //     }
            //
            //     extendedEmail {
            //         recipientList(recipient_list)
            //         defaultSubject('$DEFAULT_SUBJECT')
            //         defaultContent('$DEFAULT_CONTENT')
            //         triggers {
            //             pipelineHelper.defaultEmailTriggers(delegate)
            //         }
            //     }
            // }
            //
            // For more information, visit
            // JOB DSL Wiki: https://github.com/jenkinsci/job-dsl-plugin/wiki
            // JOB DSL Reference: https://jenkinsci.github.io/job-dsl-plugin/
        }

        config['jobs']['deploy-latest'].with {
            // Add/Remove/Modify the job configuration here.
        }

        config['jobs']['uat-latest'].with {
            // Add/Remove/Modify the job configuration here.
        }

        config['jobs']['deploy-demo'].with {
            // Add/Remove/Modify the job configuration here.
        }

        config['jobs']['uat-demo'].with {
            // Add/Remove/Modify the job configuration here.
        }

        config['jobs']['deploy-live'].with {
            // Add/Remove/Modify the job configuration here.
        }

        config['jobs']['uat-live'].with {
            // Add/Remove/Modify the job configuration here.
        }

    }
}
