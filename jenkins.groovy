pipeline {
    agent {
        label 'master'
    }
    
    stages {
        stage('Hello World') {
            steps {
                echo "Hello, World"
            }
        }
    }
}