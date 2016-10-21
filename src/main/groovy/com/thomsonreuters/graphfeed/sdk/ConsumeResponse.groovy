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
