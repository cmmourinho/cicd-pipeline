node("master") {
    stage('Check Jenkins node') {
        stage('Prepare Pipeline Settings') {
            script {
                checkout scm
                serviceNameConfig = sh (script: "echo ${JOB_NAME} | cut -d '/' -f 3 ",returnStdout: true).trim()
                loadConfig = readYaml file: "/pipeline-config.yaml"
                globalPiplineConfig = loadConfig["settings"]["global-settings"]
                pipelineConfig = loadConfig["settings"]["${serviceNameConfig}"]
                dateFormat = sh (script: "date ${globalPiplineConfig.general.dockerTagDateFormat}",returnStdout: true).trim()
                userWhoRunPipeline = currentBuild.getBuildCauses('hudson.model.Cause$UserIdCause').userName
                userIdWhoRunPipeline = currentBuild.getBuildCauses()[0].userId
            }
        }
    }
}
pipeline {
    agent {
        label "master"
    }
    triggers {
      GenericTrigger(causeString: "Trigger From Bitbucket", genericVariables: [[defaultValue: '', key: 'RELEASE_VERSION', regexpFilter: '', value: '$.changes[0].ref.displayId'], [defaultValue: '', key: 'userIdWhoRunPipeline', regexpFilter: '', value: '$.actor.name'] , [defaultValue: '', key: 'userWhoRunPipeline', regexpFilter: '', value: '$.actor.displayName']], regexpFilterExpression: '^develop/*', regexpFilterText: '$RELEASE_VERSION', token: "${serviceNameConfig}", tokenCredentialId: '')
    }
    parameters {
       booleanParam(defaultValue: false, description: 'Disable DB Migration:', name: 'DISABLE_DB_MIGRATION')
    }
    stages {
        // stage('Git Checkout') {
        //     steps {
        //         script {
        //             echo "****** Check out ${pipelineConfig.general.gitRepositoryUrl} from branch ${pipelineConfig.general.branchToClone} ******"
        //             checkout scm: [$class : 'GitSCM', branches: [[name: "${pipelineConfig.general.branchToClone}"]], extensions: [[$class: 'SubmoduleOption', parentCredentials: true, recursiveSubmodules: true]],
        //             userRemoteConfigs: [[credentialsId: "${globalPiplineConfig.credentials.jenkinsBitbucketClone}", url: "${pipelineConfig.general.gitRepositoryUrl}"]]]
        //         }
        //     }
        // }
        stage('Trigger QA Pipeline') {
            steps {
                script {
                    echo "****** Trigger QA Pipeline for ${pipelineConfig.general.serviceName}  ******"
                }
            }
        }
    }
}