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
import groovy.util.logging.Slf4j

@Slf4j
class StatusKeeper extends Thread {

    ConsumeResponse lastResponse
    long totalSize = 0
    long totalTime = 0
    long totalCalls = 0

    @Override
    public void run() {
        if (lastResponse) {
            log.info new JsonBuilder(lastResponse).toPrettyString()
            logTotals()
        }
    }

    public void logTotals() {
        log.info "Total size consumed: $totalSize, in $totalTime msecs across $totalCalls calls (${Math.floor(totalSize / (totalTime > 0 ? (totalTime / 1000) : 1))} bytes/sec, ${Math.floor(totalSize / (totalCalls > 0 ? totalCalls : 1))} bytes/call, ${Math.floor(totalTime / (totalCalls > 0 ? totalCalls : 1))} secs/call)"
    }
}
