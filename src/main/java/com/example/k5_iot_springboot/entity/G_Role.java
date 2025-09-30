package com.example.k5_iot_springboot.entity;

import com.example.k5_iot_springboot.common.enums.RoleType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * === 권한 코드 엔티티 (roles) ===
 * : PK(role_name) - Enum/문자열 매핑
 * */
@Entity
@Table(name = "roles")
@Getter
@NoArgsConstructor
public class G_Role {
    /** 권한 명(PK) - Enum을 문자열로 저장 */
    @Id @Enumerated(EnumType.STRING)
    @Column(name = "role_name", length = 30, nullable = false)
    private RoleType name;

    public G_Role(RoleType name) {
        this.name = name;
    }
}