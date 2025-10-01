package com.example.k5_iot_springboot.security; // 패키지 선언

import com.example.k5_iot_springboot.entity.G_User; // 사용자 엔티티 클래스 임포트
import org.springframework.lang.NonNull; // NonNull 어노테이션 임포트
import org.springframework.security.core.GrantedAuthority; // GrantedAuthority 인터페이스 임포트
import org.springframework.security.core.authority.SimpleGrantedAuthority; // SimpleGrantedAuthority 클래스 임포트
import org.springframework.stereotype.Component; // 스프링 컴포넌트 임포트

import java.util.Collection; // 컬렉션 인터페이스 임포트
import java.util.List; // 리스트 인터페이스 임포트

/**
 * === UserPrincipalMapper ===
 * : 도메인 엔티티(G_User) -> 보안 표현(UserPrincipal)로 변환
 * +) 현재 G_User에는 roles가 없으므로 기본 ROLE_USER 부여
 *
 * >> 추후 역할/권한 도입 시 해당 클래스만 변경하면 전역 반영 가능
 *
 * cf) 스프링 시큐리티는 인증/인가 단계에서 UserDetails 인터페이스를 사용 ( >> UserPrincipal)
 *      - 본 매퍼는 영속 엔티티로부터 인증/인가에 꼭 필요한 값만 뽑아
 *          , 경량/불변 VO(UserPrincipal)로 만들어 SecurityContext에 안전하게 전달되도록 하는 매퍼
 *
 * # 사용 위치 #
 * CustomUserDetailsService#loadUserByUsername(...) 가 G_User 조회
 *  -> 본 매퍼로 UserPrincipal 생성
 *  -> Authentication(Principal)에 주입되어 보안 컨텍스트에 저장
 * */
@Component // 스프링이 해당 클래스를 관리하도록 지정, 의존성 주입
public class UserPrincipalMapper { // UserPrincipal 매퍼 클래스

    @NonNull // null이 아닌 값만 허용 (매개변수에 null 전달 시 NPE 발생)
    public UserPrincipal map(@NonNull G_User user) { // G_User -> UserPrincipal 변환 메서드
//        Collection<SimpleGrantedAuthority> authorities
//                = List.of(new SimpleGrantedAuthority("ROLE_USER"));

        Collection<? extends GrantedAuthority> authorities =
                // 사용자 정보 내부의 권한이 비어져 있거나 없는 경우
                (user.getUserRoles() == null || user.getUserRoles().isEmpty())
                        // 기본 권한 "ROLE_USER" 부여
                        ? List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        // 해당 권한(들)을 GrantedAuthority 타입으로 변환하여 반환
                        : user.getUserRoles().stream()
                        .map(r -> {
                            String name = r.getRole().getName().name();
                            String role = name.startsWith("ROLE_") ? name : "ROLE_" + name;
                            return new SimpleGrantedAuthority(role);
                        })
                        .toList();

        return UserPrincipal.builder() // UserPrincipal 빌더 패턴으로 생성
                .id(user.getId()) // PK
                .username(user.getLoginId()) // 로그인 아이디
                .password(user.getPassword()) // 해시 비밀번호
                .authorities(authorities) // 권한
                .accountNonExpired(true) // 계정 만료 여부 (true: 만료 안 됨)
                .accountNonLocked(true) // 계정 잠금 여부 (true: 잠기지 않음)
                .credentialsNonExpired(true) // 자격 증명 만료 여부 (true: 만료 안 됨)
                .enabled(true) // 계정 활성화 여부 (true: 활성화)
                .build(); // 빌더로 UserPrincipal 객체 생성 및 반환
    }
}