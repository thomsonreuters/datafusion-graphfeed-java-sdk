package com.thomsonreuters.graphfeed.sdk

enum Encoding {

    GZIP("gzip", ".gz"),
    BZIP2("bzip2", ".bz2")

    public final String type
    public final String extension

    public Encoding(String type, String extension) {
        this.type = type
        this.extension = extension
    }
}
