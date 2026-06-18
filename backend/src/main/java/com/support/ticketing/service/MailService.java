package com.support.ticketing.service;

public interface MailService {
    void sendEmail(String to, String subject, String body);
}
