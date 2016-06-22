package com.thomsonreuters.graphfeed.sdk

/**
 * A DTO holding information about a call to the GraphFeed consume endpoint.
 */
class ConsumeResponse {
    String resumptionToken
    Integer remainingCount
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
        return "status: $statusCode, resumptionToken: $resumptionToken, remainingCount: $remainingCount"
    }
}
