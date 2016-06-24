package com.thomsonreuters.graphfeed.sdk

import groovy.json.JsonBuilder

public class ContentSet {
    Long id
    String name
    String description

    public String toString() {
        return new JsonBuilder(this).toPrettyString()
    }
}
