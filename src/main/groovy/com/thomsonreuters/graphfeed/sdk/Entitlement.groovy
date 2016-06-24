package com.thomsonreuters.graphfeed.sdk

import groovy.json.JsonBuilder

class Entitlement {
    Long id
    Date beginDateInclusive
    Date endDateInclusive
    ContentSet contentSet

    public String toString() {
        return new JsonBuilder(this).toPrettyString()
    }
}
