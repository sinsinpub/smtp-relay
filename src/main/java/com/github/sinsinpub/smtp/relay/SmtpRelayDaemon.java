package com.github.sinsinpub.smtp.relay;

import java.util.Date;

import org.apache.commons.lang.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.sinsinpub.smtp.relay.context.ApplicationVersion;
import com.github.sinsinpub.smtp.relay.core.InstanceFactory;
import com.github.sinsinpub.smtp.relay.utils.AppBootUtils;

/**
 * Simple bootstrap entry.
 *
 * @author sin_sin
 */
public class SmtpRelayDaemon {

	public static void main(String[] args) {
		long startTime = System.currentTimeMillis();
		AppBootUtils.initVmDefault();
		AppBootUtils.initLogger();
		AppBootUtils.printSystemInfo();
		Logger logger = LoggerFactory.getLogger(SmtpRelayDaemon.class);
		logger.info("Starting {} v{} server...",
				ApplicationVersion.getInstance().getApplicationName(),
				ApplicationVersion.getInstance().getApplicationVersion());
		Date factoryStartTime = InstanceFactory.getFactory().getStartupDate();
		logger.info("JMX remote connector address: {}",
				InstanceFactory.getFactory().getJmxRmiAddress());
		logger.info("Server is currently listening on {}",
				InstanceFactory.getFactory().getListenerBindAddress());
		logger.info(new StringBuilder("Server started at: ").append(
				DateFormatUtils.format(factoryStartTime,
						"yyyy-MM-dd HH:mm:ss Z E"))
				.append(", used: ")
				.append(System.currentTimeMillis() - startTime)
				.append(" ms")
				.toString());
	}

}
