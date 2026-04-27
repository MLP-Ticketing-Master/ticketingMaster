package com.ticketmaster.backend.global.exception;

// 회원가입 시 이메일 중복이 발생할 경우 던지는 예외
public class DuplicateEmailException extends BusinessException {
	public DuplicateEmailException() {
		super(ErrorCode.DUPLICATE_EMAIL);
	}
}
