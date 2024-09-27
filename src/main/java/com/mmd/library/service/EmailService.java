package com.mmd.library.service;

import com.mmd.library.constant.UrlConstants;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Service
public class EmailService {

	private JavaMailSender javaMailSender;

	// Load HTML file from classpath
	Resource resource = new ClassPathResource("verify-email.html");

	public EmailService(JavaMailSender javaMailSender) {
		this.javaMailSender = javaMailSender;
	}

	@Async
	public void sendEmailConfirmation(String to, String token) {

		byte[] bdata = null;
		try (InputStream inputStream = resource.getInputStream()) {
			bdata = FileCopyUtils.copyToByteArray(inputStream);
		} catch (IOException e) {
			System.out.println("Something went wrong");
			return;
		}

		String htmlContent = new String(bdata, StandardCharsets.UTF_8);
		String htmlContentWithLink = htmlContent.replace("[Verification URL]", "%s/verify-email/%s".formatted(UrlConstants.BACKEND_URL, token));

		MimeMessage message = javaMailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message);
		try {
			helper.setSubject("Email Confirmation");
			helper.setTo(to);
			helper.setText(htmlContentWithLink, true);
		} catch (MessagingException e) {
			System.out.println("Something with messages went wrong");
			return;
		}
		javaMailSender.send(message);
	}
}
