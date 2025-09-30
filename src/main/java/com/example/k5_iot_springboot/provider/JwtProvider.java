package com.example.k5_iot_springboot.provider;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.*;

/*
 * === JwtProvider ===
 * : JWT(JSON Web Token) 토큰을 생성하고 검증하는 역할
 *       >> 로그인 후 서버가 만들어서 클라이언트(브라우저)에게 전달하는 문자열 토큰
 *
 * cf) JWT
 *   : 사용자 정보를 암호화된 토큰으로 저장
 *   - (클라이언트가) 서버에 요청할 때마다 전달 가능 (사용자 정보 확인용, Authorization: Bearer <토큰>)
 *   - 서버는 토큰을 검증하여 누가 요청했는지 판단
 *   >> 주로 로그인 인증에 사용
 *
 *   +) JWT 구조
 *       - header: 어떤 알고리즘으로 서명했는지
 *       - payload: 사용자 정보 (예: username-로그인아이디, roles-권한)
 *       - signature: 토큰 변조 방지용 서명
 *
 *   +) HS256 암호화 알고리즘 사용한 JWT 서명
 *       - 비밀키는 Base64로 인코딩 지정
 *       - JWT 만료 기간은 10시간으로 지정
 *           >> 환경 변수 설정 (jwt.secret / jwt.expiration)
 *
 *   # JwtProvider 클래스 전체 역할 #
 *   1) 토큰 생성(발급)                                - generateJwtToken 메서드
 *   2) Bearer 제거                                   - removeBearer 메서드
 *   3) 토큰 검증/파싱                                 - parseClaimsInternal 메서드
 *   4) payload에 저장된 데이터 추출 (username, roles)  - getUsernameFromJwt, getRolesFromJwt 메서드
 *   5) 만료까지 남은 시간 계산                         - getRemainingMillis 메서드
 *
 * */
@Component
// cf) @Component(클래스 레벨 선언) - 스프링 런타임 시 컴포넌트 스캔을 통해 자동으로 빈을 찾고 등록 (의존성 주입)
//     @Bean(메서드 레벨 선언) - 반환되는 객체를 개발자가 수동으로 빈 등록
public class JwtProvider {

    // === 상수 & 필드 선언 === //
    /** Authorization의 접두사 */
    public static final String BEARER_PREFIX = "Bearer "; // removeBearer에서 사용
    /** 커스텀 클레임 키 */
    public static final String CLAIM_ROLES = "roles";

    /** 서명용 비밀키, 엑세스 토큰 만료시간(ms), 만료 직후 허용할 시계 오차(s) */
    // 환경 변수에 지정한 비밀키와 만료 시간 저장 변수 선언
    private final SecretKey key;
    private final long jwtExpirationMs;
    private final long jwtEmailExpirationMs;
    private final int clockSkewSeconds;

    /** 검증/파싱 파서: 파서를 생성자에서 1회 구성하여 재사용 - 성능/일관성 보장 (JJWT의 파서 객체) */
    private final JwtParser parser;

    // === 생성자: 환경변수로부터 설정 주입 + 파서 준비 ===
    public JwtProvider(
            // @Value: application.properties나 application.yml과 같은 설정 파일의 값을 클래스 변수에 주입
            //          >> 데이터 타입 자동 인식
            @Value("${jwt.secret}") String secret, // cf) Base64 인코딩된 비밀키 문자열이어야 함!
            @Value("${jwt.expiration}") long jwtExpirationMs,
            @Value("${jwt.email-expiration}") long jwtEmailExpirationMs,
            @Value("${jwt.clock-skew-seconds:0}") int clockSkewSeconds // 기본 0 - 옵션
    ) {
        // 생성자: JwtProvider 객체 생성 시 비밀키와 만료시간 초기화

        // 키 강도 검증(Base64 디코딩 후 256 비트 이상 권장)
        byte[] secretBytes = Decoders.BASE64.decode(secret);
        if (secretBytes.length < 32) {
            // 32 bytes == 256 bits
            // : HS256에 적정한 강도의 키를 강제하여 보안 강화
            throw new IllegalArgumentException("jwt.secret은 항상 256 비트 이상을 권장합니다.");
        }

        // HMAC-SHA 알고리즘으로 암호화된 키 생성
        this.key = Keys.hmacShaKeyFor(secretBytes); // HMAC-SHA용 SecretKey 객체 생성
        this.jwtExpirationMs = jwtExpirationMs;
        this.jwtEmailExpirationMs = jwtEmailExpirationMs;
        this.clockSkewSeconds = Math.max(clockSkewSeconds, 0); // 음수 방지

        this.parser = Jwts.parser()
                .verifyWith(this.key) // 해당 키로 서명 검증을 수행하는 파서 (이후 파싱마다 반복 설정 X)
                .build();
    }

    /* ==============
     * 토큰 생성
     * ============== */

    /**
     * 엑세스 토큰 생성
     * @param username  sub(Subject)에 저장할 사용자 식별자
     * @param roles     권한 목록(중복 제거용 Set 권장) - JSON 배열로 직렬화
     *
     * subject=sub(username), roles는 커스텀 클레임 */
    public String generateJwtToken(String username, Set<String> roles) {
        long now = System.currentTimeMillis();
        Date iat = new Date(now);
        Date exp = new Date(now + jwtExpirationMs);

        // List로 변환하여 직렬화 시 타입 안정성 확보
        List<String> roleList = (roles == null) ? List.of() : new ArrayList<>(roles);

        return Jwts.builder()
                // 표준 클레임 sub(Subject)에 사용자 아이디(또는 고유 식별자) 설정
                .setSubject(username)
                .claim(CLAIM_ROLES, roleList) // 커스텀 클레임 키에 권한 목록 저장
                .setIssuedAt(iat) // 표준 클레임에 현재 시간 설정 (발행 시간)
                .setExpiration(exp) // 현재 시간에 만료 시간을 더한 설정 (만료 시간)
//                .signWith(key, SignatureAlgorithm.HS256)
                .signWith(key) // 서명 키로 서명 (자동 HS256 선택) - 비밀키를 서명
                .compact(); // 빌더를 압축하여 최종 JWT 문자열 생성
    }

    public String generateEmailJwtToken(String email) {
        return Jwts.builder()
                .claim("email", email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtEmailExpirationMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /* ==============
     * Bearer 처리
     * ============== */

    /** HTTP Authorization 헤더에서 "Bearer " 제거 */
    public String removeBearer(String bearerToken) { // 입력: "Bearer <token>"
        if (bearerToken == null || !bearerToken.startsWith(BEARER_PREFIX)) {
            throw new IllegalArgumentException("Authorization 형식이 올바르지 않습니다.");
        }
        // substring 인자가 한 개인 경우: index 0 부터 인자값 "전"까지 잘라내기
        return bearerToken.substring(BEARER_PREFIX.length()).trim(); // 순수 토큰 반환
    }

    /* ==============
     * 검증 / 파싱
     * ============== */

    /**
     * 내부 파싱(검증 포함) - 서명 검증 + 구조 검증한 뒤 Claims(페이로드)를 반환
     *      >> 만료 시 clock-skew 허용 옵션
     * */
    private Claims parseClaimsInternal(String token, boolean allowClockSkewOnExpiry) {
        // allowClockSkewOnExpiry: 만료 직후 허용 오차 적용 여부
        try {
            return parser.parseSignedClaims(token).getPayload();
            // 1) 토큰 서명 검증 (key로 signature 확인)
            // 2) JWT 기본 구조 검사(h.p.s)
            // 3) 성공 시 Claims 꺼내기 가능 (.getPayload() 가능)
        } catch (ExpiredJwtException ex) {
            // 토큰이 만료된 경우 발생하는 JJWT 전용 예외 처리
            // : 만료 시간이 지난 토큰에도(예외 안에도) Claims 정보가 들어있음
            if(allowClockSkewOnExpiry && clockSkewSeconds > 0 && ex.getClaims() != null) {
                // 호출부가 "만료 직후 오차 허용"을 활성화했고, 설정값이 0보다 큰지 확인
                Date exp = ex.getClaims().getExpiration(); // 만료 시각(exp) 추출
                if (exp != null) {
                    long skewMs = clockSkewSeconds * 1000L; // 허용 오차(초)를 밀리초로 변환
                    long now = System.currentTimeMillis();
                    if (now - exp.getTime() <= skewMs) {
                        // 현재 시각 - 만료 시각 <= 허용오차 이내면 "방금 만료" 간주
                        return ex.getClaims(); // 예외에서 Claims를 꺼내 그대로 유효한 것으로 반환
                    }
                }
            }
            throw ex; // 허용 오차 범위를 벗어나면 원래의 만료 예외를 다시 던짐

            // EX) 토큰 만료가 12:00:00 이고 서버가 12:00:45로 45초 빠른 경우 clockSkewSeconds가 60이기 때문에
            //          , 유효한 요청으로 판단하여 Claims 반환
        }
    }

    /**
     * 토큰 유효성 검사 (서명/만료 포함)
     * / clock-skew 허용 적용
     *
     * >> 컨트롤러/필터에서 사용가능한 토큰인지 여부 확인 */
    public boolean isValidToken(String tokenWithoutBearer) {
        // 검증 - 서명 불일치, 변조, 포맷 이상, 만료(허용 오차 초과) 등 모든 예외는 catch로 전달 - false 반환
        try {
            parseClaimsInternal(tokenWithoutBearer, true); // clock-skew 허용 적용
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Claims 추출 (검증 포함) */
    public Claims getClaims(String tokenWithoutBearer) {
        // 유효성 검사 + 파싱을 한 번에 처리하고, payload(Claims) 반환
        return parseClaimsInternal(tokenWithoutBearer, true);
    }

    /** 실제 페이로드값 (Claims 값 추출)
     *  : sub - .getSubject()
     *  : 커스텀 클레임 - .get("클레임명")
     * */
    public String getUsernameFromJwt(String tokenWithoutBearer) {
        return getClaims(tokenWithoutBearer).getSubject();
    }

    /** roles >> Set<String> 변환 */
    @SuppressWarnings("unchecked") // 제네릭 캐스팅 경고 억제 (런타임 타입 확인으로 보완)
    public Set<String> getRolesFromJwt(String tokenWithoutBearer) {
        // get("roles")로 커스텀 클레임을 가져오면, JSON 파싱 결과가 List로 반환이 일반적
        //      >> 문자열 집합(Set<String>)으로 표준화해서 반환
        Object raw = getClaims(tokenWithoutBearer).get(CLAIM_ROLES);
        if (raw == null) return Set.of(); // 권한 없음

        if (raw instanceof List<?> list) {
            Set<String> result = new HashSet<>(); // 중복 제거 목적
            for (Object o: list) if (o != null) result.add(o.toString());
            return result;
        }

        if (raw instanceof Set<?> set) {
            Set<String> result = new HashSet<>();
            for (Object o: set) if (o != null) result.add(o.toString());
            return result;
        }

        return Set.of(raw.toString());
    }

    public String getEmailFromJwt(String token) {
        Claims claims = getClaims(token);
        return claims.get("email", String.class);
    }

    /** 남은 만료시간(ms)이 음수면 이미 만료 */
    public long getRemainingMillis(String tokenWithoutBearer) {
        Claims c = parseClaimsInternal(tokenWithoutBearer, true);
        return c.getExpiration().getTime() - System.currentTimeMillis();
    }
}