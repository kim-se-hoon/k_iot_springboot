package com.example.k5_iot_springboot.dto.J_Mail;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class MailRequest{
    public record SendMail(
            @NotBlank
            @Email String email
    ) { }

    public record PasswordReset(
            @NotBlank @Email String email,
            @NotBlank String newPassword,
            @NotBlank String confirmPassword
    ) {}

}
