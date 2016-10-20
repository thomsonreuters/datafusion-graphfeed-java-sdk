package com.thomsonreuters.graphfeed.sdk

enum Environment {
    LOCAL, DEV, QA, PROD

    private Object config

    public getConfig() {
        if (!config) {
            URL url = Environment.class.getClassLoader().getResource("config.groovy")
            config = new ConfigSlurper(toString()).parse(url)
        }
        return config
    }
}
