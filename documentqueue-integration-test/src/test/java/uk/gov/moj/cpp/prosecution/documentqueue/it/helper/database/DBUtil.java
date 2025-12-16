package uk.gov.moj.cpp.prosecution.documentqueue.it.helper.database;

import static java.lang.String.format;
import static java.time.Duration.ofSeconds;

import uk.gov.justice.services.jdbc.persistence.DataAccessException;
import uk.gov.justice.services.test.utils.common.host.TestHostProvider;
import uk.gov.justice.services.test.utils.persistence.DatabaseCleaner;
import uk.gov.justice.services.test.utils.persistence.TestJdbcConnectionProvider;
import uk.gov.moj.cpp.prosecution.documentqueue.it.helper.DocumentQueueTableList;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import org.awaitility.Awaitility;
import org.awaitility.Durations;

public class DBUtil {

    private static final TestJdbcConnectionProvider CONNECTION_PROVIDER = new TestJdbcConnectionProvider();
    private static final DatabaseCleaner DATABASE_CLEANER = new DatabaseCleaner();

    private static final String CONTEXT_NAME = "documentqueue";
    private static final String COUNT_QRY = "SELECT count(1) FROM %s WHERE %s";
    private static final String COUNT_QRY_WITHOUT_CRITERIA = "SELECT count(1) FROM %s";
    private static final int EMPTY_COUNT = 0;

    public static int getCount(String table, String criteria) {

        try (Connection documentQueueViewStoreConnection = CONNECTION_PROVIDER.getViewStoreConnection(CONTEXT_NAME);
             Statement statement = documentQueueViewStoreConnection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(format(COUNT_QRY, table, criteria));
            while (resultSet.next()) {
                return resultSet.getInt(1);
            }
        } catch (SQLException exception) {
            throw new RuntimeException(format("SQLException while getting count from table %s with condition %s", table, criteria), exception);

        }

        return 0;
    }

    public static int getCount(String table) {

        try (Connection documentQueueViewStoreConnection = CONNECTION_PROVIDER.getViewStoreConnection(CONTEXT_NAME);
             Statement statement = documentQueueViewStoreConnection.createStatement()) {
            ResultSet resultSet = statement.executeQuery(format(COUNT_QRY_WITHOUT_CRITERIA, table));
            while (resultSet.next()) {
                return resultSet.getInt(1);
            }
        } catch (SQLException exception) {
            throw new RuntimeException(format("SQLException while getting count from table %s", table), exception);
        }

        return 0;
    }

    public static int checkIfTableIsEmpty(String tableName) {
        try (Connection documentQueueViewStoreConnection = CONNECTION_PROVIDER.getViewStoreConnection(CONTEXT_NAME);
             Statement statement = documentQueueViewStoreConnection.createStatement()) {
            ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM " + tableName);

            while (resultSet.next()) {
                return resultSet.getInt(1);
            }
        } catch (SQLException exception) {
            throw new RuntimeException(format("SQLException while getting count from table ", tableName), exception);
        }

        return 0;
    }


    public  static void waitUntilTheDocumentCountIsMatched(final UUID fileStoreId, final boolean deleted) {
        Awaitility.await()
                .pollDelay(ofSeconds(0))
                .pollInterval(Durations.FIVE_SECONDS)
                .until(() -> checkIfFileInFileStoreIsDeleted(fileStoreId) == deleted);
    }

    public static boolean checkIfFileInFileStoreIsDeleted(final UUID fileStoreId) {
        try (Connection documentQueueViewStoreConnection = getFileServiceConnection();
             PreparedStatement statement = documentQueueViewStoreConnection.prepareStatement("select deleted from content WHERE file_id = ?")) {
            statement.setObject(1, fileStoreId);
            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                return resultSet.getBoolean(1);
            }
        } catch (SQLException exception) {
            throw new RuntimeException(format("Exception while checking if a file is deleted %s", fileStoreId), exception);
        }

        return false;
    }

    public static Connection getFileServiceConnection() {
        String host = TestHostProvider.getHost();
        String url = "jdbc:postgresql://" + host + "/fileservice";
        String username = "fileservice";
        String password = "fileservice";

        try {
            return DriverManager.getConnection(url, username, password);
        } catch (SQLException var8) {
            throw new DataAccessException("Exception while getting the file service connection", var8);
        }
    }

    public static void waitUntilDataDeleted(String table, int count) {
        Awaitility.await()
                .pollDelay(ofSeconds(0))
                .pollInterval(Durations.TWO_HUNDRED_MILLISECONDS)
                .until(() -> checkIfTableIsEmpty(table) == count);
    }

    public static void waitUntilDataPersist(String table, String criteria, int count) {
        Awaitility.await()
                .pollDelay(Durations.ONE_MILLISECOND)
                .pollInterval(Durations.TWO_HUNDRED_MILLISECONDS)
                .until(() -> getCount(table, criteria) == count);
    }

    public static void waitUntilDataPersist(String table, int count) {
        Awaitility.await()
                .pollDelay(ofSeconds(0))
                .pollInterval(Durations.TWO_HUNDRED_MILLISECONDS)
                .until(() -> getCount(table) == count);
    }

    public static void cleanDatabase() {
        for (DocumentQueueTableList documentQueueTableList : DocumentQueueTableList.values()) {
            DATABASE_CLEANER.cleanViewStoreTables(CONTEXT_NAME, documentQueueTableList.name());
            waitUntilDataDeleted(documentQueueTableList.name(), EMPTY_COUNT);
        }
    }

}
