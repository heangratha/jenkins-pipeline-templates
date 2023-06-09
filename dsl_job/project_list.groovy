class ProjectList {

    /**
    * DEPLOYMENT PIPELINES
    */
    def deployment_pipelines = [
        [
            id: 'foo-bar',
            name: 'foo.bar.internal',
            repository: 'git@github.com:heangratha/jenkins-seedjob-templates.git',
            recipient_list: 'heangratha08@gmail.com'
        ]
    ]
}
