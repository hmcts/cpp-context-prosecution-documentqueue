package uk.gov.moj.cpp.prosecution.documentqueue.command.api.accesscontrol;

import static java.util.Arrays.asList;

import java.util.List;

public final class RuleConstants {

    private static final String GROUP_COURT_ADMINISTRATORS = "Court Administrators";
    private static final String GROUP_LEGAL_ADVISERS = "Legal Advisers";
    private static final String GROUP_COURT_ASSOCIATE = "Court Associate";
    private static final String GROUP_CROWN_COURT_ADMIN = "Crown Court Admin";
    private static final String GROUP_LISTING_OFFICERS = "Listing Officers";
    private static final String GROUP_COURT_CLERKS = "Court Clerks";
    private static final String GROUP_SYSTEM_USERS = "System Users";

    private RuleConstants() {
    }

    public static List<String> getReviewDocumentsAccessGroups() {
        return asList(
                GROUP_LEGAL_ADVISERS,
                GROUP_COURT_ADMINISTRATORS,
                GROUP_COURT_ASSOCIATE,
                GROUP_CROWN_COURT_ADMIN,
                GROUP_LISTING_OFFICERS,
                GROUP_COURT_CLERKS);
    }

    public static List<String> getAccessGroupsForMarkCaseDocumentsDeleted() {
        return asList(GROUP_SYSTEM_USERS);
    }

    public static List<String> getAccessGroupsForDeleteDocumentsExpired() {
        return asList(GROUP_SYSTEM_USERS);
    }

}
