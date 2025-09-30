package com.example.k5_iot_springboot.repository;

import com.example.k5_iot_springboot.entity.G_User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface G_UserRepository extends JpaRepository<G_User, Long> {

    // roles의 지연로딩(LAZY)로 인한 LazyInitializationException 위험
    // 해결 방법 1) 리포지토리에 fetch-join 쿼리 추가
    // : u.roles 컬렉션을 한 번에 가져오기 때문에 N+1 문제 방지
    @Query("""
        SELECT u 
        FROM G_User u
            LEFT JOIN FETCH u.userRoles
        WHERE u.loginId = :loginId
    """)
    Optional<G_User> findWithRolesByLoginId(@Param("loginId") String loginId);

    // 해결 방법 2) JPA의 @EntityGraph을 사용하여 fetch join을 자동으로 적용 방식
    // - @EntityGraph: DATA JPA에서 fetch 조인을 어노테이션으로 대신하는 기능
    @EntityGraph(attributePaths = "userRoles")
    Optional<G_User> findByLoginId(String loginId);

    @EntityGraph(attributePaths = "userRoles")
    Optional<G_User> findWithRolesById(
            @NotNull(message = "userId는 필수입니다.")
            @Positive(message = "userId는 양수여야 합니다.")
            Long id
    );

    boolean existsByLoginId(String loginId);
    boolean existsByEmail(String email);
    boolean existsByNickname(String nickname);

    Optional<G_User> findByEmail(@NotBlank @Email String email);
}