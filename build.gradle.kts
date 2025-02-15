plugins {
	java
	id("org.springframework.boot") version "3.5.0-SNAPSHOT"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "me.sosedik"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
	maven("https://repo.spring.io/milestone")
	maven("https://repo.spring.io/snapshot")
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-web")
	developmentOnly("org.springframework.boot:spring-boot-devtools")

	// Storing & working with data
	runtimeOnly("com.h2database:h2")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")

	// HTML templates
	implementation("org.springframework.boot:spring-boot-starter-thymeleaf")

	// WebP support
	implementation("com.github.usefulness:webp-imageio:0.9.0")

	// RAR archives support
	implementation("com.github.junrar:junrar:7.5.5")

	compileOnly("org.projectlombok:lombok:1.18.36")
	annotationProcessor("org.projectlombok:lombok:1.18.36")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")

	testCompileOnly("org.projectlombok:lombok:1.18.36")
	testAnnotationProcessor("org.projectlombok:lombok:1.18.36")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
