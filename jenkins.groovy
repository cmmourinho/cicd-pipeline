pipeline {
    agent {
        label 'master'
    }
    
    stages {
        stage('Clone Repository') {
            steps {
                git credentialsId: 'cmmourinho-github', url: 'https://github.com/cmmourinho/nodejs.git'
            }
        }

        stage('Hello World') {
            steps {
                echo "Hello, World"
            }
        }
    }
}