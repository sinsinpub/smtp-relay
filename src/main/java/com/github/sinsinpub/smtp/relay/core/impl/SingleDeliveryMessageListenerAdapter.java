package com.github.sinsinpub.smtp.relay.core.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.subethamail.smtp.MessageContext;
import org.subethamail.smtp.MessageHandler;
import org.subethamail.smtp.MessageHandlerFactory;
import org.subethamail.smtp.RejectException;
import org.subethamail.smtp.TooMuchDataException;
import org.subethamail.smtp.helper.SimpleMessageListener;

/**
 * MessageHandlerFactory implementation which adapts to a collection of MessageListeners.
 * <p>
 * The key point is that for any message, all accepted recipient will get only single delivery.<br>
 * No multi-listener supported as single delivery data may not be safe.
 * 
 * @see org.subethamail.smtp.helper.SimpleMessageListenerAdapter
 * @author Jeff Schnitzer
 * @author sin_sin
 */
public class SingleDeliveryMessageListenerAdapter implements MessageHandlerFactory {

    private SimpleMessageListener listener;

    /**
     * Initializes this factory with a single listener.
     */
    public SingleDeliveryMessageListenerAdapter(SimpleMessageListener listener) {
        this.listener = listener;
    }

    public MessageHandler create(MessageContext ctx) {
        return new Handler(ctx);
    }

    /**
     * Needed by this class to track which listeners need delivery.
     */
    static class Delivery {
        SimpleMessageListener listener;
        String recipient;
        List<String> recipients = new ArrayList<String>();

        public Delivery(SimpleMessageListener listener, String recipient) {
            this.listener = listener;
            this.recipient = recipient;
            addRecipients(recipient);
        }

        public void addRecipients(String recipient) {
            this.recipients.add(recipient);
        }

        public SimpleMessageListener getListener() {
            return this.listener;
        }

        public String getRecipient() {
            return this.recipient;
        }

        public List<String> getRecipients() {
            return recipients;
        }

    }

    /**
     * Class which implements the actual handler interface.
     */
    class Handler implements MessageHandler {
        MessageContext ctx;
        String from;
        Delivery delivery = null;

        public Handler(MessageContext ctx) {
            this.ctx = ctx;
        }

        public void from(String from) throws RejectException {
            this.from = from;
        }

        public void recipient(String recipient) throws RejectException {
            if (SingleDeliveryMessageListenerAdapter.this.listener.accept(this.from, recipient)) {
                if (null == this.delivery) {
                    this.delivery = new Delivery(listener, recipient);
                } else {
                    this.delivery.addRecipients(recipient);
                }
            } else {
                throw new RejectException(553, "<" + recipient + "> address unknown.");
            }
        }

        public void data(InputStream data) throws TooMuchDataException, IOException {
            if (delivery.getRecipients().size() > 1) {
                delivery.getListener().deliver(this.from,
                        StringUtils.join(delivery.getRecipients(), ","), data);
            } else {
                delivery.getListener().deliver(this.from, delivery.getRecipient(), data);
            }
        }

        public void done() {
        }

    }

}
