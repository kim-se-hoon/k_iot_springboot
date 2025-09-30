package com.example.k5_iot_springboot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

// Spring 애플리케이션 구성 정보를 제공하는 설정 클래스
// [ 역할 ]
//: JavaMailSender Bean 구성
// - SMTP/STARTTLS/인코딩 설정
@Configuration
public class MailConfig {
    @Value("${spring.mail.host}") private String host;
    @Value("${spring.mail.port}") private int port;

    @Value("${spring.mail.username}") private String username;
    @Value("${spring.mail.password}") private String password;

    @Bean
    public JavaMailSender javaMailSender() {

        // JavaMailSender의 기본 구현체 생성
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(username);
        mailSender.setPassword(password);

        // 이메일 전손 시 사용할 추가 속성 설정을 위한 객체 생성
        Properties props = mailSender.getJavaMailProperties();

        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.enable", "true");
        props.put("mail.debug", "true");

        return mailSender;
    }



}
