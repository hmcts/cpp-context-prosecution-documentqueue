package uk.gov.moj.cpp.prosecution.documentqueue.service.material.pojo;

import java.time.ZonedDateTime;
import java.util.UUID;

public class MaterialMetadata {

    private final UUID materialId;
    private final String alfrescoAssetId;
    private final String fileName;
    private final String mimeType;
    private final String externalLink;
    private ZonedDateTime materialAddedDate;

    public MaterialMetadata(final UUID materialId, final String alfrescoAssetId, final String fileName, final String mimeType, final String externalLink, final ZonedDateTime materialAddedDate) {
        this.materialId = materialId;
        this.alfrescoAssetId = alfrescoAssetId;
        this.fileName = fileName;
        this.mimeType = mimeType;
        this.externalLink = externalLink;
        this.materialAddedDate = materialAddedDate;
    }

    public UUID getMaterialId() {
        return materialId;
    }

    public String getAlfrescoAssetId() {
        return alfrescoAssetId;
    }

    public String getFileName() {
        return fileName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public ZonedDateTime getMaterialAddedDate() {
        return materialAddedDate;
    }

    public String getExternalLink() {
        return externalLink;
    }
}
