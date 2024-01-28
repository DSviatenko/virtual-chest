package com.sviatenterprise.service;

import com.sviatenterprise.entity.AppDocument;
import com.sviatenterprise.entity.AppPhoto;
import org.telegram.telegrambots.meta.api.objects.Message;

public interface FileService {
    AppDocument processDoc(Message telegramMessage);

    AppPhoto processPhoto(Message telegramMessage);

    String generateLink(Long id, LinkType type);
}
