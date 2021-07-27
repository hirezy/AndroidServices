
package com.bnd.andserver.plugin;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;
import com.bnd.andserver.annotation.AppInfo;
import com.bnd.andserver.plugin.util.Constants;

import org.apache.commons.io.FileUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.util.Random;

import javax.lang.model.element.Modifier;

public class AppInfoGenerator extends DefaultTask {

    private static final char[] CHARS = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o',
        'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'};

    private File outputDir;
    private String appId;
    private String packageName;

    public AppInfoGenerator() {
    }

    public void setOutputDir(File outputDir) {
        this.outputDir = outputDir;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    @TaskAction
    public void generate() throws Exception {
        FileUtils.deleteDirectory(outputDir);

        Random random = new Random();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            int index = random.nextInt(CHARS.length);
            builder.append(CHARS[index]);
        }

        AnnotationSpec annotation = AnnotationSpec.builder(AppInfo.class)
            .addMember("value", "$S", appId)
            .build();

        TypeSpec adapterClass = TypeSpec.classBuilder(AndServerPlugin.capitalize(builder.toString()))
            .addJavadoc(Constants.DOC_EDIT_WARN)
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addAnnotation(annotation)
            .build();

        JavaFile javaFile = JavaFile.builder(packageName, adapterClass).build();
        javaFile.writeTo(outputDir);
    }

}