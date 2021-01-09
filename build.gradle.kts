import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.google.protobuf.gradle.*

buildscript {
    repositories {
        mavenLocal()
        maven(url = "http://maven.aliyun.com/nexus/content/groups/public/")
        maven(url = "http://maven.aliyun.com/nexus/content/repositories/jcenter")
        mavenCentral()
        jcenter()
//        maven(url = "http://repo.spring.io/plugins-release")
        maven(url = "https://plugins.gradle.org/m2/")
    }
    dependencies {
        classpath("com.google.protobuf:protobuf-gradle-plugin:0.8.13")
    }
}

plugins {
    idea
    id("org.springframework.boot") version "2.3.4.RELEASE"
    id("io.spring.dependency-management") version "1.0.10.RELEASE"
    id("com.google.protobuf") version "0.8.13"

    kotlin("plugin.serialization") version "1.4.10"
    kotlin("jvm") version "1.4.0"
    kotlin("plugin.spring") version "1.4.0"
}

group = "net.lz1998"
version = "0.0.12"
java.sourceCompatibility = JavaVersion.VERSION_1_8

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenLocal()
//    maven(url = "http://maven.aliyun.com/nexus/content/groups/public/")
//    maven(url = "http://maven.aliyun.com/nexus/content/repositories/jcenter")
    mavenCentral()
    jcenter()
//    maven(url = "http://repo.spring.io/plugins-release")
    maven(url = "https://plugins.gradle.org/m2/")
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.0.0-RC2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.0.0-RC")
//    api("net.mamoe:mirai-core:2.0-M1-1")
    implementation("net.mamoe:mirai-core-jvm:2.0-M2") {
        exclude("net.mamoe","mirai-core-api")
        exclude("net.mamoe","mirai-core-utils")
    }
    implementation("net.mamoe:mirai-core-api-jvm:2.0-M2") {
        exclude("net.mamoe", "mirai-core-utils")
    }
    implementation("net.mamoe:mirai-core-utils-jvm:2.0-M2")
    implementation("com.squareup.okhttp3:okhttp:4.8.0")
//    implementation("com.google.protobuf:protobuf-javalite:3.8.0")

    implementation("com.google.protobuf:protobuf-java:3.12.2")
    implementation("com.googlecode.protobuf-java-format:protobuf-java-format:1.4")
    implementation("com.google.protobuf:protobuf-java-util:3.12.2")
//    implementation("com.googlecode.protobuf:protobuf-java-format:1.2")

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    annotationProcessor("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
}

protobuf {
    generatedFilesBaseDir = "$projectDir/src"
    println(generatedFilesBaseDir)
    protoc {
        // You still need protoc like in the non-Android case
        artifact = "com.google.protobuf:protoc:3.8.0"
    }
    generateProtoTasks {
        all().forEach {
            it.builtins {
//                remove("java")
//                id("java"){
//                    option("lite")
//                }
            }
        }
    }
}
sourceSets {
    main {
        proto {
            // 除了默认的'src/main/proto'目录新增proto文件的方法
            srcDir("onebot_idl")
            include("**/*.proto")
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "1.8"
    }
}

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}