package com.github.sinsinpub.smtp.relay.context;

import java.io.ByteArrayInputStream;
import java.io.Serializable;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

/**
 * The context container of a e-mail message.
 */
public class MailContext implements Serializable {

    /** serialVersionUID */
    private static final long serialVersionUID = 1L;
    private byte[] messageData;
    private String envelopeSender;
    private String envelopeReceiver;
    private ByteArrayInputStream stream;

    public MailContext() {
    }

    public MailContext(String envelopeSender, String envelopeReceiver,
            byte[] messageData) {
        this.envelopeSender = envelopeSender;
        this.envelopeReceiver = envelopeReceiver;
        this.messageData = messageData;
        this.stream = new ByteArrayInputStream(messageData);
    }

    public MimeMessage getMimeMessage(Session session)
            throws MessagingException {
        resetStream();
        return new MimeMessage(session, stream);
    }

    public byte[] getMessageData() {
        return messageData;
    }

    public String getEnvelopeSender() {
        return envelopeSender;
    }

    public String getEnvelopeReceiver() {
        return envelopeReceiver;
    }

    public void resetStream() {
        stream.reset();
    }

}
