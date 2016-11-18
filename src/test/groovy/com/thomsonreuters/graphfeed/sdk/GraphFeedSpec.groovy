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

    void 'download - bzip2'() {
        given:
        ByteArrayOutputStream outStream = new ByteArrayOutputStream()
        Map authClient = createAuthClient()
        MockConnection mockConn = new MockConnection(HttpStatus.SC_OK, true)
        MockS3Connection mockS3Conn = new MockS3Connection(HttpStatus.SC_OK, true)

        when:
        ConsumeResponse consumeResponse = graphFeed.download("1", outStream)

        then:
        1 * graphFeed.createUrlConnection(Environment.PROD.config.graphfeed.api.url + "/contentSet/1/download") >> mockConn
        1 * graphFeed.createRestClient(Environment.PROD.config.graphfeed.auth.url as String) >> authClient
        1 * graphFeed.createUrlConnection(MockConnection.S3_URL_BZIP2) >> mockS3Conn
        authClient.headers['Authorization'] == 'Basic ' + "clientId:clientSecret".getBytes('iso-8859-1').encodeBase64()
        mockConn.verify(AUTH_TOKEN, Encoding.BZIP2.type)
        new BZip2CompressorInputStream(new ByteArrayInputStream(outStream.toByteArray())).text == MockS3Connection.RDF
        consumeResponse.statusCode == HttpStatus.SC_OK
        consumeResponse.resumptionToken == MockConnection.RESUMPTION_TOKEN
        consumeResponse.requestedVersion == MockConnection.CURRENT_VERSION
        consumeResponse.latestVersion == MockConnection.LATEST_VERSION
        consumeResponse.size == 225
    }

    void 'download - gzip'() {
        given:
        ByteArrayOutputStream outStream = new ByteArrayOutputStream()
        Map authClient = createAuthClient()
        MockConnection mockConn = new MockConnection(HttpStatus.SC_OK, false)
        MockS3Connection mockS3Conn = new MockS3Connection(HttpStatus.SC_OK, false)

        when:
        ConsumeResponse consumeResponse = graphFeed.download("1", outStream, null, Encoding.GZIP)

        then:
        1 * graphFeed.createUrlConnection(Environment.PROD.config.graphfeed.api.url + "/contentSet/1/download") >> mockConn
        1 * graphFeed.createRestClient(Environment.PROD.config.graphfeed.auth.url as String) >> authClient
        1 * graphFeed.createUrlConnection(MockConnection.S3_URL_GZIP) >> mockS3Conn
        authClient.headers['Authorization'] == 'Basic ' + "clientId:clientSecret".getBytes('iso-8859-1').encodeBase64()
        mockConn.verify(AUTH_TOKEN, Encoding.GZIP.type)
        new GZIPInputStream(new ByteArrayInputStream(outStream.toByteArray())).text == MockS3Connection.RDF
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

    public static final String S3_URL_GZIP = 'http://someurl.com/download/1.cache.nt.gz'
    public static final String S3_URL_BZIP2 = 'http://someurl.com/download/1.cache.nt.bz2'
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
        this.rdf = rdf
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
        if (!rdf) {
            return new ByteArrayInputStream("{\"downloadUrl\": \"${zipAsBzip2 ? S3_URL_BZIP2 : S3_URL_GZIP}\"}".getBytes('UTF-8'))
        }
        return getZippedInputStream(this.rdf)
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

class MockS3Connection {

    public static final String RDF = "<http://data.thomsonreuters.com/sc/supplychain_agreement/4298532259_4295860302> <http://cm-well-uk-lab.int.thomsonreuters.com/meta/nn#lastUpdated> \"2016-02-12T01:08:33.255+00:00\"^^<http://www.w3.org/2001/XMLSchema#dateTime> ."

    int _responseCode
    boolean zipAsBzip2
    String rdf

    public MockS3Connection(int responseCode, boolean zipAsBzip2 = false, String rdf = null) {
        _responseCode = responseCode
        this.zipAsBzip2 = zipAsBzip2
        this.rdf = (rdf ?: RDF)
    }

    int getResponseCode() { return _responseCode }

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
