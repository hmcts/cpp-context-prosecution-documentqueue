package uk.gov.moj.cpp.prosecution.documentqueue.it.helper.file;

import static java.lang.String.format;

import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.fileservice.utils.test.FileServiceTestClient;
import uk.gov.justice.services.test.utils.core.files.ClasspathFileResource;
import uk.gov.justice.services.test.utils.persistence.TestJdbcDataSourceProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

public class FileServiceInserter {

    private final ClasspathFileResource classpathFileResource = new ClasspathFileResource();
    private final FileServiceTestClient fileServiceTestClient = new FileServiceTestClient();
    private final TestJdbcDataSourceProvider testJdbcDataSourceProvider = new TestJdbcDataSourceProvider();

    public void addPdf(final UUID fileId, final String pathOnClasspath, final String fileName) throws FileServiceException {

        final File fileFromClasspath = classpathFileResource.getFileFromClasspath(pathOnClasspath + "/" + fileName);

        try (
                final InputStream pdfInputStream = new FileInputStream(fileFromClasspath);
                final Connection fileServiceConnection = testJdbcDataSourceProvider.getFileServiceDataSource().getConnection()) {

            fileServiceTestClient.create(fileId, fileName, "application/pdf", pdfInputStream, fileServiceConnection);

        } catch (final SQLException | IOException e) {
            throw new FileServiceException(format("Failed to insert file %s/%s into the file service database", pathOnClasspath, fileName), e);
        }
    }

    public void clean() throws FileServiceException {

        try (final Connection fileServiceConnection = testJdbcDataSourceProvider.getFileServiceDataSource().getConnection();
             final PreparedStatement contentPreparedStatement = fileServiceConnection.prepareStatement("TRUNCATE content CASCADE");
             final PreparedStatement metadataPreparedStatement = fileServiceConnection.prepareStatement("TRUNCATE metadata CASCADE")) {

            contentPreparedStatement.execute();
            metadataPreparedStatement.execute();

        } catch (final SQLException e) {
            throw new FileServiceException("Failed to truncate content/metadata tables in file service database", e);
        }
    }
}
