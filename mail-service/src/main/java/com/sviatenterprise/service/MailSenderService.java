package com.sviatenterprise.service;

import com.sviatenterprise.dto.MailParams;

public interface MailSenderService {
    void send(MailParams mailParams);
}
