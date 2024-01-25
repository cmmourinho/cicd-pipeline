node("master") {
        stage('Prepare Pipeline Settings') {
            script {
                checkout scm
                serviceNameConfig = sh (script: "echo ${JOB_NAME} | cut -d '/' -f 3 ",returnStdout: true).trim()
                loadConfig = readYaml file: "./uat/pipeline-config.yaml"
                loadVersionConfig = readYaml file: "./uat/release-config.yaml"
                appReleaseVersion = loadVersionConfig["appVersionSettings"]["releaseVersion"] 
                globalPiplineConfig = loadConfig["settings"]["global-settings"]
                pipelineConfig = loadConfig["settings"]["${serviceNameConfig}"]
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
                    selectedAppReleaseVersion = input message: 'User input required', ok: 'Ok',
                    parameters: [choice(name: 'APP_RELEASE_VERSION', choices: "${appReleaseVersion.keySet().join('\n')}", description: 'Select Application Release Version To Deploy')]
                    msReleaseVersion =  appReleaseVersion.get(selectedAppReleaseVersion)
                    echo "This will deploy :"
                    msReleaseVersion.each{k,v -> println "$k version: $v"}
                    for (def key in msReleaseVersion.keySet()) {
                        echo "****** Trigger ${key} ******"
                        build job: "${key}/${key}-deployment-pipeline", parameters: [string(name: 'APP_RELEASE_VERSION', value: "${selectedAppReleaseVersion}")]
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
