pipeline {
    agent {
        label 'master'
    }
    
    stages {
        stage('Clone Repository') {
            steps {
                git 'https://github.com/cmmourinho/nodejs.git'
            }
        }

        stage('Hello World') {
            steps {
                echo "Hello, World"
            }
        }
    }
}