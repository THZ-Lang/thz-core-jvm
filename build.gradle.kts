// ==============================================================================
// thz-core â€” NÃºcleo da linguagem THZ-LANG (Core/Stdlib)
//
// RepositÃ³rio independente: https://github.com/<org>/thz-core
// Consumido por thz-cli e thz-gui via Gradle Composite Build (dev) ou
// artefato publicado (CI): thz.lang:thz-core:<versÃ£o>
// ==============================================================================

plugins {
    `java-library`
    `maven-publish`
}

val repoRoot = rootProject.projectDir.resolve("../thz-lang/")
val versionFile = if (file("version.txt").exists()) file("version.txt") else repoRoot.resolve("version.txt")
val thzVersion = if (versionFile.exists()) versionFile.readText().trim() else "0.4.0"

group = "thz.lang"
version = thzVersion

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
    withSourcesJar()
}

dependencies {
    // Motor de Documentos (PDF, XLSX, DOCX) â€” capacidade de stdlib DOCUMENTO.*
    implementation("org.apache.poi:poi-ooxml:5.3.0")
    implementation("com.github.librepdf:openpdf:2.0.3")
    implementation("org.apache.logging.log4j:log4j-api:2.23.1")
    implementation("org.apache.logging.log4j:log4j-core:2.23.1")

    // Banco de Dados Local (SQLite JDBC)
    implementation("org.xerial:sqlite-jdbc:3.47.1.0")

    // Criptografia AvanÃ§ada (Argon2id, AES-XTS, ChaCha20)
    implementation("org.bouncycastle:bcprov-jdk18on:1.79")

    // Testes Automatizados
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-processing"))
}

val gerarVersaoTask = tasks.register("gerarVersaoPropriedades") {
    val saidaDir = layout.buildDirectory.dir("generated/version-resources")
    val v = thzVersion
    inputs.property("thzVersion", v)
    outputs.dir(saidaDir)

    doLast {
        val propFile = saidaDir.get().file("thz-version.properties").asFile
        propFile.parentFile.mkdirs()
        propFile.writeText("version=$v\n")
    }
}

sourceSets["main"].resources.srcDir(gerarVersaoTask)

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            pom {
                name.set("THZ-LANG Core")
                description.set("NÃºcleo da linguagem THZ-LANG: lexer, parser, semÃ¢ntico, interpretador, runtime decimal exato, SIMD, IR e governanÃ§a.")
            }
        }
    }
}

