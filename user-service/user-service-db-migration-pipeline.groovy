@Library("jenkins-lib")_

node("master") {
    stage('Check Jenkins node') {
        script {
            isOnline = nodesByLabel 'STMMHAPU1 || STMMHAPU2'
            if (isOnline.isEmpty()) {
                echo "node offline failing job"
                throw new Exception("STMMHAPU1 || STMMHAPU2 Offline")
            }
        }
        stage('Prepare Pipeline Settings') {
            script {
                checkout scm
                serviceNameConfig = sh (script: "echo ${JOB_NAME} | cut -d '/' -f 3 ",returnStdout: true).trim()
                loadConfig = readYaml file: "./uat/pipeline-config.yaml"
                globalPiplineConfig = loadConfig["settings"]["global-settings"]
                pipelineConfig = loadConfig["settings"]["${serviceNameConfig}"]
                userWhoRunPipeline = currentBuild.getBuildCauses('hudson.model.Cause$UserIdCause').userName
                userIdWhoRunPipeline = currentBuild.getBuildCauses()[0].userId
                isTriggeredByUpstream = currentBuild.getBuildCauses()
            }
        }
    }
}
pipeline {
    agent {
        // label "STMMHAPU1 || STMMHAPU2"
        label "STMMHAPU1 || STMMHAPU2"
    }
    parameters {
        gitParameter branch: 'release', branchFilter: 'origin/release.*', defaultValue: 'origin/release', description: 'Select release branch', name: 'RELEASE_VERSION', quickFilterEnabled: false, selectedValue: 'TOP', sortMode: 'DESCENDING_SMART', tagFilter: '*', type: 'GitParameterDefinition', useRepository: "${pipelineConfig.general.gitRepositoryUrl}"
    }
    options {
        ansiColor('css')
        azureKeyVault(
          credentialID: "${globalPiplineConfig.credentials.azureLogin}", 
          keyVaultURL: "${globalPiplineConfig.general.keyVaultURL}", 
          secrets: [
              [envVariable: 'dbCredetials', name: "${pipelineConfig.credentials.dbLogin}", secretType: 'Secret'],
              [envVariable: 'dbFieldEncryptKey', name: "${globalPiplineConfig.credentials.dbFieldEncryptKey}", secretType: 'Secret']
          ]
        )
    }
    stages {
        stage('Git Checkout') {
            steps {
                script {
                    echo "****** Check out ${pipelineConfig.general.gitRepositoryUrl} from branch ${RELEASE_VERSION} ******"
                    checkout scm: [$class : 'GitSCM', branches: [[name: "${RELEASE_VERSION}"]], extensions: [[$class: 'SubmoduleOption', parentCredentials: true, recursiveSubmodules: true]],
                    userRemoteConfigs: [[credentialsId: "${globalPiplineConfig.credentials.jenkinsBitbucketClone}", url: "${pipelineConfig.general.gitRepositoryUrl}"]]]
                }
            }
        }
        stage('Run Migration') {
            steps {
                script {
                    echo "****** Run Migration ${pipelineConfig.general.serviceName} from branch ${RELEASE_VERSION} ******"
                    lib_dbMigration.be("${pipelineConfig.credentials.dbLogin}","${globalPiplineConfig.general.dbHost}","${pipelineConfig.general.dbName}","${globalPiplineConfig.credentials.npmConfig}","${globalPiplineConfig.general.dbHostIp}","${globalPiplineConfig.general.configForEnvironment}")
                }
            }
        }
    }
    post {
        always {
            script {
                if (!isTriggeredByUpstream.toString().contains("upstream")){
                    lib_teamNotification("${userWhoRunPipeline}","${userIdWhoRunPipeline}","${RELEASE_VERSION}","${globalPiplineConfig.general.configForEnvironment}","${globalPiplineConfig.general.teamChannelNotificationUrl}")
                }else{
                    echo "Team Notification Skip Because Upstream Trigger. Status will report by upstream pipeline"
                }
                
            }
        }
    }
}