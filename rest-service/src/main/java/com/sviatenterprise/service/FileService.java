package com.sviatenterprise.service;

import com.sviatenterprise.entity.AppDocument;
import com.sviatenterprise.entity.AppPhoto;
import com.sviatenterprise.entity.BinaryContent;
import org.springframework.core.io.FileSystemResource;

public interface FileService {
    AppDocument getDocument(String id);

    AppPhoto getPhoto(String id);
}
