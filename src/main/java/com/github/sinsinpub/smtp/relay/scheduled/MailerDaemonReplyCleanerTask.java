package com.github.sinsinpub.smtp.relay.scheduled;

import java.io.BufferedReader;
import java.io.StringReader;
import java.io.StringWriter;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import javax.mail.BodyPart;
import javax.mail.Flags.Flag;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.search.HeaderTerm;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple timer task for cleaning auto-replied status report mails from
 * postmaster by facilities of Mail Store (POP3/IMAP protocol).
 * 
 * @author sin_sin
 */
@ThreadSafe
public class MailerDaemonReplyCleanerTask implements Runnable {

    private final static Logger logger = LoggerFactory.getLogger(MailerDaemonReplyCleanerTask.class);

    protected static final String DEFAULT_MAILER_DAEMON_FROM_ADDRESS = "MAILER-DAEMON";
    protected static final String DEFAULT_INBOX_FOLDER_NAME = "INBOX";
    protected static final String DEFAULT_STATUS_REPORT_CONTENT_TYPE_HEADER = "report-type=delivery-status;";
    protected static final String DEFAULT_AUTO_SUBMITTED_HEADER = "auto-replied";

    private String sessionName;
    @GuardedBy("itself")
    private Session popSession;

    public MailerDaemonReplyCleanerTask() {
        super();
    }

    @Override
    public void run() {
        try {
            doClean();
        } catch (Throwable e) {
            // Do not throw any exception as default ScheduledExecutorService will
            // remove the Runnable who throws an exception.
            logger.error(e.toString(), e);
        }
    }

    /**
     * Connect to the Mail Store and walk through default in-box, expunge those
     * reply messages which made by mailer daemon itself for reporting deliver
     * status.
     */
    private void doClean() {
        Store store = null;
        Folder inbox = null;
        try {
            store = popSession.getStore();
            store.connect();
            // Only INBOX folder support for this POP3 provider
            // No auto archiving could apply :(
            inbox = store.getDefaultFolder().getFolder(
                    DEFAULT_INBOX_FOLDER_NAME);
            inbox.open(Folder.READ_WRITE);
            HeaderTerm term = new HeaderTerm("Content-Type",
                    DEFAULT_STATUS_REPORT_CONTENT_TYPE_HEADER);
            logger.info(
                    "Searching status report messages in folder {} of <{}>...",
                    inbox.getFullName(), getSessionName());
            // Doing search will make a long-time data exchange with mailer server
            Message[] messages = inbox.search(term);
            // Message[] messages = inbox.getMessages();
            logger.info("Status report messages in total: {}/{}",
                    messages.length, inbox.getMessageCount());
            if (messages.length > 0) {
                int idx = 0;
                for (Message message : messages) {
                    idx += 1;
                    MimeMessage mime = (MimeMessage) message;
                    logger.debug("Checking mail #{}: {}", idx,
                            mime.getMessageID());
                    boolean systemMail = true;
                    systemMail &= mime.getContentType().contains(
                            DEFAULT_STATUS_REPORT_CONTENT_TYPE_HEADER);

                    String from = null;
                    String[] returnPath = mime.getHeader("Return-Path");
                    if (returnPath.length >= 1) {
                        from = returnPath[0];
                    } else if (mime.getFrom().length >= 1) {
                        from = mime.getFrom()[0].toString();
                    }
                    if (systemMail && from != null) {
                        systemMail &= from.contains(DEFAULT_MAILER_DAEMON_FROM_ADDRESS);
                    }

                    String[] autoFlag = mime.getHeader("Auto-Submitted");
                    if (systemMail && autoFlag != null && autoFlag.length >= 1) {
                        systemMail &= DEFAULT_AUTO_SUBMITTED_HEADER.equals(autoFlag[0]);
                    }

                    if (systemMail) {
                        mime.setFlag(Flag.DELETED, true);
                        logger.debug(
                                "Status report mail #{}:{} has been marked as DELETED",
                                idx, mime.getMessageID());
                        String reason = retrieveSystemMessagePart(mime);
                        if (reason != null) {
                            logger.info(
                                    "Mail {} reported status: {}",
                                    String.valueOf(idx) + ":"
                                            + mime.getMessageID(), reason);
                        }
                    }
                }
                logger.info("Status report messages deleted: {}/{}",
                        inbox.getDeletedMessageCount(), messages.length);
            }
        } catch (MessagingException e) {
            logger.error("Exception on communicating with mailer daemon: " + e,
                    e);
        } finally {
            if (inbox != null) {
                try {
                    inbox.close(true);
                } catch (MessagingException e) {
                    logger.error(e.toString(), e);
                }
            }
            if (store != null) {
                try {
                    store.close();
                } catch (MessagingException e) {
                    // Ignore this
                }
            }
        }
    }

    /**
     * Try to lookup the reason message which made by mailer daemon from the first
     * part of status report contents.
     * 
     * @param message The message of status report
     * @return One line report of the command output by mailer daemon, null if no
     *         reasonable found or exception occurs.
     */
    private String retrieveSystemMessagePart(MimeMessage message) {
        String messageId = null;
        try {
            messageId = message.getMessageID();
            if (message.getContent() instanceof MimeMultipart) {
                MimeMultipart multipart = (MimeMultipart) message.getContent();
                if (multipart.getCount() >= 1) {
                    BodyPart firstBodyPart = multipart.getBodyPart(0);
                    String body = String.valueOf(firstBodyPart.getContent());
                    BufferedReader br = new BufferedReader(new StringReader(
                            body));
                    StringWriter sw = new StringWriter();
                    boolean outputFound = false;
                    String line = br.readLine();
                    while (line != null) {
                        if (!outputFound && line.contains("Command output")) {
                            outputFound = true;
                        }
                        if (outputFound && StringUtils.isNotBlank(line)) {
                            if (StringUtils.isNotEmpty(sw.toString())) {
                                sw.append(" ");
                            }
                            sw.append(StringUtils.strip(line));
                        }
                        line = br.readLine();
                    }
                    br.close();
                    sw.close();
                    return outputFound ? sw.toString() : null;
                }
            }
        } catch (Exception e) {
            logger.warn("Retrieving content exception: {} on message {}",
                    e.toString(), messageId);
        }
        return null;
    }

    public String getSessionName() {
        return String.valueOf(sessionName);
    }

    public void setSessionName(String sessionName) {
        this.sessionName = sessionName;
    }

    public Session getPopSession() {
        return popSession;
    }

    public void setPopSession(Session popSession) {
        this.popSession = popSession;
    }

}
