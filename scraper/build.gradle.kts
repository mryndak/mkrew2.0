plugins {
	java
	alias(libs.plugins.spring.boot)
	alias(libs.plugins.spring.dependency.management)
	alias(libs.plugins.axion.release)
}

group = "pl.mkrew"
version = scmVersion.version
description = "Demo project for Spring Boot"

scmVersion {
	tag {
		prefix.set("v")
	}
}

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation(libs.spring.boot.starter.data.jpa)
	implementation(libs.spring.boot.starter.quartz)
	implementation(libs.spring.boot.starter.web)
	implementation(libs.jsoup)
	runtimeOnly(libs.postgresql)
	testImplementation(libs.spring.boot.starter.test)
	testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.withType<Test> {
	useJUnitPlatform()
}
