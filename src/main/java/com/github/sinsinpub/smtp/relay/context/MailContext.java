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
    private transient ByteArrayInputStream stream;

    public MailContext() {
    }

    public MailContext(String envelopeSender, String envelopeReceiver,
            byte[] messageData) {
        setEnvelopeSender(envelopeSender);
        setEnvelopeReceiver(envelopeReceiver);
        setMessageData(messageData);
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
        return new MimeMessage(session, stream);
    }

    public byte[] getMessageData() {
        return messageData;
    }

    public void setMessageData(byte[] messageData) {
        this.messageData = messageData;
        resetStream();
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

    protected void resetStream() {
        if (stream == null) {
            this.stream = new ByteArrayInputStream(getMessageData());
        } else {
            stream.reset();
        }
    }

}
