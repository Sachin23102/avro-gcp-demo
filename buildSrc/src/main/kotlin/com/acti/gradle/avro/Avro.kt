package com.acti.gradle.avro

import org.apache.avro.AvroTypeException
import org.apache.avro.ParseContext
import org.apache.avro.Schema
import org.apache.avro.SchemaParseException
import org.apache.avro.compiler.specific.SpecificCompiler
import org.apache.avro.idl.IdlReader
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import java.io.File
import java.util.EnumSet

class AvroPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val mainSourceSet = target.extensions.getByType<SourceSetContainer>()
            .getByName("main")

        val generateAvroInlinedAvscFromIdlTask =
            target.tasks.register("generateAvroInlinedAvscFromIdl", GenerateAvroAvscFromIdl::class) {
                source(project.file("src/${mainSourceSet.name}"))
                inlineReferencedSchemas = true
                outputDir.convention(project.layout.buildDirectory.dir("generated/avro/${mainSourceSet.name}/avsc-inline"))
            }

        target.tasks.named("classes") {
            dependsOn(generateAvroInlinedAvscFromIdlTask)
        }

        val generateAvroAvscTask = target.tasks.register("generateAvroAvscFromIdl", GenerateAvroAvscFromIdl::class) {
            source(project.file("src/${mainSourceSet.name}"))
            outputDir.convention(project.layout.buildDirectory.dir("generated/avro/${mainSourceSet.name}/avsc"))
        }

        val javaGenerationSourceTask = generateAvroInlinedAvscFromIdlTask
        val generateAvroSchemaJavaTask =
            target.tasks.register("generateJavaFromAvroSchema", GenerateJavaFromAvroSchema::class) {
                source(javaGenerationSourceTask.get().outputDir.get())
                outputDir.convention(project.layout.buildDirectory.dir("generated/avro/${mainSourceSet.name}/java"))
                dependsOn(javaGenerationSourceTask)
                mainSourceSet.java.srcDir(outputDir)
            }

        target.tasks.named<JavaCompile>(mainSourceSet.compileJavaTaskName) {
            source(generateAvroSchemaJavaTask)
        }
    }
}

abstract class GenerateAvroAvscFromIdl : SourceTask() {
    init {
        include("**/*.avdl")
    }


    @OutputDirectory
    val outputDir: DirectoryProperty = project.objects.directoryProperty()


    @Input
    var inlineReferencedSchemas = false


    @TaskAction
    fun generate() {
        val parseContext = ParseContext()
        val reader = IdlReader(parseContext)
        val sources = source
        sources.forEach { source ->
            logger.info("Will process IDL file: {}", source)
            val parsed = runCatching { reader.parse(source.toPath()) }
                .getOrElse {
                    logger.warn(
                        """Failed to parse IDL file ${source.path}. Context follows.""", it
                    )
                    parseContext.typesByName().forEach { name, schema ->
                        logger.warn("parseContext.typesByName: $name -> $schema")
                    }
                    throw IllegalStateException("Failed to parse IDL file ${source.path}", it)
                }
            parsed.warnings.forEach { warning ->
                logger.warn("IDL Warning for file ${source.path}: $warning")
            }
        }
        parseContext.commit()

        val otherSchemas = parseContext.resolveAllSchemas().toMutableSet()

        parseContext.typesByName().forEach { (name, schema) ->
            otherSchemas.remove(schema)
            val outputFile = File(outputDir.get().asFile, "$name.avsc")
            logger.debug("Output schema {} to {}", schema, outputFile)

            if (inlineReferencedSchemas) {
                @Suppress("DEPRECATION") // don't know how to do this easily with formatter
                outputFile.writeText(schema.toString(emptySet(), true))
            } else {
                @Suppress("DEPRECATION") // don't know how to do this easily with formatter
                outputFile.writeText(schema.toString(otherSchemas, true))
            }

            otherSchemas.add(schema)
        }

    }
}


abstract class GenerateJavaFromAvroSchema : SourceTask() {
    init {
        include("**/*.avsc")
    }

    @OutputDirectory
    val outputDir: DirectoryProperty = project.objects.directoryProperty()


    @TaskAction
    fun generate() {
        compileEachSourceSeparately()
//        compileEachSchemaOnce()
    }

    private fun compileEachSchemaOnce() {
        val parseContext = ParseContext()
        val parser = Schema.Parser(parseContext)
        val sourceToSchema =
            source.associateWith { source ->
                val schema = source.inputStream().use {
                    parser.parseInternal(it.bufferedReader().readText())
                }
                parseContext.commit()
                parseContext.resolveAllSchemas()
                schema
            }

        sourceToSchema.forEach { (source, schema) ->
            val resolved = parseContext.getNamedSchema(schema.fullName)
            SpecificCompiler(resolved).apply {
                isCreateNullSafeAnnotations = true
            }.compileToDestination(source, outputDir.get().asFile)
        }
    }

    private fun compileEachSourceSeparately() {
        source.associateWith { source ->
            logger.info("Compiling {}", source)
            //                source.inputStream().use { parser.parse(it.bufferedReader().readText()) }
            val parser = Schema.Parser()
            val resolved = parser.parse(source)
            SpecificCompiler(resolved).apply {
                isCreateNullSafeAnnotations = true
            }.compileToDestination(source, outputDir.get().asFile)
        }
    }
}