package de.elnarion.sample.imap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.security.Security;
import java.util.Properties;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.URLName;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentest4j.MultipleFailuresError;

import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.DummySSLSocketFactory;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;

class IMAPReaderTest {

	private static final String TEST_USER = "duke";
	private static final String TEST_EMAIL = "duke@test.com";
	private static final String TEST_PWD = "supersecure";
	private static final String TEST_EMAIL_FROM = "hank@superstore.com";
	private static final String TEST_SUBJECT = "testsubject";
	private static final String TEST_BODY = "Hello Mr. Somebody, bla bla bla Greetings bla bla";
	private static final String LOCALHOST = "127.0.0.1";

	private GreenMail greenMailServer;

	@BeforeEach
	void setup() {
		// switch off security checks for integrationtests with dynamic certificates
		Security.setProperty("ssl.SocketFactory.provider", DummySSLSocketFactory.class.getName());
		greenMailServer = new GreenMail(ServerSetupTest.IMAPS);
		greenMailServer.start();
	}

	private void addMessageToTestMailbox() throws MessagingException, AddressException {
		// create user on mail server
		GreenMailUser user = greenMailServer.setUser(TEST_EMAIL, TEST_USER, TEST_PWD);

		// create an e-mail message using javax.mail ..
		MimeMessage message = new MimeMessage((Session) null);
		message.setFrom(new InternetAddress(TEST_EMAIL_FROM));
		message.addRecipient(Message.RecipientType.TO, new InternetAddress(TEST_EMAIL));
		message.setSubject(TEST_SUBJECT);
		message.setText(TEST_BODY);

		// use greenmail to store the message
		user.deliver(message);
	}

	@AfterEach()
	void tearDown() {
		greenMailServer.stop();
	}
	
	
	@Test
	void testIMAPAccessViaURLName() throws MessagingException, IOException {
		// ARRANGE
		// create test message in inbox
		addMessageToTestMailbox();
		
		// ACT
		Properties props = new Properties();
		Session session = Session.getInstance(props);
		URLName urlName = new URLName("imaps", LOCALHOST, ServerSetupTest.IMAPS.getPort(), null, TEST_USER,
				TEST_PWD);
		Store store = session.getStore(urlName);
		store.connect();
		Folder folder = store.getFolder("INBOX");
		folder.open(Folder.READ_ONLY);
		Message[] messages = folder.getMessages();
		
		// ASSERT
		assertMessages(messages);
	}

	@Test
	void testIMAPAccessViaPropertiesWithHostInProperties() throws MessagingException, IOException {
		// ARRANGE
		// create test message in inbox
		addMessageToTestMailbox();
		
		// ACT
		Properties props = new Properties();
		props.setProperty("mail.store.protocol", "imaps");
		props.setProperty("mail.imaps.host", LOCALHOST);
		props.setProperty("mail.imaps.port", ""+ServerSetupTest.IMAPS.getPort());
		Session session = Session.getInstance(props);
		Store store = session.getStore();
		store.connect(TEST_USER,TEST_PWD);
		Folder folder = store.getFolder("INBOX");
		folder.open(Folder.READ_ONLY);
		Message[] messages = folder.getMessages();
		
		// ASSERT
		assertMessages(messages);
	}

	@Test
	void testIMAPAccessViaPropertiesWithHostInConnect() throws MessagingException, IOException {
		// ARRANGE
		// create test message in inbox
		addMessageToTestMailbox();
		
		// ACT
		Properties props = new Properties();
		props.setProperty("mail.store.protocol", "imaps");
		props.setProperty("mail.imaps.port", ""+ServerSetupTest.IMAPS.getPort());
		Session session = Session.getDefaultInstance(props);
		Store store = session.getStore("imaps");
		store.connect(LOCALHOST,TEST_USER,TEST_PWD);
		Folder folder = store.getFolder("INBOX");
		folder.open(Folder.READ_ONLY);
		Message[] messages = folder.getMessages();
		
		// ASSERT
		assertMessages(messages);
	}
	
	
	private void assertMessages(Message[] messages) throws MultipleFailuresError {
		assertAll(() -> assertNotNull(messages), () -> assertThat(1, Matchers.equalTo(messages.length)),
				() -> assertEquals(TEST_SUBJECT, messages[0].getSubject()),
				() -> assertTrue(String.valueOf(messages[0].getContent()).contains(TEST_BODY)),
				() -> assertEquals(TEST_EMAIL_FROM, messages[0].getFrom()[0].toString()));
	}


}
