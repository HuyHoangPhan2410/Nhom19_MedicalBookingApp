package com.medbooking.controller.admin;

import com.medbooking.entity.Doctor;
import com.medbooking.entity.Patient;
import com.medbooking.entity.Specialty;
import com.medbooking.entity.User;
import com.medbooking.repository.DoctorRepository;
import com.medbooking.repository.PatientRepository;
import com.medbooking.repository.SpecialtyRepository;
import com.medbooking.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {
    @Mock UserRepository userRepository;
    @Mock DoctorRepository doctorRepository;
    @Mock PatientRepository patientRepository;
    @Mock SpecialtyRepository specialtyRepository;

    @Test
    void convertingPatientToDoctorCreatesRequiredDoctorProfile() {
        User user = new User();
        user.setId(31);
        user.setEmail("patient@example.com");
        user.setRole(User.Role.patient);
        Patient patient = new Patient();
        patient.setUserId(31);
        patient.setFullName("Nguyen Van A");
        Specialty specialty = new Specialty();
        specialty.setId(2);

        when(userRepository.findById(31)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(doctorRepository.findById(31)).thenReturn(Optional.empty());
        when(patientRepository.findById(31)).thenReturn(Optional.of(patient));
        when(specialtyRepository.findById(2)).thenReturn(Optional.of(specialty));

        AdminUserController controller = new AdminUserController(userRepository, doctorRepository, patientRepository, specialtyRepository);
        Map<String, String> body = new HashMap<>();
        body.put("role", "doctor");
        body.put("specialtyId", "2");
        body.put("experienceYears", "3");
        body.put("consultationFee", "200000");
        controller.updateUser(31, body);

        ArgumentCaptor<Doctor> captor = ArgumentCaptor.forClass(Doctor.class);
        verify(doctorRepository).save(captor.capture());
        Doctor savedDoctor = captor.getValue();
        assertEquals(User.Role.doctor, user.getRole());
        assertSame(user, savedDoctor.getUser());
        assertEquals("Nguyen Van A", savedDoctor.getFullName());
        assertSame(specialty, savedDoctor.getSpecialty());
        assertEquals(3, savedDoctor.getExperienceYears());
        assertEquals("200000", savedDoctor.getConsultationFee().toPlainString());
    }
}
