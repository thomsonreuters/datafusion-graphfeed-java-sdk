package com.thomsonreuters.graphfeed.sdk

import groovyx.net.http.HTTPBuilder
import groovyx.net.http.HttpResponseException
import groovyx.net.http.RESTClient
import org.apache.http.HttpStatus
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials

import java.util.zip.GZIPInputStream

public class GraphFeed {

    protected static final String HEADER_RESUMPTION_TOKEN = 'X-DFGF-RESUMPTION-TOKEN'
    protected static final String HEADER_REMAINING_COUNT = 'X-DFGF-REMAINING-COUNT'

    protected String url
    protected String authUrl
    protected String clientId
    protected String clientSecret
    protected ApiToken apiToken

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
            RESTClient restClient = createRestClient(this.authUrl)
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

    protected static RESTClient createRestClient(String defaultUri) {
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
     * Fully consumes a single content set, directing content to the provided OutputStream.
     * @param contentSetId - The ID of the content set to consume
     * @param outStream - The OutputStream to which the RDF will be written
     * @param resumptionToken - Optional resumption token (leave null or pass in empty string to start consuming the content set from the beginning)
     * @throws IOException
     */
    public ConsumeResponse consumeFully(String contentSetId, OutputStream outStream, String resumptionToken = null) throws IOException {
        ConsumeResponse consumeResponse = new ConsumeResponse(statusCode: HttpStatus.SC_OK, resumptionToken: resumptionToken)
        int retryCount = 10
        while (consumeResponse.statusCode != HttpStatus.SC_NO_CONTENT && retryCount > 0) {
            consumeResponse = consume(contentSetId, outStream, consumeResponse.resumptionToken)
            if (consumeResponse.statusCode != HttpStatus.SC_OK && consumeResponse.statusCode != HttpStatus.SC_NO_CONTENT) {
                System.err.println "Got unexpected status code: ${consumeResponse.statusCode} -- retrying $retryCount time(s)"
                retryCount--
            } else if (consumeResponse.statusCode == HttpStatus.SC_OK) {
                retryCount = 10
            }
        }
        return consumeResponse
    }

    /**
     * Consume a single chunk/page of a content set, directing content to the provided OutputStream.
     * @param contentSetId - The ID of the content set to consume
     * @param outStream - The OutputStream to which the RDF will be written
     * @param resumptionToken - Optional resumption token (leave null or pass in empty string to consume the first chunk/page of the content set)
     * @throws IOException
     */
    public ConsumeResponse consume(String contentSetId, OutputStream outStream, String resumptionToken = null) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(this.url + "/contentSet/$contentSetId/consume?resumptionToken=${resumptionToken ?: ''}").openConnection()
        conn.setRequestProperty('Authorization', "Bearer ${getAccessToken()}")
        conn.connect()
        ConsumeResponse consumeResponse = new ConsumeResponse(statusCode: conn.responseCode)
        switch(consumeResponse.statusCode) {
            case HttpStatus.SC_OK:
                GZIPInputStream zippedStream = new GZIPInputStream(conn.inputStream)
                try {
                    outStream << zippedStream
                } catch (Exception ex) {
                    throw new IOException("Failed to read content from consume endpoint", ex)
                } finally {
                    zippedStream.close()
                }
            case HttpStatus.SC_NO_CONTENT:
                consumeResponse.resumptionToken = (conn.getHeaderField(HEADER_RESUMPTION_TOKEN) ?: resumptionToken)
                consumeResponse.remainingCount = (conn.getHeaderField(HEADER_REMAINING_COUNT) ?: "0").toInteger()
        }
        conn.disconnect()
        return consumeResponse
    }

    public static void main(String[] args) {
        String url = args[0]
        String authUrl = args[1]
        String clientId = args[2]
        String clientSecret = args[3]
        String contentSetId = (args.size() > 4 ? args[4] : null)
        String resumptionToken = (args.size() > 5 ? args[5] : null)

        GraphFeed graphFeed = new GraphFeed(url, authUrl, clientId, clientSecret)

        if (contentSetId) {
            ConsumeResponse consumeResponse = graphFeed.consumeFully(contentSetId, System.out, resumptionToken)
            System.err.println "Finished with $consumeResponse"
        } else {
            println graphFeed.getContentSets()
            println graphFeed.getContentSet(1)
            println graphFeed.getEntitlements()
            println graphFeed.getEntitlement(2)
        }
    }
}