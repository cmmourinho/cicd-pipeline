@Library('mylibs') _

pipeline {
    agent any

    stages {
        stage('Hello World') {
            steps {
                script {
                    echo.helloWorld()
                }
            }
        }

        stage('Thailand Greeting') {
            steps {
                script {
                    echo.thailand()
                }
            }
        }
    }
}
