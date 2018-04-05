PUBLISH_MAJOR_VERSION = '0'
PUBLISH_MINOR_VERSION = '1'
API_VERSION = '1'

REPO_JOB = node() {
  dir("/tmp/${env.BUILD_ID}") {
    sh "(git clean -dfx && git reset --hard) || :"
    checkout changelog: false, poll: false, scm: [$class: 'GitSCM', branches: [
    [name: '*/master']
    ], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [
    [credentialsId: 'dc985300-8b25-4d0e-88ff-b44517009691', url: 'http://stash.neovest.com:7990/scm/neohc/ahoy.git']
    ]]
    stash includes: '**', name: "ahoy-master"
    load "groovy/jobs/maven/fix-datapump.groovy"
  }
}

REPO_JOB.runBuild()