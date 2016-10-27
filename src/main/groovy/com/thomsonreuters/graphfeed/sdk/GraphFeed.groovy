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

import groovy.json.JsonBuilder
import groovy.util.logging.Slf4j
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.HttpResponseException
import groovyx.net.http.RESTClient
import org.apache.commons.io.IOUtils
import org.apache.commons.io.output.CountingOutputStream
import org.apache.http.HttpStatus
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials

import java.util.zip.GZIPInputStream

@Slf4j
public class GraphFeed {

    protected static final String HEADER_RESUMPTION_TOKEN = 'X-DFGF-RESUMPTION-TOKEN'
    protected static final String HEADER_REMAINING_COUNT = 'X-DFGF-REMAINING-COUNT'
    protected static final String HEADER_CONTENT_SET_VERSION = 'X-DFGF-CONTENT-SET-VERSION'
    protected static final String HEADER_CONTENT_SET_LATEST_VERSION = 'X-DFGF-CONTENT-SET-LATEST-VERSION'
    protected static final String HEADER_CUSTOM_ACCEPT_ENCODING = 'X-ACCEPT-ENCODING'
    protected static final String HEADER_CUSTOM_CONTENT_ENCODING = 'X-CONTENT-ENCODING'

    String url
    String authUrl
    String clientId
    String clientSecret
    protected ApiToken apiToken

    public GraphFeed() {}

    public GraphFeed(Environment env, String clientId, String clientSecret) {
        this(env.config.graphfeed.api.url as String, env.config.graphfeed.auth.url as String, clientId, clientSecret)
    }

    public GraphFeed(String url, String authUrl, String clientId, String clientSecret) {
        this.url = url
        this.authUrl = authUrl
        this.clientId = clientId
        this.clientSecret = clientSecret
        setProxyAuthentication()
    }

    /**
     * @return An access token for the current credentials.  Will automatically generate a new token if the current is expired.
     * @throws IOException
     */
    public String getAccessToken() throws IOException {
        if (this.apiToken == null || this.apiToken.isExpired()) {
            def restClient = createRestClient(this.authUrl)
            restClient.headers['Authorization'] = 'Basic ' + "${this.clientId}:${this.clientSecret}".getBytes('iso-8859-1').encodeBase64()
            int status = HttpStatus.SC_INTERNAL_SERVER_ERROR
            try {
                def authResponse = restClient.post(contentType: 'application/json', body: '')
                status = authResponse.status
                if (status == HttpStatus.SC_OK) {
                    this.apiToken = new ApiToken(
                            issuedAt: new Date(Long.parseLong(authResponse.data.issued_at)),
                            accessToken: authResponse.data.access_token
                    )
                }
            } catch (HttpResponseException hre) {
                status = hre.response.status
                throw new IOException("Failed to create access token for clientId = '${this.clientId}' and clientSecret = '${this.clientSecret}' -- ${status}", hre)
            } catch (Exception ex) {
                throw new IOException("Failed to create access token for clientId = '${this.clientId}' and clientSecret = '${this.clientSecret}'", ex)
            }
        }
        return apiToken.accessToken
    }

    protected createRestClient(String defaultUri) {
        RESTClient restClient = new RESTClient(defaultUri ?: '/')
        setProxy(restClient)
        restClient.ignoreSSLIssues()
        restClient.getClient().getParams().setParameter("http.connection.timeout", new Integer(10 * 60 * 1000)) // ten mins
        restClient.getClient().getParams().setParameter("http.socket.timeout", new Integer(10 * 60 * 1000))
        restClient
    }

    protected RESTClient createApiClient() {
        RESTClient client = createRestClient(this.url + (this.url.endsWith('/') ? '' : '/'))
        client.setHeaders(['Authorization': "Bearer ${getAccessToken()}"])
        client
    }

    static void setProxy(HTTPBuilder httpBuilder) {
        String proxyHost = System.properties.'http.proxyHost'
        if (proxyHost) {
            int proxyPort = System.properties.'http.proxyPort' as int
            httpBuilder.setProxy(proxyHost, proxyPort, 'http')

            String proxyUser = System.properties.'http.proxyUser'
            if (proxyUser) {
                String proxyPassword = System.properties.'http.proxyPassword'
                httpBuilder.client.getCredentialsProvider().setCredentials(
                        new AuthScope(proxyHost, proxyPort),
                        new UsernamePasswordCredentials(proxyUser, proxyPassword)
                )
            }
        }
    }

    protected static void setProxyAuthentication() {
        String proxyUser = System.properties.'http.proxyUser'
        if (proxyUser) {
            String proxyPassword = System.properties.'http.proxyPassword'
            Authenticator authenticator = new Authenticator() {
                public PasswordAuthentication getPasswordAuthentication() {
                    return (new PasswordAuthentication(proxyUser, proxyPassword.toCharArray()))
                }
            }
            Authenticator.setDefault(authenticator)
        }
    }

    /**
     * Retrieve all content sets.
     * @throws IOException
     */
    public Collection<ContentSet> getContentSets() throws IOException {
        RESTClient apiClient = createApiClient()
        List<ContentSet> contentSets = null
        try {
            def authResponse = apiClient.get(contentType: 'application/json', path: 'contentSet')
            int status = authResponse.status
            if (status == HttpStatus.SC_OK) {
                contentSets = authResponse.data?.collect { contentSetJson ->
                    new ContentSet(id: contentSetJson.id, name: contentSetJson.name, description: contentSetJson.description)
                }
            }
        } catch (HttpResponseException hre) {
            throw new IOException("Failed to retrieve content sets (status code ${hre.response.status})", hre)
        } catch (Exception ex) {
            throw new IOException("Failed to retrieve content sets", ex)
        }
        return contentSets
    }

    /**
     * Retrieve an individual content set.
     * @param contentSetId
     * @throws IOException
     */
    public ContentSet getContentSet(Long contentSetId) throws IOException {
        RESTClient apiClient = createApiClient()
        ContentSet contentSet = null
        try {
            def authResponse = apiClient.get(contentType: 'application/json', path: "contentSet/$contentSetId")
            int status = authResponse.status
            if (status == HttpStatus.SC_OK) {
                contentSet = new ContentSet(id: authResponse.data.id, name: authResponse.data.name, description: authResponse.data.description)
            }
        } catch (HttpResponseException hre) {
            throw new IOException("Failed to retrieve content set with id $contentSetId (status code ${hre.response.status})", hre)
        } catch (Exception ex) {
            throw new IOException("Failed to retrieve content set with id $contentSetId", ex)
        }
        return contentSet
    }

    /**
     * Retrieve a collection of all entitlements.
     * @throws IOException
     */
    public Collection<Entitlement> getEntitlements() throws IOException {
        RESTClient apiClient = createApiClient()
        List<Entitlement> entitlements = null
        try {
            def authResponse = apiClient.get(contentType: 'application/json', path: 'entitlement')
            int status = authResponse.status
            if (status == HttpStatus.SC_OK) {
                entitlements = authResponse.data?.collect { entitlementJson ->
                    new Entitlement(
                            id: entitlementJson.id,
                            beginDateInclusive: Date.parse('yyyy-MM-dd', entitlementJson.beginDateInclusive),
                            endDateInclusive: Date.parse('yyyy-MM-dd', entitlementJson.endDateInclusive),
                            contentSet: new ContentSet(id: entitlementJson.contentSet.id, name: entitlementJson.contentSet.name, description: entitlementJson.contentSet.description)
                    )
                }
            }
        } catch (HttpResponseException hre) {
            throw new IOException("Failed to retrieve entitlements (status code ${hre.response.status})", hre)
        } catch (Exception ex) {
            throw new IOException("Failed to retrieve entitlements", ex)
        }
        return entitlements
    }

    /**
     * Retrieve an individual entitlement.
     * @param entitlementId
     * @throws IOException
     */
    public Entitlement getEntitlement(Long entitlementId) throws IOException {
        RESTClient apiClient = createApiClient()
        Entitlement entitlement = null
        try {
            def apiResponse = apiClient.get(contentType: 'application/json', path: "entitlement/$entitlementId")
            int status = apiResponse.status
            if (status == HttpStatus.SC_OK) {
                entitlement = new Entitlement(
                        id: apiResponse.data.id,
                        beginDateInclusive: Date.parse('yyyy-MM-dd', apiResponse.data.beginDateInclusive),
                        endDateInclusive: Date.parse('yyyy-MM-dd', apiResponse.data.endDateInclusive),
                        contentSet: new ContentSet(id: apiResponse.data.contentSet.id, name: apiResponse.data.contentSet.name, description: apiResponse.data.contentSet.description)
                )
            }
        } catch (HttpResponseException hre) {
            throw new IOException("Failed to retrieve entitlement with id $entitlementId (status code ${hre.response.status})", hre)
        } catch (Exception ex) {
            throw new IOException("Failed to retrieve entitlement with id $entitlementId", ex)
        }
        return entitlement
    }

    /**
     * Fully consumes a single content set given an initial resumption token, directing content to the provided OutputStream.
     * @param contentSetId - The ID of the content set to consume
     * @param version - The version of the content set to consume
     * @param outStream - The OutputStream to which the RDF will be written
     * @param resumptionToken - The starting-point resumption token
     * @throws IOException
     */
    public ConsumeResponse consumeFully(String contentSetId, String version, String resumptionToken, OutputStream outStream) throws IOException {
        StatusKeeper statusKeeper = new StatusKeeper()
        Runtime.getRuntime().addShutdownHook(statusKeeper)
        ConsumeResponse consumeResponse = new ConsumeResponse(statusCode: HttpStatus.SC_OK, resumptionToken: resumptionToken)
        int retryCount = 10
        while (consumeResponse.statusCode != HttpStatus.SC_NO_CONTENT && retryCount > 0) {
            consumeResponse = consume(contentSetId, version, consumeResponse.resumptionToken, outStream)
            statusKeeper.lastResponse = consumeResponse
            statusKeeper.totalSize += consumeResponse.size
            statusKeeper.totalTime += consumeResponse.time
            statusKeeper.totalCalls++
            if (consumeResponse.statusCode != HttpStatus.SC_OK && consumeResponse.statusCode != HttpStatus.SC_NO_CONTENT) {
                log.warn "Got unexpected status code: ${consumeResponse.statusCode} -- retrying $retryCount time(s)"
                retryCount--
            } else if (consumeResponse.statusCode == HttpStatus.SC_OK) {
                retryCount = 10
            }
        }
        consumeResponse.size = statusKeeper.totalSize
        consumeResponse.time = statusKeeper.totalTime
        Runtime.getRuntime().removeShutdownHook(statusKeeper)
        return consumeResponse
    }

    /**
     * Consume a single chunk/page of a content set, directing content to the provided OutputStream.
     * @param contentSetId - The ID of the content set to consume
     * @param version - The version of the content set to consume
     * @param outStream - The OutputStream to which the RDF will be written
     * @param resumptionToken - The resumption token identifying the chunk to retrieve
     * @throws IOException
     */
    public ConsumeResponse consume(String contentSetId, String version, String resumptionToken, OutputStream outStream) throws IOException {
        def conn = createUrlConnection(this.url + "/contentSet/$contentSetId/consume?version=$version&resumptionToken=${resumptionToken ?: ''}")
        conn.setRequestProperty('Authorization', "Bearer ${getAccessToken()}")
        long startTime = System.currentTimeMillis()
        conn.connect()
        ConsumeResponse consumeResponse = new ConsumeResponse(statusCode: conn.responseCode)
        switch (consumeResponse.statusCode) {
            case HttpStatus.SC_OK:
                GZIPInputStream zippedStream = new GZIPInputStream(conn.inputStream)
                CountingOutputStream countStream = new CountingOutputStream(outStream)
                try {
                    countStream << zippedStream
                } catch (Exception ex) {
                    throw new IOException("Failed to read content from consume endpoint", ex)
                } finally {
                    zippedStream.close()
                }
                consumeResponse.size = countStream.count
                consumeResponse.time = (System.currentTimeMillis() - startTime)
            case HttpStatus.SC_NO_CONTENT:
                consumeResponse.resumptionToken = (conn.getHeaderField(HEADER_RESUMPTION_TOKEN) ?: resumptionToken)
                consumeResponse.remainingCount = (conn.getHeaderField(HEADER_REMAINING_COUNT) ?: "0").toInteger()
                consumeResponse.requestedVersion = conn.getHeaderField(HEADER_CONTENT_SET_VERSION)
                consumeResponse.latestVersion = conn.getHeaderField(HEADER_CONTENT_SET_LATEST_VERSION)
        }
        conn.disconnect()
        return consumeResponse
    }

    /**
     * Download the initial file of RDF content for the given content set.
     * @param contentSetId - The ID of the content set to download
     * @param outStream - The OutputStream to which the RDF will be written
     * @param version - Optional version number.  If not provided, the latest version of the content set will be retrieved.
     * @param encoding - Optional encoding.  If not provided, the default is Encoding.BZIP2
     * @throws IOException
     */
    public ConsumeResponse bulk(String contentSetId, OutputStream outStream, String version = null, Encoding encoding = Encoding.BZIP2) {
        def conn = createUrlConnection(this.url + "/contentSet/$contentSetId/bulk${version ? "?version=$version": ''}")
        conn.setRequestProperty('Authorization', "Bearer ${getAccessToken()}")
        conn.setRequestProperty(HEADER_CUSTOM_ACCEPT_ENCODING, encoding.type)
        long startTime = System.currentTimeMillis()
        conn.connect()
        ConsumeResponse consumeResponse = new ConsumeResponse(statusCode: conn.responseCode)
        if (consumeResponse.statusCode == HttpStatus.SC_OK) {
            CountingOutputStream countStream = new CountingOutputStream(outStream)
            try {
                countStream << conn.inputStream
            } catch (Exception ex) {
                throw new IOException("Failed to read content from bulk endpoint", ex)
            } finally {
                IOUtils.closeQuietly(countStream)
            }
            consumeResponse.resumptionToken = conn.getHeaderField(HEADER_RESUMPTION_TOKEN)
            consumeResponse.requestedVersion = conn.getHeaderField(HEADER_CONTENT_SET_VERSION)
            consumeResponse.latestVersion = conn.getHeaderField(HEADER_CONTENT_SET_LATEST_VERSION)
            consumeResponse.size = countStream.count
            consumeResponse.time = (System.currentTimeMillis() - startTime)
        }
        conn.disconnect()
        return consumeResponse
    }

    protected createUrlConnection(String path) {
        return new URL(path).openConnection()
    }

    public static void main(String[] args) {
        Environment env = Environment.PROD

        String clientId = args[1]
        String clientSecret = args[2]
        String contentSetId = (args.size() > 3 ? args[3] : null)

        if (args[0].equalsIgnoreCase('bulk')) {
            String version = (args.size() > 4 ? (args[4] ?: null) : null)
            env = (args.size() > 5 ? Environment.valueOf(args[5]) : env)
            GraphFeed graphFeed = new GraphFeed(env, clientId, clientSecret)
            log.info "Starting bulk download"
            ConsumeResponse consumeResponse = graphFeed.bulk(contentSetId, System.out, version)
//            ConsumeResponse consumeResponse = graphFeed.bulk(contentSetId, new NullOutputStream(), version)
            log.info "Finished bulk download:"
            log.info new JsonBuilder(consumeResponse).toPrettyString()
        } else if (args[0].equalsIgnoreCase('consume')) {
            String version = args[4]
            String resumptionToken = args[5]
            env = (args.size() > 6 ? Environment.valueOf(args[6]) : env)
            GraphFeed graphFeed = new GraphFeed(env, clientId, clientSecret)
            log.info "Starting consume (fully)"
            ConsumeResponse consumeResponse = graphFeed.consumeFully(contentSetId, version, resumptionToken, System.out)
            log.info "Finished consume:"
            log.info new JsonBuilder(consumeResponse).toPrettyString()
        } else {
            env = (args.size() > 4 ? Environment.valueOf(args[4]) : env)
            GraphFeed graphFeed = new GraphFeed(env, clientId, clientSecret)
            log.info "Content Sets:"
            log.info new JsonBuilder(graphFeed.getContentSets()).toPrettyString()
            log.info "Entitlements:"
            log.info new JsonBuilder(graphFeed.getEntitlements()).toPrettyString()
        }
    }
}
