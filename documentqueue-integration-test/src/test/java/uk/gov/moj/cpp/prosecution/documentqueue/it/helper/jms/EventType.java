package uk.gov.moj.cpp.prosecution.documentqueue.it.helper.jms;

public enum EventType {

    DOCUMENTQUEUE_EVENT("documentqueue.event"),
    USERSGROUPS_EVENT("usersgroups.event"),
    PUBLIC_EVENT("public.event");
    
    private String name;

    EventType(final String name) {
        this.name = name;
    }

    public String getStringValue() {
        return name;
    }
}
