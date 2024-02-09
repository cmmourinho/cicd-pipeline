pipeline {
    agent {
        label 'master'
    }
    
    stages {
        stage('Clone Repository') {
            steps {
                //checkout scmGit(branches: [[name: '*/main']], extensions: [], userRemoteConfigs: [[credentialsId: 'cmmourinho-github', url: 'https://github.com/cmmourinho/nodejs.git']])
                git branch: "main", credentialsId: "cmmourinho-github" ,url: "https://github.com/cmmourinho/nodejs.git"
                sh "ls -la"
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
