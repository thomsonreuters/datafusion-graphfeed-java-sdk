
environments {
    LOCAL {
        graphfeed.api.url = 'http://localhost:9090'
        graphfeed.auth.url = 'https://play.api.apigarden-qa.int.thomsonreuters.com/oauth/v1/accesstoken?grant_type=client_credentials'
    }
    DEV {
        graphfeed.api.url = 'https://play.api.apigarden-qa.int.thomsonreuters.com/v1/graphfeed'
        graphfeed.auth.url = 'https://play.api.apigarden-qa.int.thomsonreuters.com/oauth/v1/accesstoken?grant_type=client_credentials'
    }
    QA {
        graphfeed.api.url = 'https://api.apigarden-qa.int.thomsonreuters.com/v1/graphfeed'
        graphfeed.auth.url = 'https://api.apigarden-qa.int.thomsonreuters.com/oauth/v1/accesstoken?grant_type=client_credentials'
    }
    PROD {
        graphfeed.api.url = 'https://api.thomsonreuters.com/v1/graphfeed'
        graphfeed.auth.url = 'https://api.thomsonreuters.com/oauth/v1/accesstoken?grant_type=client_credentials'
    }
}