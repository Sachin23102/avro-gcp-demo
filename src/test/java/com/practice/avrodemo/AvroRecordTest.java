package com.practice.avrodemo;

import static com.practice.avrodemo.avro.EmploymentStatus.EMPLOYED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.practice.avrodemo.avro.ContactId;
import com.practice.avrodemo.avro.Employee;
import com.practice.avrodemo.avro.User;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.junit.jupiter.api.Test;

class AvroRecordTest {
    @Test
    void compareAvroAndJsonSize() throws IOException {
        Employee employee = Employee.newBuilder()
                .setName("John Doe")
                .setSalary(1000L)
                .setAge(45)
                .setContactId(ContactId.newBuilder().setId(UUID.randomUUID()).build())
                .build();

        // Serialize the Avro record to a binary format
        var outputStream = new ByteArrayOutputStream();
        var encoder = EncoderFactory.get().binaryEncoder(outputStream, null);
        var writer = new SpecificDatumWriter<>(Employee.class);
        writer.write(employee, encoder);
        encoder.flush();

        // assertions
        System.out.println("avro serialization: = " + outputStream);
        byte[] avroBytes = outputStream.toByteArray();
        String jsonString = employee.toString();
        byte[] jsonBytes = jsonString.getBytes();
        System.out.println("Avro size: " + avroBytes.length);
        System.out.println("JSON size: " + jsonBytes.length);

        assertThat(avroBytes.length).isLessThan(jsonBytes.length);
    }

    @Test
    void shouldCreateEmployeeWithAllRequiredFields() {
        // contactId and age have default values in schema, so they are not required
        Employee employee = Employee.newBuilder()
                .setName("John Doe")
                .setSalary(1000L)
                //                .setAge(45)
                //                .setContactId(ContactId.newBuilder().setId(UUID.randomUUID()).build())
                .build();

        assertNotNull(employee);
        assertThat(employee.getName()).isEqualTo("John Doe");
        assertThat(employee.getSalary()).isEqualTo(1000L);
        assertThat(employee.getAge()).isEqualTo(45);
    }

    @Test
    void shouldNotCreateEmployeeWithRequiredFieldAsNull() {
        // required fields cannot be set to null, if they are not optional
        Employee employee = Employee.newBuilder().setName(null).setSalary(1000L).build();
    }

    @Test
    void shouldNotCreateEmployeeRecordWithoutSalary() {
        // optional fields can be set to null, but are required to be set if they don't have default values
        Employee employee = Employee.newBuilder()
                .setName("John Doe")
                //            .setSalary(null)
                .build();
    }

    @Test
    void testCreatingUserRecordWithoutEmploymentStatus() {
        User user = User.newBuilder()
                .setUsername("sachin")
                .setContactId(new ContactId(UUID.randomUUID()))
                .setEmail("test@email.com")
                .build();

        assertThat(user.getEmploymentStatus()).isEqualTo(EMPLOYED);
    }
}
