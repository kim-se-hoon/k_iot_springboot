package com.example.k5_iot_springboot.service.impl;

import com.example.k5_iot_springboot.dto.J_Mail.MailRequest;
import com.example.k5_iot_springboot.provider.JwtProvider;
import com.example.k5_iot_springboot.service.J_MailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class J_MailServiceImpl implements J_MailService {
    private final JavaMailSender javaMailSender;


    @Value("${spring.mail.username}")
    private String senderEmail;
    private final JwtProvider jwtProvider;

    private MimeMessage createEmail(String email, String token) throws MessagingException {
        // 메일 내용을 생성하는 메서드
        // : 메일 주소와 JWT 토큰을 매개변수로 받음
        MimeMessage message = javaMailSender.createMimeMessage();
        message.setFrom(senderEmail); // 환경변수의 이메일 (발신자)
        message.setRecipients(MimeMessage.RecipientType.TO, email); // 매개변수의 이메일 (수신자)

        message.setSubject("=== [k5_iot_springboot] 이메일 인증 링크 발송  ==="); // 이메일 제목 설정

        String body = """
                    <h3>이메일 인증링크 입니다.</h3>
                    <a href="http://localhost:8080/api/v1/auth/verify?token=%s">여기를 클릭하여 인증을 완료해주세요.</a>
                """.formatted(token);

        message.setText(body, "UTF-8", "html");

        return message;
    }

    @Override
    public void sendEmail(MailRequest.@Valid SendMail req) {
        try {
            String token = jwtProvider.generateEmailJwtToken(req.email());
            MimeMessage message = createEmail(req.email(), token);

            javaMailSender.send(message);

            System.out.println("인증 이메일이 전송되었습니다.");

        } catch (MessagingException | MailException e) {
            System.err.println("이메일 전송 실패: " + e.getMessage());
            throw new RuntimeException("이메일 전송 실패", e);
        }
    }

    @Override
    public void verifyEmail(String token) {
        String email = jwtProvider.getEmailFromJwt(token);
        System.out.println("이메일 인증이 완료되었습니다. 이메일: " + email);
    }
}