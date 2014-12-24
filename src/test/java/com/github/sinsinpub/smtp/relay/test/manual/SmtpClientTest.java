package com.github.sinsinpub.smtp.relay.test.manual;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class SmtpClientTest {

    public static void main(String[] args) {

        Properties props = new Properties();
        props.put("mail.smtp.host", "127.0.0.1");
        props.put("mail.smtp.port", "2025");
        props.put("mail.smtp.auth", "false");

        final String from = "sender@domain.net";
        final String to = "receiver@domain.net";

        try {
            Session session = Session.getInstance(props);

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse(to));
            message.setSubject("Testing Smtp Relay");
            message.setText("Dear Mail Crawler,"
                    + "\n\n  This is just a test mail, no spam plz!");

            Transport.send(message);

            System.out.println("Done");
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

}
