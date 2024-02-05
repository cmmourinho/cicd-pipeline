@Library('mylibs') _

pipeline {
    agent any

    stages {
        stage('Hello World') {
            steps {
                script {
                    helloWorld()
                }
            }
        }

        stage('Thailand Greeting') {
            steps {
                script {
                    thailand()
                }
            }
        }
    }
}
