/**
 * TEMPLATES
 */

job('template-pipeline') {
    description 'This job sets default properties for all jobs'
    disabled(false)
    logRotator(-1, 10)
    quietPeriod(5)

    blockOn(['^pipeline_jobs/seed_jobs']) {
        blockLevel('GLOBAL')
    }

    wrappers {
        colorizeOutput()
        timestamps()
    }
}



job('template-custom') {
    description 'This job inherits all properties from template-pipeline but add email notification'

    using('template-pipeline')

    publishers {
        extendedEmail {
            recipientList('$DEFAULT_RECIPIENTS')
            triggers {
                statusChanged {
                    sendTo {
                        culprits()
                        developers()
                        recipientList()
                        requester()
                        upstreamCommitter()
                    }
                }
            }
        }
    }
}



job('template-php') {
    description('''\
        <img type="image/svg+xml" height="300" src="ws/build/pdepend/overview-pyramid.svg" width="500"></img>
        <img type="image/svg+xml" height="300" src="ws/build/pdepend/dependencies.svg" width="500"></img>
    '''.stripIndent().trim())

    using('template-pipeline')

    steps {
        ant()
    }

    publishers {


        plotBuildData {
            plot('phploc', 'phploc.csv') {
                title('A - Lines of code')
                numberOfBuilds(100)
                yAxis('Lines of Code')
                csvFile('build/logs/phploc.csv') {
                    includeColumns('Lines of Code (LOC),Comment Lines of Code (CLOC),Non-Comment Lines of Code (NCLOC)')
                }
            }

            plot('phploc', 'phploc.csv') {
                title('B - Structures')
                numberOfBuilds(100)
                yAxis('Count')
                csvFile('build/logs/phploc.csv') {
                    includeColumns('Directories,Files,Namespaces,Interfaces,Classes,Methods,Functions,Anonymous Functions,Constants')
                }
            }

            plot('phploc', 'phploc.csv') {
                title('C - Testing')
                numberOfBuilds(100)
                yAxis('Count')
                csvFile('build/logs/phploc.csv') {
                    includeColumns('Classes,Methods,Functions,Test Clases,Test Methods')
                }
            }

            plot('phploc', 'phploc.csv') {
                title('D - Types of Classes')
                numberOfBuilds(100)
                yAxis('Count')
                csvFile('build/logs/phploc.csv') {
                    includeColumns('Classes,Abstract Classes,Concrete Classes')
                }
            }

            plot('phploc', 'phploc.csv') {
                title('E - Types of Methods')
                numberOfBuilds(100)
                yAxis('Count')
                csvFile('build/logs/phploc.csv') {
                    includeColumns('Methods,Non-Static Methods,Static Methods,Public Methods,Non-Public Methods')
                }
            }

            plot('phploc', 'phploc.csv') {
                title('F - Types of Constants')
                numberOfBuilds(100)
                yAxis('Count')
                csvFile('build/logs/phploc.csv') {
                    includeColumns('Constants,Global Constants,Class Constants')
                }
            }

            plot('phploc', 'phploc.csv') {
                title('G - Average Length')
                numberOfBuilds(100)
                yAxis('Average Non-Comment Lines of Code')
                csvFile('build/logs/phploc.csv') {
                    includeColumns('Average Class Length (NCLOC),Average Method Length (NCLOC)')
                }
            }

            plot('phploc', 'phploc.csv') {
                title('H - Relative Cyclomatic Complexity')
                numberOfBuilds(100)
                yAxis('Cyclomatic Complexity by Structure')
                csvFile('build/logs/phploc.csv') {
                    includeColumns('Cyclomatic Complexity / Lines of Code,Cyclomatic Complexity / Number of Methods')
                }
            }
        }


    }


}
