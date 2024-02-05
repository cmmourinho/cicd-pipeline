@Library('mylibs') _

pipeline {
    agent any

    stages {
        stage('prepare') {
            steps {
	        script {
  		    checkout scm
		    loadConfig = readYaml file: "./global-config.yaml"
                    globalConfig = loadConfig["settings"]["global-settings"]
		}
	    }
	}
        stage('Thailand Greeting') {
            steps {
                script {
                    test.thailand()
 		    echo """
			my docker url is ${globalConfig.general.dockerRepositoryUrl}
		        echo my acr is ${globalConfig.general.acrNameToLogin}
		    """
                }
            }
        }
    }
}

