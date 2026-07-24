package com.medbooking.service;

import com.medbooking.dto.request.RegisterDoctorRequest;
import com.medbooking.dto.response.DoctorResponse;
import com.medbooking.entity.Doctor;
import com.medbooking.entity.Specialty;
import com.medbooking.repository.DoctorRepository;
import com.medbooking.service.impl.DoctorServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Kiểm thử đơn vị (Unit Test) cho các dịch vụ xem và quản lý thông tin bác sĩ.
 */
@ExtendWith(MockitoExtension.class)
class DoctorServiceTest {

    @Mock
    private DoctorRepository doctorRepository;

    @InjectMocks
    private DoctorServiceImpl doctorService;

    @Test
    @DisplayName("Kiểm thử lấy chi tiết hồ sơ bác sĩ theo ID thành công")
    void testGetDoctorById_Success() {
        Doctor doc = createDummyDoctor(2, "Dr. Smith", 1, "Tim mạch");
        when(doctorRepository.findById(2)).thenReturn(Optional.of(doc));

        DoctorResponse res = doctorService.getDoctorById(2);

        assertNotNull(res, "Thông tin bác sĩ trả về không được null");
        assertEquals(2, res.getUserId(), "Mã bác sĩ phải là 2");
        assertEquals("Dr. Smith", res.getFullName(), "Họ tên bác sĩ phải trùng khớp");
        assertEquals("Tim mạch", res.getSpecialtyName(), "Tên chuyên khoa phải trùng khớp");
    }

    @Test
    @DisplayName("Kiểm thử lấy tất cả danh sách bác sĩ")
    void testGetAllDoctors() {
        Doctor doc1 = createDummyDoctor(1, "Dr. A", 1, "Tim mạch");
        Doctor doc2 = createDummyDoctor(2, "Dr. B", 2, "Nhi khoa");

        when(doctorRepository.findAll()).thenReturn(List.of(doc1, doc2));

        List<DoctorResponse> list = doctorService.getAllDoctors();

        assertNotNull(list);
        assertEquals(2, list.size());
    }

    @Test
    @DisplayName("Kiểm thử lọc danh sách bác sĩ theo chuyên khoa")
    void testGetDoctorsBySpecialty() {
        Doctor doc = createDummyDoctor(1, "Dr. A", 1, "Tim mạch");
        when(doctorRepository.findBySpecialtyId(1)).thenReturn(List.of(doc));

        List<DoctorResponse> list = doctorService.getDoctorsBySpecialty(1);

        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("Tim mạch", list.get(0).getSpecialtyName());
    }

    @Test
    @DisplayName("Kiểm thử đăng ký tài khoản hồ sơ bác sĩ mới")
    void testRegisterDoctor_Success() {
        RegisterDoctorRequest req = new RegisterDoctorRequest();
        req.setEmail("doctor.new@hospital.com");
        req.setPassword("pass123");
        req.setSpecialtyId(1);
        req.setFullName("BS. Mới");
        req.setExperienceYears(5);
        req.setConsultationFee(new BigDecimal("400000"));
        req.setBio("Mô tả");

        Map<String, Object> procResult = new HashMap<>();
        procResult.put("p_doctor_id", 10);
        procResult.put("p_message", "Đăng ký bác sĩ thành công");

        when(doctorRepository.spRegisterDoctor(
                anyString(), anyString(), anyInt(), anyString(), anyInt(), any(BigDecimal.class), anyString()))
                .thenReturn(procResult);

        Doctor doc = createDummyDoctor(10, "BS. Mới", 1, "Tim mạch");
        when(doctorRepository.findById(10)).thenReturn(Optional.of(doc));

        DoctorResponse res = doctorService.registerDoctor(req);

        assertNotNull(res);
        assertEquals(10, res.getUserId());
        assertEquals("BS. Mới", res.getFullName());
    }

    private Doctor createDummyDoctor(Integer id, String name, Integer specialtyId, String specialtyName) {
        Doctor doc = new Doctor();
        doc.setUserId(id);
        doc.setFullName(name);
        doc.setExperienceYears(10);
        doc.setConsultationFee(new BigDecimal("500000"));
        
        Specialty specialty = new Specialty();
        specialty.setId(specialtyId);
        specialty.setName(specialtyName);
        doc.setSpecialty(specialty);

        return doc;
    }
}
