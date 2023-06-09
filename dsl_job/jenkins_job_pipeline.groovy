/**
 * This block reads project list from another file to build the pipeline
 * @return array deployment_pipelines
 */


import hudson.FilePath

String seed_job_path = 'dsl_job'
String project_list_name = 'project_list.groovy'

def workspace_path = hudson.model.Executor.currentExecutor().getCurrentWorkspace().absolutize()
def seed_job_full_path = new FilePath(workspace_path, seed_job_path).getRemote()

def script = new GroovyScriptEngine(seed_job_full_path).with {
  loadScriptByName(project_list_name)
}

this.metaClass.mixin script



/**
 * PIPELINE HELPERS
 */
def pipelineLog(String s) {
    println this.getClass().getName() + ': ' + s
}



/**
 * PIPELINE HELPERS
*/
class WEPipelineHelper {
    static void emailRecipientListDevs(jobContext) {
        jobContext.with {
            sendTo {
                developers()
                requester()
                upstreamCommitter()
            }
        }
    }

    static void emailRecipientListInvolved(jobContext) {
        jobContext.with {
            sendTo {
                culprits()
                developers()
                failingTestSuspects()
                firstFailingBuildSuspects()
                recipientList()
                requester()
                upstreamCommitter()
            }
        }
    }

    static void defaultEmailTriggers(jobContext) {
        jobContext.with {
            aborted {
                emailRecipientListDevs(delegate)
            }
            firstFailure {
                emailRecipientListInvolved(delegate)
            }
            stillFailing {
                emailRecipientListDevs(delegate)
            }
            fixed {
                emailRecipientListInvolved(delegate)
            }
            notBuilt {
                emailRecipientListDevs(delegate)
            }
            firstUnstable {
                emailRecipientListInvolved(delegate)
            }
            stillUnstable {
                emailRecipientListDevs(delegate)
            }
        }
    }
}

/**
 * Generate the default pipeline for all projects
 */


pipelineLog('Generating the default pipelines.')

deployment_pipelines.each { project ->
    project_name   = (project['id'] + '-' + project['name']).replaceAll(' ', '-')
    recipient_list = ('recipient_list' in project) ? project['recipient_list'] : '$DEFAULT_RECIPIENTS'
    build_branch   = ('branch' in project) ? project['branch'] : 'origin/master'
    gitlab_url     = ('gitlab_url' in project) ? project['gitlab_url'] : 'https://git.foo.bar/' + project['id'] + '/Distribution'
    gitlab_version = '11.10'
    trigger_repos  = ('triggers' in project) ? project['triggers'] : []
    multi_repo     = (trigger_repos.size() > 0) ? true : false

    // we keep a reference for each job for later customization
    def jobs = [:]
    project['jobs'] = jobs

    folder(project_name)

    if ('stages' in project) {
        stages = project['stages']
    } else {
        stages = [
            'latest': 'demo',
            'demo': 'live',
            'live': ''
        ]
    }

    jobs['build'] = job(project_name + '/build') {
        description('Initial job for project ' + project['name'])
        using('template-pipeline')

        if (('node' in project) && (project['node'] != '')) {
            label(project['node'])
        }
        if (multi_repo) {
            multiscm {
                git {
                    remote {
                        url(project['repository'])
                    }
                    branch(build_branch)
                    browser {
                        gitLab(gitlab_url, gitlab_version)
                    }
                    extensions {
                        cleanBeforeCheckout()
                    }
                }

                trigger_repos.each { repo_key, repo_data ->
                    git {
                        remote {
                            url(repo_data['repository'])
                        }
                        branch(('branch' in repo_data) ? repo_data['branch'] : build_branch)
                        extensions {
                            relativeTargetDirectory('multiscm/' + repo_key)
                        }
                    }
                }
            }
        } else {
            scm {
                git {
                    remote {
                        url(project['repository'])
                    }
                    branch(build_branch)
                    browser {
                        gitLab(gitlab_url, gitlab_version)
                    }
                }
            }
        }

        steps {
            if (multi_repo) {
                shell('echo "GIT_COMMIT=`git -C . rev-parse HEAD`\nDEPLOY_ID=${BUILD_ID}" > build.properties')
            } else {
                shell('echo "GIT_COMMIT=${GIT_COMMIT}\nDEPLOY_ID=${BUILD_ID}" > build.properties')
            }

            shell('''\
                TARGET_FILE=`ls -1 [Cc]onfig*/[Jj]enkins*/build.sh | head -n 1`
                if [ -z "$TARGET_FILE" ]; then
                    echo "ERROR No script for this pipeline stage found."
                    exit 1
                else
                    echo "Executing $TARGET_FILE"
                    $TARGET_FILE
                fi
            '''.stripIndent().trim())
        }

        publishers {
            if (('disable_first_stage_auto_deploy' in project) && (project['disable_first_stage_auto_deploy'])) {
                if ('first_stage' in project) {
                    buildPipelineTrigger(project_name + '/deploy-' + project['first_stage']) {
                        parameters {
                            propertiesFile('build.properties')
                        }
                    }
                } else {
                    buildPipelineTrigger(project_name + '/deploy-latest') {
                        parameters {
                            propertiesFile('build.properties')
                        }
                    }
                }
            } else {
                downstreamParameterized {
                    if ('first_stage' in project) {
                        trigger(project_name + '/deploy-' + project['first_stage']) {
                            parameters {
                                propertiesFile('build.properties')
                            }
                        }
                    } else {
                        trigger(project_name + '/deploy-latest') {
                            parameters {
                                propertiesFile('build.properties')
                            }
                        }
                    }
                }
            }

            // Report UNIT Test Results
            //   run phpunit with: --log-junit build/logs/junit.xml
            archiveXUnit {
                jUnit {
                    pattern('build/logs/junit.xml')
                    skipNoTestFiles(true)
                    failIfNotNew(true)
                    deleteOutputFiles(true)
                    stopProcessingIfError(true)
                }
            }



            extendedEmail {
                recipientList(recipient_list)
                defaultSubject('$DEFAULT_SUBJECT')
                defaultContent('$DEFAULT_CONTENT')
                triggers {
                    WEPipelineHelper.defaultEmailTriggers(delegate)
                }
            }
        }
    }

    /**
     * Define stages for deployment jobs
     *
     *    'current[_stage]' triggers deployment to 'target[_stage]'
     *
     * The last stage does not trigger any further deployments.
     */
    stages.each { current, target ->
        jobs['deploy-' + current] = job(project_name + '/deploy-' + current) {
            description('Deploy job for project ' + project['name'] + ' to ' + current)
            using('template-pipeline')

            blockOn([
                '^' + project_name + '/uat-' + current,
                '^014-701-we-infrastructure/seed_job'
            ]) {
                blockLevel('GLOBAL')
            }

            parameters {
                stringParam('GIT_COMMIT', '', 'Git commit ID sent to pipeline build job by GitLab hook.')
                stringParam('DEPLOY_ID', '', 'Build ID of pipeline starting build job used for fetching the right compiled resources.')
            }

            if (('node' in project) && (project['node'] != '')) {
                label(project['node'])
            }

            scm {
                git {
                    remote {
                        url(project['repository'])
                    }
                    branch('$GIT_COMMIT')
                    browser {
                        gitLab(gitlab_url, gitlab_version)
                    }
                    extensions {
                        cleanAfterCheckout()
                        ignoreNotifyCommit()
                    }
                }
            }

            steps {
                shell('''\
                    TARGET_FILE=`ls -1 [Cc]onfig*/[Jj]enkins*/[Dd]eploy/###DEPLOY_SCRIPT###.sh | head -n 1`
                    if [ -z "$TARGET_FILE" ]; then
                        echo "ERROR No script for this pipeline stage found."
                        exit 1
                    else
                        echo "Executing $TARGET_FILE"
                        $TARGET_FILE
                    fi
                '''.replaceAll('###DEPLOY_SCRIPT###', current)
                   .stripIndent().trim())
            }

            if (target != '') {
                publishers {
                    // Use customized pipeline if configured
                    if (('auto_deploy_stages' in project) && (target in project['auto_deploy_stages'])) {
                        downstreamParameterized {
                            trigger(
                                project_name + '/uat-' + current + ', ' +
                                project_name + '/deploy-' + target
                            ) {
                                parameters {
                                    predefinedProp('GIT_COMMIT', '$GIT_COMMIT')
                                    predefinedProp('DEPLOY_ID', '$DEPLOY_ID')
                                }
                            }
                        }
                    } else {
                        buildPipelineTrigger(project_name + '/deploy-' + target) {
                            parameters {
                                predefinedProp('GIT_COMMIT', '$GIT_COMMIT')
                                predefinedProp('DEPLOY_ID', '$DEPLOY_ID')
                            }
                        }
                        downstreamParameterized {
                            trigger(project_name + '/uat-' + current) {
                                parameters {
                                    predefinedProp('GIT_COMMIT', '$GIT_COMMIT')
                                    predefinedProp('DEPLOY_ID', '$DEPLOY_ID')
                                }
                            }
                        }
                    }
                }
            } else {
                // Current job is the last stage in our pipeline.
                // Thus no further deployments get triggered.
                // This leaves room for other publishers e.g. send email to customers etc.
                publishers {
                    downstreamParameterized {
                        trigger(project_name + '/uat-' + current) {
                            parameters {
                                predefinedProp('GIT_COMMIT', '$GIT_COMMIT')
                                predefinedProp('DEPLOY_ID', '$DEPLOY_ID')
                            }
                        }
                    }
                }
            }

            publishers {
                extendedEmail {
                    recipientList(recipient_list)
                    defaultSubject('$DEFAULT_SUBJECT')
                    defaultContent('$DEFAULT_CONTENT')
                    triggers {
                        WEPipelineHelper.defaultEmailTriggers(delegate)
                    }
                }
            }
        }

        /**
         * UAT job
         */
        jobs['uat-' + current] = job(project_name + '/uat-' + current) {
            description('UAT job for project ' + project['name'] + ' on stage ' + current)
            using('template-pipeline')

            blockOn([
                '^' + project_name + '/deploy-' + current,
                '^014-701-we-infrastructure/seed_job'
            ]) {
                blockLevel('GLOBAL')
            }

            parameters {
                stringParam('GIT_COMMIT', '', 'Git commit ID sent to pipeline build job by GitLab hook.')
                stringParam('DEPLOY_ID', '', 'Build ID of pipeline starting build job used for fetching the right compiled resources.')
            }

            if (('node' in project) && (project['node'] != '')) {
                label(project['node'])
            }

            scm {
                git {
                    remote {
                        url(project['repository'])
                    }
                    branch('$GIT_COMMIT')
                    browser {
                        gitLab(gitlab_url, gitlab_version)
                    }
                    extensions {
                        ignoreNotifyCommit()
                    }
                }
            }

            steps {
                // This has be wrapped around with double quote so that variable ${current} can be replaced
                shell('''\
                    TARGET_FILE=`ls -1 [Cc]onfig*/[Jj]enkins*/[Uu]at/###DEPLOY_SCRIPT###.sh | head -n 1`
                    if [ -z "$TARGET_FILE" ]; then
                        echo "ERROR No script for this pipeline stage found."
                        exit 1
                    else
                        echo "Executing $TARGET_FILE"
                        $TARGET_FILE
                    fi
                '''.replaceAll('###DEPLOY_SCRIPT###', current)
                   .stripIndent().trim())
            }

            configure { projectXml ->
                projectXml / publishers / 'org.tap4j.plugin.TapPublisher' << {
                    testResults('yslow.tap')
                    discardOldReports('true')
                    todoIsFailure('false')
                    planRequired('false')
                }
            }

            publishers {
                publishHtml {
                    report('build/logs/') {
                        reportFiles('uat-report.html')
                        reportName('UAT Report')
                        allowMissing(true)
                        keepAll(false)
                        alwaysLinkToLastBuild(true)
                    }
                    report('build/logs/') {
                        reportFiles('index.html')
                        reportName('Sitespeed Report')
                        allowMissing(true)
                        keepAll(false)
                        alwaysLinkToLastBuild(true)
                    }
                }

                extendedEmail {
                    recipientList(recipient_list)
                    defaultSubject('$DEFAULT_SUBJECT')
                    defaultContent('''\
                        $DEFAULT_CONTENT

                        Attachments: build/logs/uat-report.html, build/logs/index.html
                    '''.stripIndent().trim())
                    attachmentPatterns('build/logs/uat-report.html, build/logs/index.html')
                    triggers {
                        WEPipelineHelper.defaultEmailTriggers(delegate)
                    }
                }
            }
        }
    }

    buildPipelineView(project_name + '/pipeline') {
        alwaysAllowManualTrigger(true)
        // consoleOutputLinkStyle()
        displayedBuilds(10)
        filterBuildQueue(false)
        filterExecutors(false)
        refreshFrequency(2)
        selectedJob(project_name + '/build')
        showPipelineDefinitionHeader()
        showPipelineParameters()
        showPipelineParametersInHeaders()
        title('CD Pipeline for ' + project['name'])
        triggerOnlyLatestJob(false)
    }
}

// Apply custom configuration if class exists:
//   PipelineJobSettings/Project_XXX_XXX.groovy -> PipelineJobSettings.Project_XXX_XXX
// This approach is based on the JOB DSL recommendations:
//   https://github.com/jenkinsci/job-dsl-plugin/wiki/Real-World-Examples#import-other-files-ie-with-class-definitions-into-your-script
//   https://groups.google.com/forum/#!topic/job-dsl-plugin/jueturR7sCo
pipelineLog('Looking for project-specific settings.')

// We need the GroovyClassLoader. Java's `Class.forName` does not work.
def groovyClassLoader = getClass().getClassLoader()

// We need the Jenkins PrintStream to log (println) in other classes
//   http://stackoverflow.com/a/7754410
def jenkinsConfig = new HashMap()
jenkinsConfig.putAll(getBinding().getVariables())
def jenkinsConsole = jenkinsConfig['out']

deployment_pipelines.each { project ->
    // classname    = 'PipelineJobSettings.Project_123_456_project_name'
    def classname   = ('PipelineJobSettings/Project_' + project['id'] + '_' + project['name']).replace('-', '_').replace('.', '_').replace('/', '.')
    def projectname = (project['id'] + '-' + project['name']).replaceAll(' ', '-')
    try {
        def handler = groovyClassLoader.loadClass(classname)
        pipelineLog('Applying custom configuration for: ' + projectname + ' (' + classname + ')')
        handler.jenkinsConsole = jenkinsConsole
        handler.pipelineHelper = WEPipelineHelper
        handler.customizeJobs(project)
    } catch (ClassNotFoundException e) {
        // no customization for this project
    }
}

pipelineLog('Done.')
