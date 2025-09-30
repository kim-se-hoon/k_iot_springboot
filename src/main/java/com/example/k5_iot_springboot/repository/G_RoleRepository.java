package com.example.k5_iot_springboot.repository;

import com.example.k5_iot_springboot.common.enums.RoleType;
import com.example.k5_iot_springboot.entity.G_Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface G_RoleRepository extends JpaRepository<G_Role, RoleType> {
}
