package com.medbooking.controller;

import com.medbooking.dto.response.ApiResponse;
import com.medbooking.dto.response.SpecialtyResponse;
import com.medbooking.service.SpecialtyService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Specialties", description = "Medical specialty lookup")
@RestController
@RequestMapping("/api/specialties")
public class SpecialtyController {

    private final SpecialtyService specialtyService;

    public SpecialtyController(SpecialtyService specialtyService) {
        this.specialtyService = specialtyService;
    }

    @GetMapping
    public ApiResponse<List<SpecialtyResponse>> getAllSpecialties() {
        return ApiResponse.success(specialtyService.getAllSpecialties());
    }

    @GetMapping("/{id}")
    public ApiResponse<SpecialtyResponse> getSpecialtyById(@PathVariable Integer id) {
        return ApiResponse.success(specialtyService.getSpecialtyById(id));
    }
}
