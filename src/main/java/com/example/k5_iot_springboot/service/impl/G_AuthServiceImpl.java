package com.example.k5_iot_springboot.service.impl;

import com.example.k5_iot_springboot.common.enums.RoleType;
import com.example.k5_iot_springboot.dto.G_Auth.request.SignInRequest;
import com.example.k5_iot_springboot.dto.G_Auth.request.SignUpRequest;
import com.example.k5_iot_springboot.dto.G_Auth.response.SignInResponse;
import com.example.k5_iot_springboot.dto.J_Mail.MailRequest;
import com.example.k5_iot_springboot.dto.ResponseDto;
import com.example.k5_iot_springboot.entity.G_Role;
import com.example.k5_iot_springboot.entity.G_User;
import com.example.k5_iot_springboot.provider.JwtProvider;
import com.example.k5_iot_springboot.repository.G_RoleRepository;
import com.example.k5_iot_springboot.repository.G_UserRepository;
import com.example.k5_iot_springboot.service.G_AuthService;
import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class G_AuthServiceImpl implements G_AuthService {
    private final G_UserRepository userRepository;
    private final G_RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    // @Bean 메서드로 BCryptPasswordEncoder 객체를 리턴하면
    //      , 스프링 컨테이너에 등록될 때 PasswordEncoder 타입으로 인식 (주입 시 해당 타입으로 정의 권장)
    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;

    @Override
    @Transactional
    public void signUp(SignUpRequest req) {
        // 1) 유효성 검사 (중복 체크 - 유니크 제약 검증)
        if (userRepository.existsByLoginId(req.loginId())) {
            throw new IllegalArgumentException("이미 사용 중인 로그인 아이디입니다.");
        }
        if (userRepository.existsByEmail(req.email())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }
        if (userRepository.existsByNickname(req.nickname())) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }

        // 2) 비밀번호 해시 - BCrypt 패스워드
        String encoded = passwordEncoder.encode(req.password());

        // 3) 엔티티 생성 / 저장
        G_User user = G_User.builder()
                .loginId(req.loginId())
                .password(encoded)
                .email(req.email())
                .nickname(req.nickname())
//                .gender(req.gender()) - null 허용
                .build();
        // 기본 권한 부여
        // cf) getReferenceById: 특정 ID를 가진 엔티티의 프록시 객체를 즉시 반환
        G_Role defaultRole = roleRepository.getReferenceById(RoleType.USER);
        user.grantRole(defaultRole); // 변경 감지로 user_roles가 insert 됨 (cascade=ALL)

        userRepository.save(user);
    }

    @Override // 읽기 전용
    public ResponseDto<SignInResponse> signIn(SignInRequest req) {

        // 스프링 시큐리티 표준 인증 흐름 (UserDetailsService + PasswordEnoder)
        Authentication auth = authenticationManager.authenticate(
                // 내부에서 DaoAuthenticationProvider가
                //      , CustomUserDetailsService.loadUserByUsername(loginId) 호출
                // >> G_User 조회
                // >> UserPrincipalMapper.map()으로 UserPrincipal 생성
                // >> PasswordEncoder로 비밀번호 비교
                // >> 인증 성공 시 Authentication 반환
                // >>>>> JWT 발급
                new UsernamePasswordAuthenticationToken(req.loginId(), req.password())
        );

        // 2) 권한 문자열 추출
        Set<String> roles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        // 3) JWT 발급 (username=loginId, roles 포함)
        String accessToken = jwtProvider.generateJwtToken(req.loginId(), roles);

        // 4) 만료 시각 추출하여 응답에 포함
        Claims claims = jwtProvider.getClaims(accessToken);
        long expiresAt = claims.getExpiration().getTime();

        // 5) 응답 DTO 구성
        SignInResponse response = new SignInResponse(
                "Bearer",
                accessToken,
                expiresAt,
                req.loginId(),
                roles
        );

        return ResponseDto.setSuccess("로그인 성공", response);
    }

    @Override
    @Transactional
    public void resetPassword(MailRequest.@Valid PasswordReset req) {
        if (!req.newPassword().equals(req.confirmPassword()))
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");

        G_User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new IllegalArgumentException("가입된 이메일이 아닙니다."));
        String encoded = passwordEncoder.encode(req.newPassword());

        user.changePassword(encoded);

        userRepository.save(user);

    }
}