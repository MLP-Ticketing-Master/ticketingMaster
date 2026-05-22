package com.ticketmaster.backend.global.util;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MailService {
	private final JavaMailSender mailSender;

	public void sendResetLink(String email, String token) {
		// 프론트엔드 도메인 주소 넣어야 함
		String resetLink = "http://localhost:5173/password-reset?token=" + token;

		SimpleMailMessage message = new SimpleMailMessage();
		message.setTo(email);
		message.setSubject("[TicketMaster] 비밀번호 재설정 안내");
		message.setText("안녕하세요. 아래 링크를 클릭하여 비밀번호를 재설정해 주세요.\n\n" + resetLink + "\n\n링크는 30분 동안 유효합니다.");

		mailSender.send(message);
	}
}
