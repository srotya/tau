/**
 * Copyright 2016 Symantec Corporation.
 * 
 * Licensed under the Apache License, Version 2.0 (the “License”); 
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.srotya.tau.alerts.media;

import java.security.Security;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.log4j.Logger;

import com.srotya.tau.wraith.actions.alerts.Alert;
import com.sun.mail.smtp.SMTPTransport;

/**
 * Provides methods for send emails.
 * 
 * @author ambud_sharma
 */
public class MailService {

	private static final String MAIL_SMTP_FROM = "mail.smtp.from";
	private static final String MAIL_SMTP_STARTTLS_ENABLE = "mail.smtp.starttls.enable";
	private Session session;
	private Logger logger;
	private String from;

	/**
	 * Creates service according provided storm topology config.
	 */
	public MailService() {
	}

	/**
	 * @param conf
	 * @throws MessagingException
	 */
	public void init(Map<String, Object> conf) throws MessagingException {
		logger = Logger.getLogger(MailService.class);
		session = createSession(conf);
		from = conf.get(MAIL_SMTP_FROM).toString();
		boolean useSsl = false;
		if(conf.containsKey(MAIL_SMTP_STARTTLS_ENABLE)) {
			useSsl = Boolean.parseBoolean(conf.get(MAIL_SMTP_STARTTLS_ENABLE).toString());
		}
		String transportString = useSsl ? "smtps" : "smtp";
		try {
			SMTPTransport transport = (SMTPTransport) session.getTransport(transportString);
			if(!transport.isConnected()) {
				transport.connect();
			}
			transport.close();
		} catch (NoSuchProviderException e) {
			String message = "Can't initialise Mail Service. Can't make transport for " + transportString;
			logger.fatal(message, e);
			throw new MessagingException(message);
		}
	}

	/**
	 * @param msg
	 * @param alert
	 */
	protected void setMessage(MimeMessage msg, Alert alert) {
		try {
			msg.setFrom(new InternetAddress(from));
			// send only to non-null recipients
			if (alert.getTarget() != null) {
				msg.setRecipients(Message.RecipientType.TO, alert.getTarget());
				msg.setSubject(alert.getSubject());
				msg.setContent(alert.getBody(), "text/html");
				msg.setSentDate(new Date(alert.getTimestamp()));
			}
		} catch (MessagingException e) {
			logger.error("Error when trying to create new e-mail.", e);
		}
	}

	/**
	 * Sends message.
	 *
	 * @return true if message sends correctly
	 */
	public boolean sendMail(Alert alert) {
		final MimeMessage msg = new MimeMessage(session);
		if (logger.isDebugEnabled()) {
			logger.debug("MAIL BOLT: " + alert);
		}
		try {
			setMessage(msg, alert);
//			if (!transport.isConnected()) {
//				transport.connect();
//			}
//			transport.sendMessage(msg, msg.getAllRecipients());
			Transport.send(msg);
			return true;
		} catch (Exception e) {
			logger.info("Error when trying to send e-mail via, tenant ID: " + alert.getRuleGroup(), e);
			return false;
		}
	}

	/**
	 * @param conf
	 * @return
	 */
	@SuppressWarnings("restriction")
	private Session createSession(Map<String, Object> conf) {
		Properties properties = new Properties();
		for (Entry<String, Object> entry : conf.entrySet()) {
			if (entry.getKey().startsWith("mail.")) {
				properties.setProperty(entry.getKey(), entry.getValue().toString());
			}
		}
		boolean useSsl = false;
		if(conf.containsKey(MAIL_SMTP_STARTTLS_ENABLE)) {
			useSsl = Boolean.parseBoolean(conf.get(MAIL_SMTP_STARTTLS_ENABLE).toString());
		}
		if (useSsl) {
			Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
		}
		Session session = Session.getInstance(properties);
		return session;
	}

}