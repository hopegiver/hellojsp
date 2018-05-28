package hellojsp.util;

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

	protected String mailFrom;
	
	protected String smtpHost;
	protected int smtpPort;
	protected String smtpFrom;
	protected String smtpId;
	protected String smtpPw;	
	protected boolean ssl = false;
	protected String encoding = "utf-8";

	public Mail() {
		smtpHost = Config.get("smtpHost");
		if(smtpHost == null) smtpHost = "127.0.0.1";
		smtpPort = Config.getInt("smtpPort");
		if(smtpPort == 0) smtpPort = 25;
		smtpFrom = Config.get("smtpFrom");	
	}

	public void setFrom(String from) {
		this.mailFrom = from;
	}

	public void setHost(String host) {
		this.smtpHost = host;
	}

	public void setPort(int port) {
		this.smtpPort = port;
	}

	public void setSSL(boolean flag) {
		this.ssl = flag;
	}

	public void setEncoding(String enc) {
		this.encoding = enc;
	}
		
	protected Session getSession() {
		Properties props = new Properties();
		props.put("mail.smtp.host", smtpHost);
		props.put("mail.smtp.port", "" + smtpPort);
		if(smtpFrom != null) props.put("mail.smtp.from", smtpFrom);

		if(smtpId != null && smtpPw != null) {
			props.put("mail.smtp.auth", "true");
			if(this.ssl) {
				props.put("mail.smtp.starttls.enable","true");
				props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory"); 
			}
			SmtpAuthenticator auth = new SmtpAuthenticator(smtpId, smtpPw);
			
			return Session.getInstance(props, auth);
		} else {
			return Session.getInstance(props);
		}
	}

	public void send(String mailTo, String subject, String body) throws Exception {
		send(new String[] { mailTo }, subject, body, null);
	}

	public void send(String mailTo, String subject, String body, String file) throws Exception {
		send(new String[] { mailTo }, subject, body, new String[] { file });
	}
	
	public void send(String mailTo, String subject, String body, String[] files) throws Exception {
		send(new String[] { mailTo }, subject, body, files);
	}

	public void send(String[] mailTo, String subject, String body) throws Exception {
		send(mailTo, subject, body, null);
	}

	public void send(String[] mailTo, String subject, String body, String[] files) throws Exception {

		MimeMessage msg = new MimeMessage(getSession());
		InternetAddress from = new InternetAddress(mailFrom);
		if(!"".equals(from.getPersonal())) from.setPersonal(from.getPersonal(), encoding);
		InternetAddress[] to = new InternetAddress[mailTo.length];
		for(int i=0; i<mailTo.length; i++) {
			to[i] = new InternetAddress(mailTo[i]);
			if(!"".equals(to[i].getPersonal())) to[i].setPersonal(to[i].getPersonal(), encoding);
		}

		msg.setFrom(from);
		msg.setRecipients(MimeMessage.RecipientType.TO, to);
		msg.setSubject(subject, encoding);
		msg.setSentDate(new Date());

		if(files == null) {
			msg.setContent(body, "text/html; charset=" + encoding);
		} else {
			for(int i=0; i<files.length; i++) {
				MimeBodyPart mbp1 = new MimeBodyPart();
				mbp1.setContent(body, "text/html; charset=" + encoding);
				MimeBodyPart mbp2 = new MimeBodyPart();

				FileDataSource fds = new FileDataSource(files[i]);
				mbp2.setDataHandler(new DataHandler(fds));
				mbp2.setFileName(fds.getName());

				Multipart mp = new MimeMultipart();
				mp.addBodyPart(mbp1);
				mp.addBodyPart(mbp2);

				msg.setContent(mp);
			}
		}

		Transport.send(msg);
	}

	private static class SmtpAuthenticator extends Authenticator {

		private String id = null;
		private String pw = null;

		public SmtpAuthenticator(String id, String pw) {
			this.id = id;
			this.pw = pw;
		}

		protected PasswordAuthentication getPasswordAuthentication() {
			return new PasswordAuthentication(id, pw);
		}

	}

}