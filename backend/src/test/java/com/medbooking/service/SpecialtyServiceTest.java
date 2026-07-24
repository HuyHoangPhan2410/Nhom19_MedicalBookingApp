package com.medbooking.service;

import com.medbooking.dto.response.SpecialtyResponse;
import com.medbooking.entity.Specialty;
import com.medbooking.repository.SpecialtyRepository;
import com.medbooking.service.impl.SpecialtyServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Kiểm thử đơn vị (Unit Test) cho dịch vụ xem danh mục chuyên khoa.
 */
@ExtendWith(MockitoExtension.class)
class SpecialtyServiceTest {

    @Mock
    private SpecialtyRepository specialtyRepository;

    @InjectMocks
    private SpecialtyServiceImpl specialtyService;

    @Test
    @DisplayName("Kiểm thử lấy danh sách tất cả các chuyên khoa")
    void testGetAllSpecialties() {
        // 1. Tạo danh sách chuyên khoa mẫu
        Specialty s1 = new Specialty();
        s1.setId(1);
        s1.setName("Nhi khoa");

        Specialty s2 = new Specialty();
        s2.setId(2);
        s2.setName("Da liễu");

        when(specialtyRepository.findAll()).thenReturn(List.of(s1, s2));

        // 2. Thực thi lấy danh sách
        List<SpecialtyResponse> result = specialtyService.getAllSpecialties();

        // 3. Kiểm tra kết quả
        assertNotNull(result, "Danh sách chuyên khoa không được null");
        assertEquals(2, result.size(), "Phải chứa 2 chuyên khoa");
        assertEquals("Nhi khoa", result.get(0).getName(), "Tên chuyên khoa đầu tiên phải là Nhi khoa");
    }

    @Test
    @DisplayName("Kiểm thử lấy thông tin chi tiết chuyên khoa theo ID")
    void testGetSpecialtyById() {
        // 1. Tạo đối tượng chuyên khoa mẫu
        Specialty s1 = new Specialty();
        s1.setId(1);
        s1.setName("Nhi khoa");

        when(specialtyRepository.findById(1)).thenReturn(Optional.of(s1));

        // 2. Thực thi lấy chi tiết
        SpecialtyResponse result = specialtyService.getSpecialtyById(1);

        // 3. Kiểm tra kết quả
        assertNotNull(result, "Kết quả trả về không được null");
        assertEquals("Nhi khoa", result.getName(), "Tên chuyên khoa phải trùng khớp");
    }
}
