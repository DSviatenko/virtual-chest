package com.sviatenterprise.service.impl;

import com.sviatenterprise.service.ConsumerService;
import com.sviatenterprise.service.MainService;
import lombok.extern.log4j.Log4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;

import static com.sviatenterprise.RabbitQueue.DOC_MESSAGE_UPDATE;
import static com.sviatenterprise.RabbitQueue.PHOTO_MESSAGE_UPDATE;
import static com.sviatenterprise.RabbitQueue.TEXT_MESSAGE_UPDATE;

@Service
@Log4j
public class ConsumerServiceImpl implements ConsumerService {
    private final MainService mainService;

    public ConsumerServiceImpl(MainService mainService) {
        this.mainService = mainService;
    }

    @Override
    @RabbitListener(queues = TEXT_MESSAGE_UPDATE)
    public void consumeTextMessageUpdates(Update update) {
        log.debug("NODE: text message is received");

        mainService.processTextMessage(update);
    }

    @Override
    @RabbitListener(queues = DOC_MESSAGE_UPDATE)
    public void consumeDocMessageUpdates(Update update) {
        log.debug("NODE: doc message is received");

        mainService.processDocMessage(update);
    }

    @Override
    @RabbitListener(queues = PHOTO_MESSAGE_UPDATE)
    public void consumePhotoMessageUpdates(Update update) {
        log.debug("NODE: photo message is received");
        mainService.processPhotoMessage(update);
    }
}
