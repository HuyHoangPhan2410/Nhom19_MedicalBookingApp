package com.medbooking.controller.admin;

import com.medbooking.dto.response.ApiResponse;
import com.medbooking.entity.Specialty;
import com.medbooking.exception.BusinessException;
import com.medbooking.repository.SpecialtyRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Administration", description = "Admin login, dashboard and CRUD APIs")
@RestController
@RequestMapping("/api/admin/specialties")
public class AdminSpecialtyController {

    private final SpecialtyRepository specialtyRepository;

    public AdminSpecialtyController(SpecialtyRepository specialtyRepository) {
        this.specialtyRepository = specialtyRepository;
    }

    @GetMapping
    public ApiResponse<List<Specialty>> getAll() {
        return ApiResponse.success(specialtyRepository.findAll());
    }

    @PostMapping
    public ApiResponse<Specialty> create(@RequestBody Map<String, String> body) {
        Specialty s = new Specialty();
        s.setName(body.get("name"));
        s.setDescription(body.get("description"));
        return ApiResponse.success("Tạo chuyên khoa thành công", specialtyRepository.save(s));
    }

    @PutMapping("/{id}")
    public ApiResponse<Specialty> update(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        Specialty s = specialtyRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "Chuyên khoa không tồn tại"));
        if (body.containsKey("name")) s.setName(body.get("name"));
        if (body.containsKey("description")) s.setDescription(body.get("description"));
        return ApiResponse.success("Cập nhật chuyên khoa thành công", specialtyRepository.save(s));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Integer id) {
        Specialty s = specialtyRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "Chuyên khoa không tồn tại"));
        specialtyRepository.delete(s);
        return ApiResponse.success("Xóa chuyên khoa thành công", null);
    }
}