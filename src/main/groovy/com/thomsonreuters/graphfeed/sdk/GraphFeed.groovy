package com.thomsonreuters.graphfeed.sdk

import org.apache.http.HttpStatus
import java.util.zip.GZIPInputStream

public class GraphFeed {

    protected String url

    public GraphFeed(String url) {
        this.url = url
    }

    public int consume(String contentSetId, String authToken, OutputStream outStream, String resumptionToken = null) {
        Integer status = HttpStatus.SC_OK
        int retryCount = 10
        while (status != HttpStatus.SC_NO_CONTENT && retryCount > 0) {
            (status, resumptionToken) = consumeOneChunk(contentSetId, authToken, outStream, resumptionToken)
            if (status != HttpStatus.SC_OK && status != HttpStatus.SC_NO_CONTENT) {
                System.err.println "Got unexpected status code: $status -- retrying $retryCount time(s)"
                retryCount--
            } else if (status == HttpStatus.SC_OK) {
                retryCount = 10
            }
        }
        return status
    }

    public List consumeOneChunk(String contentSetId, String authToken, OutputStream outStream, String resumptionToken = null) {
        HttpURLConnection conn = (HttpURLConnection) new URL(this.url + "/contentSet/$contentSetId/consume?resumptionToken=${resumptionToken ?: ''}").openConnection()
        conn.setRequestProperty('Authorization', "Bearer ${authToken}")
        conn.connect()
        Integer status = conn.responseCode
        if (status == HttpStatus.SC_OK) {
            GZIPInputStream zippedStream = new GZIPInputStream(conn.inputStream)
            try {
                outStream << zippedStream
            } catch (Exception ex) {
                System.err.println ex
            } finally {
                zippedStream.close()
            }
            resumptionToken = conn.getHeaderField("X-DFGF-RESUMPTION-TOKEN")
        }
        conn.disconnect()
        return [status, resumptionToken]
    }

    public static void main(String[] args) {
        String url = args[0]
        String contentSetId = args[1]
        String authToken = args[2]
        String resumptionToken = (args.size() > 3 ? args[3] : '')

        GraphFeed graphFeed = new GraphFeed(url)
        //TODO: make this consume the full set
        graphFeed.consumeOneChunk(contentSetId, authToken, System.out, resumptionToken)
    }
}