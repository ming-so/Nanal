package com.android.nanal;

import android.os.AsyncTask;

import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class MailSender extends AsyncTask<String, Void, Void> {
    private String host = "smtp.gmail.com";
    final private String user = "nanalmanager@gmail.com";
    final private String password = "nanal7401";
    private Session session;

    public MailSender() {
        Properties properties = new Properties();
        properties.put("mail.smtp.host", "smtp.gmail.com");
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.port", 465);
        properties.put("mail.smtp.ssl.enable", "true");
        properties.put("mail.smtp.ssl.trust", "smtp.gmail.com");

        session = Session.getDefaultInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, password);
            }
        });
    }

    @Override
    protected Void doInBackground(String... strings) {
        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(user));

            message.addRecipient(Message.RecipientType.TO, new InternetAddress(strings[0]));
            message.setSubject("나날 인증 메일입니다.");
            message.setText("나날 인증 메일 내용입니다.");

            Transport.send(message);
            System.out.println("Message sent successfully !");
        } catch (MessagingException e) {
            e.printStackTrace();
        }
        return null;
    }


    public void sendMail(String recipient) {


    }
}
