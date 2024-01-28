package com.sviatenterprise.service.impl;

import com.sviatenterprise.entity.AppDocument;
import com.sviatenterprise.entity.AppPhoto;
import com.sviatenterprise.repository.AppDocumentRepository;
import com.sviatenterprise.repository.AppPhotoRepository;
import com.sviatenterprise.service.FileService;
import com.sviatenterprise.utils.CryptoTool;
import lombok.extern.log4j.Log4j;
import org.springframework.stereotype.Service;

@Service
@Log4j
public class FileServiceImpl implements FileService {
    private final AppDocumentRepository appDocumentRepository;
    private final AppPhotoRepository appPhotoRepository;
    private final CryptoTool cryptoTool;

    public FileServiceImpl(AppDocumentRepository appDocumentRepository, AppPhotoRepository appPhotoRepository, CryptoTool cryptoTool) {
        this.appDocumentRepository = appDocumentRepository;
        this.appPhotoRepository = appPhotoRepository;
        this.cryptoTool = cryptoTool;
    }

    @Override
    public AppDocument getDocument(String hash) {
        var id = cryptoTool.idOf(hash);

        if (id == null) {
            return null;
        }

        return appDocumentRepository.findById(id).orElse(null);
    }

    @Override
    public AppPhoto getPhoto(String hash) {
        var id = cryptoTool.idOf(hash);

        if (id == null) {
            return null;
        }

        return appPhotoRepository.findById(id).orElse(null);
    }
}
