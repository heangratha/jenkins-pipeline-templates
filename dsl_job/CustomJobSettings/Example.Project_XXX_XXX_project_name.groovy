// If project already exists, make sure the name is the same
// by copy the name from Jenkins interface and paste here
project_name = 'XXX-XXX-project-name'

// CAUTION
// The following line should be enabled only if the project does NOT exist already in Jenkins
// which mean project with no pipeline
//folder(project_name)



// Job configuration
// Replace MY_CUSTOM_JOB_NAME with your job name all in small letters and separate by hyphen (-)
job(project_name + '/MY_CUSTOM_JOB_NAME') {
    description 'CHANGEME'

    // Available templates are 'template-custom' and 'template-php'
    using('template-custom')

    // If you want to block this job if another job is building,
    // specify that another job name here (comma separated list of jobs supported)
    blockOn([
        '^XXX-XXX-project-name/job-name',
        '^pipeline_jobs/seed_job'
    ]) {
        blockLevel('GLOBAL')
    }

    scm {
        git {
            remote {
                url('')
            }
            branch('')
            extensions {
                cleanAfterCheckout()
                ignoreNotifyCommit()
            }
        }
    }

    triggers {
        scm('@daily')
    }

    steps {
        shell('''\
            CHANGEME
        '''.stripIndent().trim())
    }
}



// Another job configuration
// Replace MY_CUSTOM_JOB_NAME with your job name all in small letters and separate by hyphen (-)
job(project_name + '/MY_ANOTHER_CUSTOM_JOB_NAME') {
    description 'CHANGEME'

    using('template-custom')

    // ADD MORE SETTINGS AS YOU NEED
}
