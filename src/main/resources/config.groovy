/*
 * Copyright (c) 2016 Thomson Reuters
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
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
