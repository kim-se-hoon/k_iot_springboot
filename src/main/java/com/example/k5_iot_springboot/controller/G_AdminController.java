package com.example.k5_iot_springboot.controller;

import com.example.k5_iot_springboot.dto.G_Admin.request.RoleManageRequest;
import com.example.k5_iot_springboot.dto.G_Admin.response.RoleManageResponse;

import com.example.k5_iot_springboot.dto.ResponseDto;
import com.example.k5_iot_springboot.security.UserPrincipal;
import com.example.k5_iot_springboot.service.G_AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** 관리자 전용 사용자 관리 컨트롤러 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
// @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')) - 복수 권한 체크
// +) controller 자체의 권한 부여도 가능
//      , 컨트롤러 내부의 각각의 메서드에 서로 다른 권한 부여도 해당 어노테이션 사용
public class G_AdminController {
    private final G_AdminService adminService;
    // 권한 갱신, 추가, 삭제

    // cf) 갱신 VS 추가
    // - 갱신) 기존 권한을 전부 지우고, 요청 권한만 남음 (권한 세트를 통째로 변경)
    // - 추가) 기존 권한 유지 (기존 + 요청 권한) - 권한을 확장/보강

    // 권한 갱신
    @PutMapping("/roles/replace")
    // 자원의 상태를 통째로 교체 (덮어쓰기 - 동일한 요청을 여러 번 보내도 결과가 같음)
    // : 멱등성
    public ResponseEntity<ResponseDto<RoleManageResponse.UpdateRolesResponse>> replaceRoles(
            @AuthenticationPrincipal UserPrincipal principal, // 관리자 토큰 정보 가져오기
            @Valid @RequestBody RoleManageRequest.UpdateRolesRequest req) {
        ResponseDto<RoleManageResponse.UpdateRolesResponse> response = adminService.replaceRoles(principal, req);
        return ResponseEntity.ok().body(response);
    }

    // 권한 추가
    @PostMapping("/roles/add")
    // 새로운 자원을 추가 생성하거나, 기존 자원에 무언가를 덧붙임
    // : 요청을 여러 번 보내면 결과가 달라질 수 있음
    public ResponseEntity<ResponseDto<RoleManageResponse.AddRoleResponse>> addRole(
            @AuthenticationPrincipal UserPrincipal principal, // 관리자 토큰 정보 가져오기
            @Valid @RequestBody RoleManageRequest.AddRoleRequest req
    ) {
        ResponseDto<RoleManageResponse.AddRoleResponse> response = adminService.addRole(principal, req);
        return ResponseEntity.ok().body(response);
    }

    // 권한 삭제
    @PostMapping("/roles/remove")
    public ResponseEntity<ResponseDto<RoleManageResponse.RemoveRoleResponse>> removeRole(
            @AuthenticationPrincipal UserPrincipal principal, // 관리자 토큰 정보 가져오기
            @Valid @RequestBody RoleManageRequest.RemoveRoleRequest req
    ) {
        ResponseDto<RoleManageResponse.RemoveRoleResponse> response = adminService.removeRole(principal, req);
        return ResponseEntity.ok().body(response);
        // return ResponseEntity.ok(ResponseDto.setSuccess("권한이 삭제되었습니다.", null));
    }
}