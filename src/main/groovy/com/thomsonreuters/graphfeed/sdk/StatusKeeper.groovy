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