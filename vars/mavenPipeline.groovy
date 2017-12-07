def call(body) {

    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    if (config.directory == null) {
        config.directory = '.'
    }

    node {
        properties([
            disableConcurrentBuilds(),
            pipelineTriggers([
                pollSCM('*/5 * * * *')
            ]),
            parameters ([
                booleanParam(name: 'isRelease', defaultValue: false, description: 'Release this build?'),
                string(name: 'releaseVersion', defaultValue: '', description: 'Provide the release version:'),
                string(name: 'developmentVersion', defaultValue: '', description: 'Provide the next development version:')
            ])
        ])
        

        try {
            stage('Checkout SCM') {
                checkout scm
            }

            stage ('Build Info') {
                echo "isRelease: ${params.isRelease}"
                echo "releaseVersion: ${params.releaseVersion}"
                echo "developmentVersion: ${params.developmentVersion}"
            }

            if (params.isRelease) {
                //Execute maven release process and receive the Git Tag for the release
                mavenRelease {
                    directory = config.directory
                    releaseVersion = params.releaseVersion
                    developmentVersion = params.developmentVersion
                } 
            }

            mavenBuild {
                directory = config.directory
                mavenSettings = config.mavenSettings
            }
        } catch (ex) {
            echo "Failed due to ${ex}: ${ex.message}"
            if (currentBuild.result == null) {
                currentBuild.result = 'FAILED'
            }
        } finally {
            //Send build notifications if needed
            notifyBuild(currentBuild.result)
        }
    }

}