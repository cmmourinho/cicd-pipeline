node("master") {
        stage('Prepare Pipeline Settings') {
            script {
                checkout scm
                loadConfig = readYaml file: "./pipeline-config.yaml"
                loadVersionConfig = readYaml file: "./release-config.yaml"
                appReleaseVersion = loadVersionConfig["appVersionSettings"]["releaseVersion"] 
                globalPiplineConfig = loadConfig["settings"]["global-settings"]
            }
    }
}
pipeline {
    agent {
        // label "STMDEVOPSLGAD1 || STMMHAPU1 || STMMHAPU2"
        label "master"
    }
    stages {
        stage('Select App Release Version') {
            steps {
                script {
                    echo "****** Getting App Release Version ******"
                    // selectedAppReleaseVersion = input message: 'User input required', ok: 'Ok',
                    selectedAppReleaseVersion = "1.0.0"
                    parameters: [choice(name: 'APP_RELEASE_VERSION', choices: "${appReleaseVersion.keySet().join('\n')}", description: 'Select Application Release Version To Deploy')]
                    msReleaseVersion =  appReleaseVersion.get(selectedAppReleaseVersion)
                    echo "${msReleaseVersion}"
                    echo "This will deploy :"
                    def fullcommit = "${GIT_COMMIT}"
                    def version = fullcommit.substring(0, 11)
                    echo "${version}"
                    msReleaseVersion.each{s,r -> println "$s version: $r"}
                    for (def key in msReleaseVersion.keySet()) {
                        echo "****** Trigger ${key} *****"
                        echo "${key}"
                    }
                    
                }
            }
        }
        stage('Trigger QA Pipeline') {
            steps {
                script {
                    echo "****** Trigger QA Pipeline for ******"
                }
            }
        }
    }
}
