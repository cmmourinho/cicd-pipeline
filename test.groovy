@Library('mylibs') _

pipeline {
    agent any

    stages {
        stage('Thailand Greeting') {
            steps {
                script {
                    echo.thailand()
                }
            }
        }
    }
}

