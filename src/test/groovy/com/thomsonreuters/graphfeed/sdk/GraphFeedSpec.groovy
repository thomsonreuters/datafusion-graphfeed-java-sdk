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
package com.thomsonreuters.graphfeed.sdk

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream
import org.apache.http.HttpHeaders
import org.apache.http.HttpStatus
import spock.lang.Specification

import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

class GraphFeedSpec extends Specification {

    private static final String AUTH_TOKEN = 'FEua9gyWtSWjcUtLin69APNjm9rT'

    GraphFeed graphFeed

    void setup() {
        graphFeed = Spy(GraphFeed)
        graphFeed.url = Environment.PROD.config.graphfeed.api.url as String
        graphFeed.authUrl = Environment.PROD.config.graphfeed.auth.url as String
        graphFeed.clientId = 'clientId'
        graphFeed.clientSecret = 'clientSecret'
    }

    void 'bulk - bzip2'() {
        given:
        ByteArrayOutputStream outStream = new ByteArrayOutputStream()
        Map authClient = createAuthClient()
        MockConnection mockConn = new MockConnection(HttpStatus.SC_OK, true)

        when:
        ConsumeResponse consumeResponse = graphFeed.bulk("1", outStream)

        then:
        1 * graphFeed.createUrlConnection(Environment.PROD.config.graphfeed.api.url + "/contentSet/1/bulk") >> mockConn
        1 * graphFeed.createRestClient(Environment.PROD.config.graphfeed.auth.url as String) >> authClient
        authClient.headers['Authorization'] == 'Basic ' + "clientId:clientSecret".getBytes('iso-8859-1').encodeBase64()
        mockConn.verify(AUTH_TOKEN, Encoding.BZIP2.type)
        new BZip2CompressorInputStream(new ByteArrayInputStream(outStream.toByteArray())).text == MockConnection.RDF
        consumeResponse.statusCode == HttpStatus.SC_OK
        consumeResponse.resumptionToken == MockConnection.RESUMPTION_TOKEN
        consumeResponse.requestedVersion == MockConnection.CURRENT_VERSION
        consumeResponse.latestVersion == MockConnection.LATEST_VERSION
        consumeResponse.size == 225
    }

    void 'bulk - gzip'() {
        given:
        ByteArrayOutputStream outStream = new ByteArrayOutputStream()
        Map authClient = createAuthClient()
        MockConnection mockConn = new MockConnection(HttpStatus.SC_OK, false)

        when:
        ConsumeResponse consumeResponse = graphFeed.bulk("1", outStream, null, Encoding.GZIP)

        then:
        1 * graphFeed.createUrlConnection(Environment.PROD.config.graphfeed.api.url + "/contentSet/1/bulk") >> mockConn
        1 * graphFeed.createRestClient(Environment.PROD.config.graphfeed.auth.url as String) >> authClient
        authClient.headers['Authorization'] == 'Basic ' + "clientId:clientSecret".getBytes('iso-8859-1').encodeBase64()
        mockConn.verify(AUTH_TOKEN, Encoding.GZIP.type)
        new GZIPInputStream(new ByteArrayInputStream(outStream.toByteArray())).text == MockConnection.RDF
        consumeResponse.statusCode == HttpStatus.SC_OK
        consumeResponse.resumptionToken == MockConnection.RESUMPTION_TOKEN
        consumeResponse.requestedVersion == MockConnection.CURRENT_VERSION
        consumeResponse.latestVersion == MockConnection.LATEST_VERSION
        consumeResponse.size == 191
    }

    void 'consumeFully'() {
        given:
        ByteArrayOutputStream outStream = new ByteArrayOutputStream()
        Map authClient = createAuthClient()
        MockConnection mockConn1 = new MockConnection(HttpStatus.SC_OK, false, "token2", "rdf1")
        MockConnection mockConn2 = new MockConnection(HttpStatus.SC_OK, false, "token3", "rdf2")
        MockConnection mockConn3 = new MockConnection(HttpStatus.SC_NO_CONTENT, false, "token3", "")

        when:
        ConsumeResponse consumeResponse = graphFeed.consumeFully("1", "1", "token1", outStream)

        then:
        1 * graphFeed.createUrlConnection(Environment.PROD.config.graphfeed.api.url + "/contentSet/1/consume?version=1&resumptionToken=token1") >> mockConn1
        1 * graphFeed.createRestClient(Environment.PROD.config.graphfeed.auth.url as String) >> authClient
        1 * graphFeed.createUrlConnection(Environment.PROD.config.graphfeed.api.url + "/contentSet/1/consume?version=1&resumptionToken=token2") >> mockConn2
        1 * graphFeed.createUrlConnection(Environment.PROD.config.graphfeed.api.url + "/contentSet/1/consume?version=1&resumptionToken=token3") >> mockConn3
        authClient.headers['Authorization'] == 'Basic ' + "clientId:clientSecret".getBytes('iso-8859-1').encodeBase64()
        mockConn1.verify(AUTH_TOKEN, null)
        mockConn2.verify(AUTH_TOKEN, null)
        mockConn3.verify(AUTH_TOKEN, null)
        new String(outStream.toByteArray(), 'UTF-8') == "rdf1rdf2"
        consumeResponse.statusCode == HttpStatus.SC_NO_CONTENT
        consumeResponse.resumptionToken == "token3"
        consumeResponse.requestedVersion == MockConnection.CURRENT_VERSION
        consumeResponse.latestVersion == MockConnection.LATEST_VERSION
        consumeResponse.size == 8
    }

    private Map createAuthClient() {
        return [
                headers: [:],
                post: { Map args ->
                    assert args.contentType == 'application/json'
                    assert args.body == ''
                    return [
                            status: HttpStatus.SC_OK,
                            data: [
                                    issued_at: "${new Date().time}",
                                    access_token: AUTH_TOKEN
                            ]
                    ]
                }
        ]
    }
}

class MockConnection {

    public static final String RDF = "<http://data.thomsonreuters.com/sc/supplychain_agreement/4298532259_4295860302> <http://cm-well-uk-lab.int.thomsonreuters.com/meta/nn#lastUpdated> \"2016-02-12T01:08:33.255+00:00\"^^<http://www.w3.org/2001/XMLSchema#dateTime> ."
    public static final String RESUMPTION_TOKEN = 'resumption_token'
    public static final String CURRENT_VERSION = "1"
    public static final String LATEST_VERSION = "2"

    String requestAuthToken, requestAcceptEncoding
    int _responseCode
    boolean zipAsBzip2
    String resumptionToken
    String rdf

    public MockConnection(int responseCode, boolean zipAsBzip2 = false, String resumptionToken = null, String rdf = null) {
        _responseCode = responseCode
        this.zipAsBzip2 = zipAsBzip2
        this.resumptionToken = (resumptionToken ?: RESUMPTION_TOKEN)
        this.rdf = (rdf ?: RDF)
    }

    public void verify(requestAuthToken, requestAcceptEncoding) {
        assert "Bearer $requestAuthToken".toString() == this.requestAuthToken
        assert requestAcceptEncoding == this.requestAcceptEncoding
    }

    void setRequestProperty(String propName, String propValue) {
        if (HttpHeaders.AUTHORIZATION == propName) {
            requestAuthToken = propValue
            // Swap these when the unfortunate Apigee handling (botching) of content-encoding is fixed.
//        } else if (HttpHeaders.ACCEPT_ENCODING == propName) {
        } else if (GraphFeed.HEADER_CUSTOM_ACCEPT_ENCODING == propName) {
            requestAcceptEncoding = propValue
        }
    }

    int getResponseCode() { return _responseCode }

    String getHeaderField(String headerName) {
        if (headerName == GraphFeed.HEADER_RESUMPTION_TOKEN) {
            return resumptionToken
            // Swap these when the unfortunate Apigee handling (botching) of content-encoding is fixed.
//        } else if (headerName == HttpHeaders.CONTENT_ENCODING) {
        } else if (headerName == GraphFeed.HEADER_CUSTOM_CONTENT_ENCODING) {
            return (zipAsBzip2 ? 'bzip2' : 'gzip')
        } else if (headerName == GraphFeed.HEADER_CONTENT_SET_VERSION) {
            return CURRENT_VERSION
        } else if (headerName == GraphFeed.HEADER_CONTENT_SET_LATEST_VERSION) {
            return LATEST_VERSION
        }
    }

    void connect() {}
    void disconnect() {}
    String getResponseMessage() { return '' }

    InputStream getInputStream() {
        return getZippedInputStream(rdf)
    }

    private InputStream getZippedInputStream(String text) {
        ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream()
        OutputStream zippedOutStream
        if (zipAsBzip2) {
            zippedOutStream = new BZip2CompressorOutputStream(byteOutStream)
        } else {
            zippedOutStream = new GZIPOutputStream(byteOutStream)
        }
        zippedOutStream << new ByteArrayInputStream(text.bytes)
        zippedOutStream.close()
        return new ByteArrayInputStream(byteOutStream.toByteArray())
    }
}
