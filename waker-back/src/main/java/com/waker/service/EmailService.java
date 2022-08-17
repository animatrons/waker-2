package com.waker.service;

import com.waker.model.Email;

public class EmailService {

    private static EmailService instance = null;
    private EmailService() {}
    public static EmailService getInstance() {
        return instance != null ? instance : (instance = new EmailService());
    }

    public static void sendEmail(Email email) {
        System.out.println("==========Sending email===========\n");
        System.out.printf("From %s\n", email.getFrom());
        System.out.printf("To %s\n", email.getTo());
        System.out.printf("Subject: %s\n", email.getSubject());
        System.out.println("Body:\n");
        System.out.printf(" %s\n", email.getBody());
        System.out.printf("Time: %s\n", email.getDate());
        System.out.println("==========Email sent===========\n");
    }
}
