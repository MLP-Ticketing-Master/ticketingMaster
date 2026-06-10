package com.ticketmaster.backend.global.util;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MailService {
	private final JavaMailSender mailSender;

	// 프론트 주소를 환경별 설정값으로 분리 — 로컬/운영 링크가 자동으로 달라지게 함
	@Value("${app.frontend-base-url}")
	private String frontendBaseUrl;

	public void sendResetLink(String email, String token) {

		String resetLink = frontendBaseUrl + "/password-reset?token=" + token;

		SimpleMailMessage message = new SimpleMailMessage();
		message.setTo(email);
		message.setSubject("[TicketMaster] 비밀번호 재설정 안내");
		message.setText("안녕하세요. 아래 링크를 클릭하여 비밀번호를 재설정해 주세요.\n\n" + resetLink + "\n\n링크는 30분 동안 유효합니다.");

		mailSender.send(message);
	}
}
