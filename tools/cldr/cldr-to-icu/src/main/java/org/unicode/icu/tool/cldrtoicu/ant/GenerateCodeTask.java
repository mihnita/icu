// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu.ant;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.unicode.icu.tool.cldrtoicu.CodeGenerator;
import org.unicode.icu.tool.cldrtoicu.generator.ResourceFallbackCodeGenerator;

public final class GenerateCodeTask extends Task {
    private Path cldrPath;
    private Path cOutDir;
    private Path javaOutDir;
    private String action;

    private class GeneratedFileDef {
        String cRelativePath;
        String javaRelativePath;
        CodeGenerator generator;

        public GeneratedFileDef(String cRelativePath, String javaRelativePath, CodeGenerator generator) {
            this.cRelativePath = cRelativePath;
            this.javaRelativePath = javaRelativePath;
            this.generator = generator;
        }
    }

    private GeneratedFileDef[] generatedFileDefs = {
        new GeneratedFileDef("common/localefallback_data.h", "src/main/java/com/ibm/icu/impl/LocaleFallbackData.java", new ResourceFallbackCodeGenerator()),
    };

    public void setCldrDir(String path) {
        // Use String here since on some systems Ant doesn't support automatically converting Path instances.
        this.cldrPath = checkNotNull(Paths.get(path));
    }

    public void setCOutDir(String path) {
        // Use String here since on some systems Ant doesn't support automatically converting Path instances.
        this.cOutDir = Paths.get(path);
    }

    public void setJavaOutDir(String path) {
        // Use String here since on some systems Ant doesn't support automatically converting Path instances.
        this.javaOutDir = Paths.get(path);
    }

    public void setAction(String action) {
        // Use String here since on some systems Ant doesn't support automatically converting Path instances.
        this.action = action;
    }

    public void execute() throws BuildException {
        for (GeneratedFileDef task : generatedFileDefs) {
            Path cOutPath = cOutDir.resolve(task.cRelativePath);
            Path javaOutPath = javaOutDir.resolve(task.javaRelativePath);

            try {
                if (this.action != null && this.action.equals("clean")) {
                    log("Deleting " + cOutPath + " and " + javaOutPath + "...");
                    Files.deleteIfExists(cOutPath);
                    Files.deleteIfExists(javaOutPath);
                } else {
                    Files.createDirectories(cOutPath.getParent());
                    Files.createDirectories(javaOutPath.getParent());

                    try (PrintWriter cOut = new PrintWriter(new OutputStreamWriter(new FileOutputStream(cOutPath.toFile())));
                         PrintWriter javaOut = new PrintWriter(new OutputStreamWriter(new FileOutputStream(javaOutPath.toFile())))) {

                        log("Generating " + cOutPath + " and " + javaOutPath + "...");
                        task.generator.generateCode(cldrPath, cOut, javaOut);
                    }
                }
            } catch (IOException ioException) {
                throw new BuildException("IOException: " + ioException.toString());
            }
        }
    }
}
