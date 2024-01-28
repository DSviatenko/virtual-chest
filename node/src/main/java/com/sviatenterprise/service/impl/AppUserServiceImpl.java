package com.sviatenterprise.service.impl;

import com.sviatenterprise.dto.MailParams;
import com.sviatenterprise.entity.AppUser;
import com.sviatenterprise.repository.AppUserRepository;
import com.sviatenterprise.service.AppUserService;
import com.sviatenterprise.utils.CryptoTool;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import lombok.extern.log4j.Log4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import static com.sviatenterprise.entity.UserState.BASIC_STATE;
import static com.sviatenterprise.entity.UserState.WAIT_FOR_EMAIL;

@Service
@Log4j
public class AppUserServiceImpl implements AppUserService {
    private final AppUserRepository appUserRepository;

    private final CryptoTool cryptoTool;

    @Value("${service.mail.url}")
    private String mailServiceUrl;

    public AppUserServiceImpl(AppUserRepository appUserRepository, CryptoTool cryptoTool) {
        this.appUserRepository = appUserRepository;
        this.cryptoTool = cryptoTool;
    }

    @Override
    public String registerUser(AppUser appUser) {
        if (appUser.getIsActive()) {
            return "Ви вже зареєстровані!";
        } else if (appUser.getEmail() != null) {
            return "Вам на пошту надіслано лист. "
                    + "Перейдіть за посиланням для підтвердження реєстрації.";
        }

        appUser.setState(WAIT_FOR_EMAIL);
        appUserRepository.save(appUser);

        return "Введіть адресу електронної пошти: ";
    }

    @Override
    public String setEmail(AppUser appUser, String email) {
        try {
            InternetAddress emailAddr = new InternetAddress(email);
            emailAddr.validate();
        } catch (AddressException e) {
            return "Введіть, будь ласка, корректну адресу електронної пошти. Для відміни команди введіть /cancel";
        }

        var optional = appUserRepository.findByEmail(email);
        if (optional.isEmpty()) {
            appUser.setEmail(email);
            appUser.setState(BASIC_STATE);
            appUserRepository.save(appUser);

            var cryptoUserId = cryptoTool.hashOf(appUser.getId());
            var response = sendRequestToMailService(cryptoUserId, email);
            if (response.getStatusCode() != HttpStatus.OK) {
                var message = String.format("Виникила помилка під час надсилання листа на електронну пошту %s.", email);
                log.error(message);
                appUser.setEmail(null);
                appUserRepository.save(appUser);
                return message;
            }

            return "Вам на пошту надіслано лист. "
                    + "Перейдіть за посиланням для підтвердження реєстрації.";
        }
        return "Ця електронна пошта вже використовується. "
                + "Введіть /cancel, щоб відмінити команду.";
    }

    private ResponseEntity<String> sendRequestToMailService(String cryptoUserId, String email) {
        var restTemplate = new RestTemplate();
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        var mailParams = MailParams.builder()
                .id(cryptoUserId)
                .emailTo(email)
                .build();
        var request = new HttpEntity<>(mailParams, headers);
        return restTemplate.exchange(mailServiceUrl,
                HttpMethod.POST,
                request,
                String.class);
    }
}
