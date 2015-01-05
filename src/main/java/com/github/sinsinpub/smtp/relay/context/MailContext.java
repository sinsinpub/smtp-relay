package com.github.sinsinpub.smtp.relay.context;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
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
    private transient byte[] messageData;
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

    /**
     * New <code>MimeMessage</code> instance by message binary stream data.
     * 
     * @param session
     * @return new MimeMessage instance
     * @throws MessagingException
     */
    public MimeMessage getMimeMessage(Session session)
            throws MessagingException {
        resetStream();
        return new MimeMessage(session, getStream());
    }

    public byte[] getMessageData() {
        return messageData;
    }

    public void setMessageData(byte[] messageData) {
        this.messageData = messageData;
        this.stream = new ByteArrayInputStream(messageData);
    }

    public String getEnvelopeSender() {
        return envelopeSender;
    }

    public void setEnvelopeSender(String envelopeSender) {
        this.envelopeSender = envelopeSender;

    }

    public String getEnvelopeReceiver() {
        return envelopeReceiver;
    }

    public void setEnvelopeReceiver(String envelopeReceiver) {
        this.envelopeReceiver = envelopeReceiver;
    }

    public InputStream getStream() {
        return stream;
    }

    public void resetStream() {
        stream.reset();
    }

}
