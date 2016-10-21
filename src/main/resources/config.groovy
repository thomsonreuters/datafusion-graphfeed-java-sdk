/*
 * Copyright (C) 2016 Thomson Reuters. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
