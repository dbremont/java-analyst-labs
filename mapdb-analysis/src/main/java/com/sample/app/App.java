package com.sample.app;

import com.github.javaparser.*;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.Expression;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class App {

    private static final String TARGET_SUPERCLASS =
        "dr.gov.sigef.framework.business.MapDB";

    // Accumulator
    private static final List<ClassInfo> results = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        // String path = "/home/dvictoriano/Code/hacienda/sigef";
        String path = "/home/dvictoriano/Code/hacienda/sigef/modulos/uepex/uepex-impl/src/main/java/dr/gov/sigef/uepex/ejecuciondelgasto/formulario/item";
        

        List<Path> javaFiles = collectJavaFiles(path);

        for (Path file : javaFiles) {
            processFile(file);
        }

        writeJson(); // 🔴 final output
    }

    private static List<Path> collectJavaFiles(String root) throws IOException {
        List<Path> files = new ArrayList<>();

        Files.walk(Paths.get(root))
                .filter(p -> p.toString().endsWith(".java"))
                .forEach(files::add);

        return files;
    }

    private static void processFile(Path filePath) {

        if (filePath.toString().contains("MapDBFuItem")) {
            System.out.println("Debug: " + filePath);
        }

        try {
            CompilationUnit cu = StaticJavaParser.parse(filePath);

            cu.findAll(ClassOrInterfaceDeclaration.class)
              .forEach(cls -> analyzeClass(cls, filePath));

        } catch (Exception e) {
            System.out.println("Error parsing: " + filePath);
            System.err.println("Error parsing: " + filePath);
        }
    }

    private static void analyzeClass(ClassOrInterfaceDeclaration cls, Path filePath) {

        System.out.println("Analyzing: " + cls.getNameAsString() + " in " + filePath);

        boolean inherits = cls.getExtendedTypes().stream()
                .anyMatch(t -> t.getNameAsString().equals("MapDB") ||
                               t.getNameAsString().equals(TARGET_SUPERCLASS));

        if (!inherits) return;

        String className = cls.getNameAsString();

        ClassInfo classInfo = new ClassInfo(className, filePath.toString());

        cls.findAll(FieldDeclaration.class).stream()
                .filter(FieldDeclaration::isStatic)
                .filter(field -> field.getElementType().asString().equals("String"))
                .forEach(field -> processField(field, classInfo));

        if (classInfo.fields.isEmpty()) {
            System.out.println("No static String fields found in " + className);
        }

        // Optional: skip classes with no fields
        if (!classInfo.fields.isEmpty()) {
            results.add(classInfo);
        }
    }

    private static void processField(FieldDeclaration field, ClassInfo classInfo) {
        for (VariableDeclarator var : field.getVariables()) {

            String fieldName = var.getNameAsString();

            Optional<Expression> initializer = var.getInitializer();

            String value = initializer
                    .map(App::extractValue)
                    .orElse("null");

            classInfo.fields.add(new FieldInfo(fieldName, value));
        }
    }

    private static String extractValue(Expression expr) {

    if (expr.isStringLiteralExpr()) {
        return expr.asStringLiteralExpr().getValue(); // no quotes
    }

    if (expr.isIntegerLiteralExpr()) {
        return expr.asIntegerLiteralExpr().getValue();
    }

    if (expr.isBooleanLiteralExpr()) {
        return String.valueOf(expr.asBooleanLiteralExpr().getValue());
    }

    if (expr.isDoubleLiteralExpr()) {
        return expr.asDoubleLiteralExpr().getValue();
    }

    if (expr.isCharLiteralExpr()) {
        return expr.asCharLiteralExpr().getValue();
    }

    // fallback → keep source representation
    return expr.toString();
}

    private static void writeJson() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        mapper.writeValue(new File("data.json"), results);

        System.out.println("✔ JSON written to data.json");
    }
}
