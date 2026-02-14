plugins {
    id("org.openrewrite.build.recipe-library") version "latest.release"
    id("org.openrewrite.build.moderne-source-available-license") version "latest.release"
}

group = "org.openrewrite.recipe"
description = "Recipes for Joda-Time."

val rewriteVersion = rewriteRecipe.rewriteVersion.get()
dependencies {
    annotationProcessor("org.projectlombok:lombok:latest.release")
    compileOnly("org.projectlombok:lombok:latest.release")

    implementation(platform("org.openrewrite:rewrite-bom:${rewriteVersion}"))
    implementation("org.openrewrite:rewrite-java")

    implementation("org.openrewrite.recipe:rewrite-java-dependencies:${rewriteVersion}")

    testImplementation("org.openrewrite:rewrite-test")
    testImplementation("org.openrewrite:rewrite-maven")

    testRuntimeOnly("org.openrewrite:rewrite-java-21")
}

recipeDependencies {
    parserClasspath("org.threeten:threeten-extra:1.8.0")
    testParserClasspath("joda-time:joda-time:2.12.3")
}
