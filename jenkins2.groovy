pipeline {
    agent {
        label 'master'
    }
    
    stages {
        stage('Clone Repository') {
            steps {
                git credentialsId: 'cmmourinho-github', url: 'https://github.com/cmmourinho/nodejs2.git', branch: 'main'
            }
        }

        stage('Hello World') {
            steps {
                echo "Hello, World"
            }
        }

        stage('Example') {
            steps {
                script {
                    // Get the user who triggered the pipeline using Cause$UserIdCause
                    def userWhoRunPipeline = currentBuild.getBuildCauses('hudson.model.Cause$UserIdCause').userName

                    // Get the user ID who triggered the pipeline (not always available)
                    def userIdWhoRunPipeline = currentBuild.getBuildCauses()[0]?.userId

                    echo "User who triggered the pipeline: ${userWhoRunPipeline}"
                    echo "User ID who triggered the pipeline: ${userIdWhoRunPipeline}"
                }
            }
        }
    }
}