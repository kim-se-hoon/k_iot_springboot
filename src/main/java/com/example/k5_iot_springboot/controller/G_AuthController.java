package com.example.k5_iot_springboot.controller;

import com.example.k5_iot_springboot.dto.G_Auth.request.SignInRequest;
import com.example.k5_iot_springboot.dto.G_Auth.request.SignUpRequest;
import com.example.k5_iot_springboot.dto.G_Auth.response.SignInResponse;
import com.example.k5_iot_springboot.dto.J_Mail.MailRequest;
import com.example.k5_iot_springboot.dto.ResponseDto;
import com.example.k5_iot_springboot.service.G_AuthService;
import com.example.k5_iot_springboot.service.J_MailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth") // 회원가입, 로그인, 아이디찾기, 비밀번호 재설정 등
@RequiredArgsConstructor
public class G_AuthController {
    private final G_AuthService authService;
    private final J_MailService mailService;

    /** 회원가입 */
    @PostMapping("/sign-up")
    public ResponseEntity<ResponseDto<Void>> signUp(@Valid @RequestBody SignUpRequest req) {
        authService.signUp(req);
        return ResponseEntity.ok(ResponseDto.setSuccess("회원가입이 완료되었습니다.", null));
    }

    /** 로그인 */
    @PostMapping("/sign-in")
    public ResponseEntity<ResponseDto<SignInResponse>> signIn(@Valid @RequestBody SignInRequest req) {
        ResponseDto<SignInResponse> response = authService.signIn(req);
        return ResponseEntity.ok().body(response);
    }

    /** 이메일 전송 */
    @PostMapping("/send-email")
    public ResponseEntity<ResponseDto<Void>> sendEmail(@Valid @RequestBody MailRequest.SendMail req) {
        mailService.sendEmail(req);
        return ResponseEntity.noContent().build();
    }

    /** 이메일 인증 */
    @GetMapping("/verify")
    public ResponseEntity<ResponseDto<Void>> verifyEmail(@RequestParam String token) {
        mailService.verifyEmail(token);
        return ResponseEntity.noContent().build();
    }

    /** 아이디 찾기 */


    /** 비밀번호 재설정 */
    @PostMapping("/reset-password")
    public ResponseEntity<ResponseDto<Void>> resetPassword(@Valid @RequestBody MailRequest.PasswordReset req) {
        authService.resetPassword(req);
        return ResponseEntity.noContent().build();
    }
}