@Library("jenkins-lib")_
/**
 *  Pipeline to setup and deploy kong api gateway service
 *  to use this pipeline define following value in jenkins job config
 *  AZURE_PRINCIPLE_IDENTITY    : azure service principle identity name in jenkins eg. A0542_MYWORK_JENKINS_DEVOPS
 *  AKS_CONTEXT                 : aks context name to deploy eg. aks-a0542-mywork-uat
 *  NAMESPACE                   : aks name space to deploy eg. a0542-mywork-uat
 *  DEPLOY_ENV                  : environment name to deploy. (dev, sit, uat, prd)
 *  AZURE_RESOURCE_GROUP        : azure resource group name eg. MyWork-UAT
 */


pipeline {
    agent {
        label "master"
    }
    parameters {
        booleanParam description: 'run releated resource for kong', name: 'CONFIG'
        booleanParam description: 'run helm for kong', name: 'HELM'
    }
    options {
        disableConcurrentBuilds()
        buildDiscarder logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '15')
    }
    environment {
        PATH = '/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin'
    }
    stages {
        stage('PREPARATION AGENT') {
            steps {
                echo '*************** Stage: Preparation_Agent  **************'
                script {
                    checkout scm
                    serviceNameConfig = sh (script: "echo ${JOB_NAME} | cut -d '/' -f 3 ",returnStdout: true).trim()
                    loadConfig = readYaml file: "./dev/pipeline-config.yaml"
                    globalPiplineConfig = loadConfig["settings"]["global-settings"]
                    pipelineConfig = loadConfig["settings"]["${serviceNameConfig}"]
                    dateFormat = sh (script: "date ${globalPiplineConfig.general.dockerTagDateFormat}",returnStdout: true).trim()
                    userWhoRunPipeline = currentBuild.getBuildCauses('hudson.model.Cause$UserIdCause').userName
                    userIdWhoRunPipeline = currentBuild.getBuildCauses()[0].userId
                    isTriggeredByUpstream = currentBuild.getBuildCauses()
                }
            }
        }
        stage('CHECKOUT DEPLOYMENT CONFIG') {
            steps {
                echo '*************** Stage: Preparation_Agent  **************'
                checkout scm: [$class           : 'GitSCM',
                               branches         : [[name: "${globalPiplineConfig.general.branchToCloneDeployManifest}"]],
                               extensions       : [[$class: 'SubmoduleOption', parentCredentials: true, recursiveSubmodules: true]],
                               userRemoteConfigs: [[credentialsId: "${globalPiplineConfig.credentials.jenkinsBitbucketClone}", url: "${globalPiplineConfig.general.deployManifestGitUrl}"]]]
            }
        }
        stage('LOGIN TO AZURE') {
            when {
                anyOf {
                    expression { return params.CONFIG }
                    expression { return params.HELM }
                }
            }
            steps {
                script {
                    withCredentials([azureServicePrincipal("${globalPiplineConfig.credentials.azureLogin}")]) {
                        sh 'az login --service-principal -u $AZURE_CLIENT_ID -p $AZURE_CLIENT_SECRET -t $AZURE_TENANT_ID'
                        sh 'az account set --subscription $AZURE_SUBSCRIPTION_ID'
                        sh "az aks get-credentials --resource-group ${globalPiplineConfig.general.azureCloudResourceGroup} --name ${globalPiplineConfig.aksCluster.context} --overwrite-existing"
                        sh "kubelogin convert-kubeconfig -l azurecli"
                    }
                }
            }
        }
        stage('DEPLOY RELATED CONFIGURATION') {
            when {
                expression { return params.CONFIG }
            }
            steps {
                script {
                    withCredentials([azureServicePrincipal("${globalPiplineConfig.credentials.azureLogin}")]) {
                        sh "kubectl config use-context ${globalPiplineConfig.aksCluster.context}"
                        sh "kubectl config set-context --current --namespace=${pipelineConfig.general.namespace}"
                        sh """
                        	for i in ./${globalPiplineConfig.general.configForEnvironment}/kong-ingress-service/*.yml
                            do
                            	kubectl apply -f \$i
                            done
                        """
                    }
                }
            }
        }
        stage('DEPLOY KONG WITH HELM') {
            when {
                expression { return params.HELM }
            }
            steps {
                script {
                    withCredentials([azureServicePrincipal("${globalPiplineConfig.credentials.azureLogin}")]) {
                        sh "kubectl config use-context ${globalPiplineConfig.aksCluster.context}"
                        sh "kubectl config set-context --current --namespace=${pipelineConfig.general.namespace}"
                        sh "helm upgrade --install kong-ingrss helm -n kong --values ${globalPiplineConfig.general.configForEnvironment}/kong-ingress-service/helm-value.yaml"
                    }
                }
            }
        }
    }
    post {
        always {
            script {
                if (userIdWhoRunPipeline == null){
                    userIdWhoRunPipeline = env.userIdWhoRunPipeline
                }
                if (userWhoRunPipeline.isEmpty()){
                    userWhoRunPipeline = env.userWhoRunPipeline
                }
                //lib_teamNotification("${userWhoRunPipeline}","${userIdWhoRunPipeline}","${pipelineConfig.general.branchToClone}","${globalPiplineConfig.general.configForEnvironment}","${globalPiplineConfig.general.teamChannelNotificationUrl}")
            }
        }
        cleanup {
            cleanWs()
        }
    }
}
