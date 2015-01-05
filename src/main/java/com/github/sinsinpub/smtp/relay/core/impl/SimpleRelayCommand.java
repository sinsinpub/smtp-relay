package com.github.sinsinpub.smtp.relay.core.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.concurrent.NotThreadSafe;
import javax.mail.Address;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.sinsinpub.smtp.relay.context.MailContext;
import com.github.sinsinpub.smtp.relay.core.MailForwardCommand;
import com.github.sinsinpub.smtp.relay.exception.RetryException;
import com.github.sinsinpub.smtp.relay.transport.AdditiveWaitRetryStrategy;
import com.github.sinsinpub.smtp.relay.transport.RetryStrategy;
import com.github.sinsinpub.smtp.relay.utils.ConcurrentUtil;

/**
 * Simple implementation of mail relay worker thread.
 * 
 * @author sin_sin
 */
@NotThreadSafe
final class SimpleRelayCommand implements MailForwardCommand {

    private final static Logger logger = LoggerFactory.getLogger(SimpleRelayCommand.class);

    private MailContext mail;
    private String mtaName;
    private Session mta;
    private File errDump;
    private RetryStrategy retryStrategy;
    private ConcurrentMap<String, AtomicLong> exceptionCounter;

    private MimeMessage mimeMsgCache = null;

    public SimpleRelayCommand() {
        super();
    }

    public SimpleRelayCommand(MailContext mail, Session session) {
        this(mail, session, null, null);
    }

    public SimpleRelayCommand(MailContext mail, Session session, File errDump,
            RetryStrategy retryStrategy) {
        this();
        this.mail = mail;
        this.mta = session;
        this.errDump = errDump;
        if (retryStrategy != null) {
            this.retryStrategy = retryStrategy;
        } else {
            this.retryStrategy = new AdditiveWaitRetryStrategy();
        }
    }

    @Override
    public void setMail(MailContext mail) {
        this.mail = mail;
    }

    @Override
    public void setMailSession(Session session) {
        this.mta = session;
    }

    public void setSessionName(String sessionName) {
        this.mtaName = sessionName;
    }

    public void setErrDump(File errDump) {
        this.errDump = errDump;
    }

    public void setRetryStrategy(RetryStrategy retryStrategy) {
        this.retryStrategy = retryStrategy;
    }

    public void setExceptionCounter(
            ConcurrentMap<String, AtomicLong> exceptionCounter) {
        this.exceptionCounter = exceptionCounter;
    }

    private void incrementExceptionCount() {
        if (exceptionCounter != null) {
            ConcurrentUtil.incrementLong(exceptionCounter,
                    String.valueOf(mtaName));
        }
    }

    public void run() {
        if (mail == null) {
            logger.error("No correct mail message context input, stop forwarding");
            incrementExceptionCount();
            return;
        }
        if (mta == null) {
            logger.error("No backend mail transport agent session configured correctly, stop forwarding");
            incrementExceptionCount();
            return;
        }
        if (retryStrategy == null) {
            logger.error("No retry strategy instance configured correctly, stop forwarding");
            incrementExceptionCount();
            return;
        }
        while (retryStrategy.shouldRetry()) {
            try {
                if (mimeMsgCache == null) {
                    mimeMsgCache = mail.getMimeMessage(mta);
                }
                Transport.send(mimeMsgCache);
                for (Address address : mimeMsgCache.getAllRecipients())
                    logger.info(mimeMsgCache.getMessageID() + " forwarded to "
                            + address);
                break;
            } catch (Exception e) {
                try {
                    logger.error(
                            String.format("%s, retrying for %s...",
                                    e.toString(),
                                    retryStrategy.printRetriesCount()), e);
                    retryStrategy.tryRetry();
                } catch (RetryException retryExp) {
                    incrementExceptionCount();
                    if (errDump != null) {
                        try {
                            MimeMessage failedToSend = mimeMsgCache;
                            String file = failedToSend.getMessageID()
                                    .substring(
                                            1,
                                            failedToSend.getMessageID()
                                                    .lastIndexOf(">"))
                                    + ".msg";
                            logger.error("Message dumped to " + file);
                            int at = mail.getEnvelopeReceiver()
                                    .lastIndexOf('@');
                            File recipientDump;
                            if (at > 0) {
                                recipientDump = new File(errDump,
                                        mail.getEnvelopeReceiver().substring(0,
                                                at));
                            } else {
                                recipientDump = new File(errDump,
                                        mail.getEnvelopeReceiver());
                            }
                            if (!recipientDump.exists()) {
                                if (!recipientDump.mkdir()) {
                                    throw new IOException(
                                            "Make new directory failed for "
                                                    + recipientDump.toString());
                                }
                            }
                            FileOutputStream fos = null;
                            try {
                                fos = new FileOutputStream(new File(
                                        recipientDump, file));
                                failedToSend.writeTo(fos);
                            } finally {
                                if (fos != null) {
                                    IOUtils.closeQuietly(fos);
                                }
                            }
                        } catch (Exception fatal) {
                            logger.error(
                                    "Dump mail message error: "
                                            + fatal.toString(), fatal);
                        }
                    }
                    logger.error("Retry failed: " + retryExp.getMessage());
                    break;
                }
            }
        }
    }

}
