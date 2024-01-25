@Library("jenkins-lib")_
def APP_RELEASE_VERSION = params.APP_RELEASE_VERSION
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
                loadVersionConfig = readYaml file: "./uat/release-config.yaml"
                appReleaseVersion = loadVersionConfig["appVersionSettings"]["releaseVersion"] 
                if (APP_RELEASE_VERSION) {
                msReleaseVersion =  appReleaseVersion.get(APP_RELEASE_VERSION)
                version = msReleaseVersion["${serviceNameConfig}"]
                msGitBranch = pipelineConfig.general.branchToClone + version.split("-").first()

                }
                // userCause = currentBuild.rawBuild.getCause(Cause.UserIdCause)
                // upstreamCause = currentBuild.rawBuild.getCause(Cause.UpstreamCause)
                // println("User cause: " + userCause)
                // println("Upstream cause: " + upstreamCause)
                // upstreamJob = Jenkins.getInstance().getItemByFullName(upstreamCause.getUpstreamProject(), hudson.model.Job.class)
                // upstreamBuild = upstreamJob.getBuildByNumber(upstreamCause.getUpstreamBuild())
                // realUpstreamCause = upstreamBuild.getCause(Cause.UserIdCause)
                   
                // echo "${realUpstreamCause}"
                // echo "${realUpstreamCause.getUserId()}"
            
            }
        }
    }
}
pipeline {
    agent {
        // label "STMDEVOPSLGAD1 || STMMHAPU1 || STMMHAPU2"
        label "STMMHAPU1 || STMMHAPU2"
    }
    //parameters {
       //booleanParam(defaultValue: false, description: 'Disable DB Migration:', name: 'DISABLE_DB_MIGRATION')
        //gitParameter branch: 'release', branchFilter: 'origin/release.*', defaultValue: 'origin/release', description: 'Select release branch', name: 'RELEASE_VERSION', quickFilterEnabled: false, selectedValue: 'TOP', sortMode: 'DESCENDING_SMART', tagFilter: '*', type: 'GitParameterDefinition', useRepository: "${pipelineConfig.general.gitRepositoryUrl}"
    //}
    options {
        ansiColor('css')
    }
    stages {
        stage('Select Release') {
            when {
               allOf {
                    expression { APP_RELEASE_VERSION == null }                     
               }
            }
            steps {
                script {
                    selectedAppReleaseVersion = input message: 'User input required', ok: 'Ok',
                    parameters: [choice(name: 'APP_RELEASE_VERSION', choices: "${appReleaseVersion.keySet().join('\n')}", description: 'Select Application Release Version To Deploy')]
                    msReleaseVersion =  appReleaseVersion.get(selectedAppReleaseVersion)
                    version = msReleaseVersion["${serviceNameConfig}"]
                    msGitBranch = pipelineConfig.general.branchToClone + version.split("-").first()
                    echo "${version}"
                }
            }
        }
        stage('Git Checkout') {
            steps {
                script {
                    echo "****** Check out ${pipelineConfig.general.gitRepositoryUrl} from branch ${msGitBranch} ******"
                    scmVar = checkout scm: [$class : 'GitSCM', branches: [[name: "${msGitBranch}"]], extensions: [[$class: 'SubmoduleOption', parentCredentials: true, recursiveSubmodules: true]],
                    userRemoteConfigs: [[credentialsId: "${globalPiplineConfig.credentials.jenkinsBitbucketClone}", url: "${pipelineConfig.general.gitRepositoryUrl}"]]]
                    commitSha = scmVar.GIT_COMMIT.substring(0,11)
                    tagVersion = msGitBranch.split("/").last()
                    version = "${tagVersion}-${commitSha}"
                }
            }
        }
        stage('Run Unit Test and Sonarqube Scan') {
            steps {
                script {
                    echo "****** Run Unit Test for ${pipelineConfig.general.serviceName} from branch ${msGitBranch} ******"
                    lib_unitTest.fe("${globalPiplineConfig.credentials.sonarqubeApiKey}","${globalPiplineConfig.credentials.npmConfigFe}","${pipelineConfig.general.sonarqubeProjectKey}")
                }
            }
        }
        stage('Quality gate') {
            steps {
                script {
                    echo "****** Check Quality gate for ${pipelineConfig.general.serviceName} ******"
                    lib_sonarqube.qualityGateCheck("${globalPiplineConfig.credentials.sonarqubeApiKey}","${pipelineConfig.general.sonarqubeProjectKey}")
                }
            }
        }  
        stage('Build Docker Image') {
            steps {
                script {
                    echo "****** Build docker image for ${pipelineConfig.general.serviceName} ******"
                    lib_imageBuild.fe("${version}","${globalPiplineConfig.general.dockerRepositoryUrl}","${pipelineConfig.general.serviceName}","${pipelineConfig.general.runMode}",,"${globalPiplineConfig.general.applicationNumber}","${globalPiplineConfig.general.projectName}")
                }                
            }
        }  
        stage('Push Docker Image to ACR') {
            steps {
                script {
                    echo "****** Push Docker Image to ACR for ${pipelineConfig.general.serviceName} ******"
                    lib_acr.pushDockerImage("${globalPiplineConfig.credentials.azureLogin}","${globalPiplineConfig.general.dockerRepositoryUrl}","${pipelineConfig.general.serviceName}","${version}","${globalPiplineConfig.general.acrNameToLogin}","${globalPiplineConfig.general.devSecOpsAzureSubscriptionId}","${globalPiplineConfig.general.applicationNumber}","${globalPiplineConfig.general.projectName}")
                }                
            }
        }
        stage('Prepare Deployment Manifest') {
            steps {
                script {
                    echo "****** Prepare Deployment Manifest for ${pipelineConfig.general.serviceName}  ******"
                    lib_aksDeployment.prepareManifest("${globalPiplineConfig.general.branchToCloneDeployManifest}","${globalPiplineConfig.credentials.jenkinsBitbucketClone}","${globalPiplineConfig.general.deployManifestGitUrl}","${globalPiplineConfig.general.dockerRepositoryUrl}","${pipelineConfig.general.serviceName}","${version}","${globalPiplineConfig.general.configForEnvironment}","${globalPiplineConfig.general.applicationNumber}","${globalPiplineConfig.general.projectName}")
                }
            }
        }
        stage('Deploy to AKS') {
            steps {
                script {
                    echo "****** Deploy ${pipelineConfig.general.serviceName} to AKS  ******"
                    lib_aksDeployment.deployAks("${globalPiplineConfig.credentials.azureLogin}","${globalPiplineConfig.aksCluster.context}","${pipelineConfig.general.namespace}","${pipelineConfig.general.serviceName}","${globalPiplineConfig.general.configForEnvironment}","${globalPiplineConfig.general.azureCloudResourceGroup}","${globalPiplineConfig.general.myHomeAzureSubscriptionId}")
                }
            }
        }
        stage('Trigger QA Pipeline') {
            steps {
                script {
                    echo "****** Trigger QA Pipeline for ${pipelineConfig.general.serviceName}  ******"
                }
            }
        }
    }
}
