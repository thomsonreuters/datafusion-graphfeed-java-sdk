package com.thomsonreuters.graphfeed.sdk

import groovy.time.TimeCategory

class ApiToken {
    String accessToken
    Date issuedAt

    public boolean isExpired() {
        use (TimeCategory) {
            if (issuedAt.after(new Date() - 50.minute)) {
                return true
            }
        }
        return false
    }
}
