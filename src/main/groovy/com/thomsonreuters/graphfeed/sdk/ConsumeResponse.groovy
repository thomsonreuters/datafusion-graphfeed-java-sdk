package com.thomsonreuters.graphfeed.sdk

import groovy.json.JsonBuilder

/**
 * A DTO holding information about a call to the GraphFeed consume endpoint.
 */
class ConsumeResponse {
    String resumptionToken
    String requestedVersion
    String latestVersion
    Integer remainingCount
    int size
    long time
    private int statusCode

    /**
     *
     * @return 200 when RDF has been retrieved, 204 when no more RDF is available
     */
    int getStatusCode() {
        return statusCode
    }

    void setStatusCode(int statusCode) {
        this.statusCode = statusCode
    }

    public String toString() {
        return new JsonBuilder(this).toPrettyString()
    }
}
