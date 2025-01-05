plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.apache.avro:avro-idl:1.12.0")
    implementation("org.apache.avro:avro-compiler:1.12.0")
    implementation("org.apache.avro:avro-tools:1.12.0")
}

gradlePlugin {
    plugins {
        create("avro") {
            id = "com.acti.avro"
            implementationClass = "com.acti.gradle.avro.AvroPlugin"
        }
    }
}
