package com.sviatenterprise.service.impl;

import com.sviatenterprise.entity.AppDocument;
import com.sviatenterprise.entity.AppPhoto;
import com.sviatenterprise.entity.AppUser;
import com.sviatenterprise.entity.RawData;
import com.sviatenterprise.exception.UploadFileException;
import com.sviatenterprise.repository.AppUserRepository;
import com.sviatenterprise.repository.RawDataRepository;
import com.sviatenterprise.service.AppUserService;
import com.sviatenterprise.service.FileService;
import com.sviatenterprise.service.LinkType;
import com.sviatenterprise.service.MainService;
import com.sviatenterprise.service.ProducerService;
import com.sviatenterprise.service.ServiceCommand;
import lombok.extern.log4j.Log4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import static com.sviatenterprise.entity.UserState.BASIC_STATE;
import static com.sviatenterprise.entity.UserState.WAIT_FOR_EMAIL;
import static com.sviatenterprise.service.ServiceCommand.CANCEL;
import static com.sviatenterprise.service.ServiceCommand.HELP;
import static com.sviatenterprise.service.ServiceCommand.REGISTRATION;
import static com.sviatenterprise.service.ServiceCommand.START;

@Service
@Log4j
public class MainServiceImpl implements MainService {
    private final RawDataRepository rawDataRepository;
    private final ProducerService producerService;
    private final AppUserRepository appUserRepository;
    private final FileService fileService;
    private final AppUserService appUserService;

    public MainServiceImpl(RawDataRepository rawDataRepository, ProducerService producerService, AppUserRepository appUserRepository, FileService fileService, AppUserService appUserService) {
        this.rawDataRepository = rawDataRepository;
        this.producerService = producerService;
        this.appUserRepository = appUserRepository;
        this.fileService = fileService;
        this.appUserService = appUserService;
    }

    @Override
    public void processTextMessage(Update update) {
        saveRawData(update);

        var appUser = findOrSaveAppUser(update);
        var userState = appUser.getState();
        var text = update.getMessage().getText();
        var output = "";

        var serviceCommand = ServiceCommand.fromValue(text);
        if (CANCEL.equals(serviceCommand)) {
            output = cancelProcess(appUser);
        } else if (BASIC_STATE.equals(userState)) {
            output = processServiceCommand(appUser, text);
        } else if (WAIT_FOR_EMAIL.equals(userState)) {
            output = appUserService.setEmail(appUser, text);
        } else {
            log.error("Unknown user state: " + userState);
            output = "Невідома помилка! Введіть /cancel та спробуйте знову!";
        }

        var chatId = update.getMessage().getChatId();
        sendAnswer(output, chatId);

    }

    @Override
    public void processDocMessage(Update update) {
        saveRawData(update);

        var appUser = findOrSaveAppUser(update);
        var chatId = update.getMessage().getChatId();

        if (isNotAllowToSendContent(chatId, appUser)) {
            return;
        }

        try {
            AppDocument doc = fileService.processDoc(update.getMessage());
            var link = fileService.generateLink(doc.getId(), LinkType.GET_DOC);
            var answer = "Документ успішно завантажено! Посилання на завантаження: " + link;
            sendAnswer(answer, chatId);
        } catch (UploadFileException e) {
            log.error(e);
            String error = "Нажаль, не вдалось завантажити файл. Повторіть спробу пізніше.";
            sendAnswer(error, chatId);
        }
    }

    private boolean isNotAllowToSendContent(Long chatId, AppUser appUser) {
        var userState = appUser.getState();
        if (!appUser.getIsActive()) {
            var error = "Зареєструйте свій обліковий запис для завантаження контенту.";
            sendAnswer(error, chatId);
            return true;
        } else if (!BASIC_STATE.equals(userState)) {
            var error = "Відмініть поточну команду за допомогою /cancel для відправки файлів";
            sendAnswer(error, chatId);
            return true;
        }
        return false;
    }

    @Override
    public void processPhotoMessage(Update update) {
        saveRawData(update);

        var appUser = findOrSaveAppUser(update);
        var chatId = update.getMessage().getChatId();

        if (isNotAllowToSendContent(chatId, appUser)) {
            return;
        }

        try {
            AppPhoto photo = fileService.processPhoto(update.getMessage());
            var link = fileService.generateLink(photo.getId(), LinkType.GET_PHOTO);
            var answer = "Фото успішно завантажено! Посилання на завантаження: " + link;
            sendAnswer(answer, chatId);
        } catch (UploadFileException e) {
            log.error(e);
            String error = "Нажаль, не вдалось завантажити фото. Повторіть спробу пізніше.";
            sendAnswer(error, chatId);
        }
    }

    private void sendAnswer(String output, Long chatId) {
        var sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(output);
        producerService.producerAnswer(sendMessage);
    }

    private String processServiceCommand(AppUser appUser, String cmd) {
        var serviceCommand = ServiceCommand.fromValue(cmd);
        if (REGISTRATION.equals(serviceCommand)) {
            return appUserService.registerUser(appUser);
        } else if (HELP.equals(serviceCommand)) {
            return help();
        } else if (START.equals(serviceCommand)) {
            return "Вітаю! Щоб переглянути список доступних команд введіть /help";
        } else {
            return "Невідома команда! Щоб переглянути список доступних команд введіть /help";
        }
    }

    private String help() {
        return "Cписок доступних команд:\n"
                + "/cancel - відміна виконання поточної команди;\n"
                + "/registration - реєстрація користувача.";
    }

    private String cancelProcess(AppUser appUser) {
        appUser.setState(BASIC_STATE);
        appUserRepository.save(appUser);
        return "Команду скасовано!";
    }

    private AppUser findOrSaveAppUser(Update update) {
        var telegramUser = update.getMessage().getFrom();

        var optional = appUserRepository.findByTelegramUserId(telegramUser.getId());

        if (optional.isEmpty()) {
            AppUser transientAppUser = AppUser.builder()
                    .telegramUserId(telegramUser.getId())
                    .username(telegramUser.getUserName())
                    .firstName(telegramUser.getFirstName())
                    .lastName(telegramUser.getLastName())
                    .isActive(false)
                    .state(BASIC_STATE)
                    .build();
            return appUserRepository.save(transientAppUser);
        }

        return optional.get();
    }

    private void saveRawData(Update update) {
        RawData rawData = RawData.builder()
                .event(update)
                .build();
        rawDataRepository.save(rawData);
    }
}
