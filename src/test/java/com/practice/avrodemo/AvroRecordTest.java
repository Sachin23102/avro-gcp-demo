package com.practice.avrodemo;

import static com.practice.avrodemo.avro.EmploymentStatus.EMPLOYED;
import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.practice.avrodemo.avro.ContactId;
import com.practice.avrodemo.avro.Employee;
import java.util.UUID;

import com.practice.avrodemo.avro.EmploymentStatus;
import com.practice.avrodemo.avro.User;
import org.junit.jupiter.api.Test;

class AvroRecordTest {
    @Test
    void testCreatingEmployeeRecordWithAllRequiredFields() {
        Employee employee = Employee.newBuilder()
                .setName("John Doe")
                .setSalary(1000L)
                .setAge(45)
                //                .setContactId(ContactId.newBuilder().setId(UUID.randomUUID()).build())
                .build();
        assertNotNull(employee);
        assertThat(employee.getName()).isEqualTo("John Doe");
        assertThat(employee.getSalary()).isEqualTo(1000L);
        assertThat(employee.getAge()).isEqualTo(45);
    }

    @Test
    void testCreatingEmployeeRecordWithoutSalary() {
        Employee employee = Employee.newBuilder()
                .setName("John Doe")
                .setAge(23)
                .setContactId(new ContactId(UUID.randomUUID()))
                .build();
    }

    @Test
    void testCreatingEmployeeRecordWithoutAge() {
        Employee employee =
                Employee.newBuilder().setName("John Doe").setSalary(1000L).build();
    }

    @Test
    void testCreatingUserRecordWithoutEmploymentStatus() throws Exception {
        User user = User.newBuilder()
                .setContactId(new ContactId(UUID.randomUUID()))
                .setUsername("sachin")
                .setEmail("test@email.com")
                .build();

        assertThat(user.getEmploymentStatus()).isEqualTo(EMPLOYED);
    }
}
