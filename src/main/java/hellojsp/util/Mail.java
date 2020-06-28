package hellojsp.util;

import java.util.Arrays;
import java.util.Date;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.Authenticator;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

public class Mail {

	protected String from;
	protected String mailHost;
	protected int mailPort;
	protected String mailFrom;
	protected String mailId;
	protected String mailPass;
	protected boolean ssl = false;
	protected String encoding = Config.getEncoding();

	public Mail() {
		mailHost = Config.get("mailHost");
		if(mailHost == null) mailHost = "127.0.0.1";
		mailPort = Config.getInt("mailPort");
		if(mailPort == 0) mailPort = 25;
		mailFrom = Config.get("mailFrom");
		mailId = Config.get("mailId");
		mailPass = Config.get("mailPass");
	}

	public void setFrom(String from) {
		this.from = from;
	}

	public void setHost(String host) {
		this.mailHost = host;
	}

	public void setPort(int port) {
		this.mailPort = port;
	}

	public void setSSL(boolean flag) {
		this.ssl = flag;
	}

	public void setEncoding(String enc) {
		this.encoding = enc;
	}
		
	protected Session getSession() {
		Properties props = new Properties();
		props.put("mail.smtp.host", mailHost);
		props.put("mail.smtp.port", "" + mailPort);
		if(mailFrom != null) props.put("mail.smtp.from", mailFrom);

		if(mailId != null && mailPass != null) {
			props.put("mail.smtp.auth", "true");
			if(this.ssl) {
				props.put("mail.smtp.starttls.enable","true");
				props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory"); 
			}
			SmtpAuthenticator auth = new SmtpAuthenticator(mailId, mailPass);
			
			return Session.getInstance(props, auth);
		} else {
			return Session.getInstance(props);
		}
	}

	public void send(String mailTo, String subject, String body) {
		send(new String[] { mailTo }, subject, body, null);
	}

	public void send(String mailTo, String subject, String body, String file) {
		send(new String[] { mailTo }, subject, body, new String[] { file });
	}
	
	public void send(String mailTo, String subject, String body, String[] files) {
		send(new String[] { mailTo }, subject, body, files);
	}

	public void send(String[] mailTo, String subject, String body) {
		send(mailTo, subject, body, null);
	}

	public void send(String[] mailTo, String subject, String body, String[] files) {
		try {
			MimeMessage msg = new MimeMessage(getSession());
			InternetAddress from = new InternetAddress(this.from);
			if (!"".equals(from.getPersonal())) from.setPersonal(from.getPersonal(), encoding);
			InternetAddress[] to = new InternetAddress[mailTo.length];
			for (int i = 0; i < mailTo.length; i++) {
				to[i] = new InternetAddress(mailTo[i]);
				if (!"".equals(to[i].getPersonal())) to[i].setPersonal(to[i].getPersonal(), encoding);
			}

			msg.setFrom(from);
			msg.setRecipients(MimeMessage.RecipientType.TO, to);
			msg.setSubject(subject, encoding);
			msg.setSentDate(new Date());

			if (files == null) {
				msg.setContent(body, "text/html; charset=" + encoding);
			} else {
				for (String file : files) {
					MimeBodyPart mbp1 = new MimeBodyPart();
					mbp1.setContent(body, "text/html; charset=" + encoding);
					MimeBodyPart mbp2 = new MimeBodyPart();

					FileDataSource fds = new FileDataSource(file);
					mbp2.setDataHandler(new DataHandler(fds));
					mbp2.setFileName(fds.getName());

					Multipart mp = new MimeMultipart();
					mp.addBodyPart(mbp1);
					mp.addBodyPart(mbp2);

					msg.setContent(mp);
				}
			}

			Transport.send(msg);
		} catch (Exception e) {
			Hello.errorLog("{Mail.send} mailTo:" + Arrays.toString(mailTo) + ", subject:" + subject, e);
		}
	}

	private static class SmtpAuthenticator extends Authenticator {

		private String id;
		private String pw;

		public SmtpAuthenticator(String id, String pw) {
			this.id = id;
			this.pw = pw;
		}

		protected PasswordAuthentication getPasswordAuthentication() {
			return new PasswordAuthentication(id, pw);
		}

	}

}