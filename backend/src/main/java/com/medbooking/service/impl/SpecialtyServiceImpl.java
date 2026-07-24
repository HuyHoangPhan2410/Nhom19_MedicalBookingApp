package com.medbooking.service.impl;

import com.medbooking.dto.response.SpecialtyResponse;
import com.medbooking.entity.Specialty;
import com.medbooking.exception.BusinessException;
import com.medbooking.repository.SpecialtyRepository;
import com.medbooking.service.SpecialtyService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service quản lý danh mục chuyên khoa khám bệnh.
 */
@Service
public class SpecialtyServiceImpl implements SpecialtyService {

    private final SpecialtyRepository specialtyRepository;

    public SpecialtyServiceImpl(SpecialtyRepository specialtyRepository) {
        this.specialtyRepository = specialtyRepository;
    }

    /**
     * Lấy danh sách tất cả chuyên khoa khám bệnh.
     */
    @Override
    public List<SpecialtyResponse> getAllSpecialties() {
        return specialtyRepository.findAll().stream()
                .map(s -> new SpecialtyResponse(s.getId(), s.getName(), s.getDescription()))
                .collect(Collectors.toList());
    }

    /**
     * Lấy chi tiết chuyên khoa theo ID.
     */
    @Override
    public SpecialtyResponse getSpecialtyById(Integer id) {
        Specialty specialty = specialtyRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "Chuyên khoa không tồn tại"));
        return new SpecialtyResponse(specialty.getId(), specialty.getName(), specialty.getDescription());
    }
}
