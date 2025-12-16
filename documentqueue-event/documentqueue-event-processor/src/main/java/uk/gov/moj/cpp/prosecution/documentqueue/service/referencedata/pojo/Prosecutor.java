package uk.gov.moj.cpp.prosecution.documentqueue.service.referencedata.pojo;

import java.io.Serializable;

public class Prosecutor implements Serializable {

    private String shortName;

    private String fullName;


    public Prosecutor(final String shortName, final String fullName) {
        this.shortName = shortName;
        this.fullName = fullName;
    }

    public String getShortName() {
        return shortName;
    }

    public String getFullName() {
        return fullName;
    }
}