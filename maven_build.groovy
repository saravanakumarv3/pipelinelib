void call(Map config) {
  pipeline {
    agent { label 'any' }
    options {
      buildDiscarder(
          logRotator(
              artifactDaysToKeepStr: '0',
              artifactNumToKeepStr: '0',
              daysToKeepStr: '10',
              numToKeepStr: '10'
          )
      )
      disableConcurrentBuilds()
      skipDefaultCheckout()
      timeout(time: 60, unit: 'MINUTES')
      timestamps()
    }

    stages {
      stage('Checkout') {
        steps {
          git(
              repoUrl: config.gitRepoName,
              repoGroup: config.gitRepoGroup,
              credentialsId: config.gitSshKey,
              defaultBranch: config.gitBranch ?: 'master',
          )
        }
      }
      stage('Gradle Build') {
        steps {
          script {
              withMaven('Maven3.5') {
                "mvn ${config.gradleTasks}".toString()
                )
              }
          }
        }
      }

      stage('Sonarqube Analysis') {
        when {
          expression {
            return env.sonarQubeExecution
          }
        }
        environment {
          sonarUrl = 'http://sonar.pe.int.thomsonreuters.com/'
        }
        steps {
          script {
            withMaven('Maven3.5') {
                "mvn ${config.gradleTasks}".toString()
                )
              }
          }
        }
      }

      stage('Appscan Analysis') {
        when {
          expression {
            return env.gitlabBranch == 'master' 
          }
        }
        steps {
          //Appscan step
        }
      }
      stage('uDeploy ') {
        steps {
          //udeploy
        }
      }
    }
    post {
      always {
        script {
          if (config.emailRecipients) {
            emailext(
                subject: "${env.JOB_BASE_NAME} - Build # ${env.BUILD_NUMBER} - Success!",
                to: config.emailRecipients,
                body: "${env.JOB_BASE_NAME} - Build # ${env.BUILD_NUMBER} - Success:\nCheck console output at ${env.BUILD_URL} to view the results."
            )
          }
        }
      }
    }
  }
}