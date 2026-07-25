package com.medbooking.controller.admin;

import com.medbooking.dto.request.LoginRequest;
import com.medbooking.dto.response.ApiResponse;
import com.medbooking.dto.response.AuthResponse;
import com.medbooking.entity.User;
import com.medbooking.exception.BusinessException;
import com.medbooking.repository.UserRepository;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    private final UserRepository userRepository;

    public AdminAuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> adminLogin(@RequestBody LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(401, "Email hoặc mật khẩu không chính xác"));

        if (!BCrypt.checkpw(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(401, "Email hoặc mật khẩu không chính xác");
        }

        if (user.getRole() != User.Role.admin) {
            throw new BusinessException(403, "Bạn không có quyền truy cập trang quản trị");
        }

        if (!user.getIsActive()) {
            throw new BusinessException(403, "Tài khoản quản trị viên đang bị khóa");
        }

        AuthResponse response = new AuthResponse(
                user.getId(),
                user.getEmail(),
                user.getRole().name(),
                "Administrator",
                "admin-token-" + user.getId()
        );

        return ApiResponse.success("Đăng nhập quản trị thành công", response);
    }
}