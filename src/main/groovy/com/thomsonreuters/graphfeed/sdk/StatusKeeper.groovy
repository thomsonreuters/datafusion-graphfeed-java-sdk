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
