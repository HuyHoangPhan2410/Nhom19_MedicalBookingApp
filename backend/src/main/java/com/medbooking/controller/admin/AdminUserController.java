package com.medbooking.controller.admin;

import com.medbooking.dto.response.ApiResponse;
import com.medbooking.entity.User;
import com.medbooking.exception.BusinessException;
import com.medbooking.repository.UserRepository;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserRepository userRepository;

    public AdminUserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public ApiResponse<List<User>> getAllUsers() {
        return ApiResponse.success(userRepository.findAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<User> getUserById(@PathVariable Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "Người dùng không tồn tại"));
        return ApiResponse.success(user);
    }

    @PostMapping
    public ApiResponse<User> createUser(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String password = body.get("password");
        String role = body.get("role");

        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(400, "Email đã tồn tại trong hệ thống");
        }

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(BCrypt.hashpw(password, BCrypt.gensalt(10)));
        user.setRole(User.Role.valueOf(role));
        user.setIsActive(true);
        user.setEmailVerified(true);

        return ApiResponse.success("Tạo tài khoản thành công", userRepository.save(user));
    }

    @PutMapping("/{id}")
    public ApiResponse<User> updateUser(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "Người dùng không tồn tại"));

        if (body.containsKey("email")) {
            String newEmail = body.get("email");
            if (!newEmail.equals(user.getEmail()) && userRepository.existsByEmail(newEmail)) {
                throw new BusinessException(400, "Email đã tồn tại");
            }
            user.setEmail(newEmail);
        }
        if (body.containsKey("role")) {
            user.setRole(User.Role.valueOf(body.get("role")));
        }
        if (body.containsKey("isActive")) {
            user.setIsActive(Boolean.parseBoolean(body.get("isActive")));
        }
        if (body.containsKey("password") && !body.get("password").isEmpty()) {
            user.setPasswordHash(BCrypt.hashpw(body.get("password"), BCrypt.gensalt(10)));
        }

        return ApiResponse.success("Cập nhật tài khoản thành công", userRepository.save(user));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteUser(@PathVariable Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "Người dùng không tồn tại"));
        userRepository.delete(user);
        return ApiResponse.success("Xóa tài khoản thành công", null);
    }
}