/*
 * Copyright 2019 Web3 Labs LTD.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.web3j.console.project;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import picocli.CommandLine;

import static org.web3j.codegen.Console.exitError;
import static org.web3j.codegen.Console.exitSuccess;
import static org.web3j.utils.Collection.tail;

public class ProjectCreator {

    public static final String COMMAND_NEW = "new";
    static final String COMMAND_INTERACTIVE = "interactive";

    final ProjectStructure projectStructure;
    final TemplateProvider templateProvider;

    ProjectCreator(final String root, final String packageName, final String projectName)
            throws IOException {
        this.projectStructure = new ProjectStructure(root, packageName, projectName);
        this.templateProvider =
                new TemplateProvider.Builder()
                        .loadGradlewBatScript("gradlew.bat.template")
                        .loadGradlewScript("gradlew.template")
                        .loadMainJavaClass("Template.java")
                        .loadGradleBuild("build.gradle.template")
                        .loadGradleSettings("settings.gradle.template")
                        .loadGradlewWrapperSettings("gradlew-wrapper.properties.template")
                        .loadGradleJar("gradle-wrapper.jar")
                        .loadSolidityGreeter("Greeter.sol")
                        .withPackageNameReplacement(
                                s -> s.replaceAll("<package_name>", packageName))
                        .withProjectNameReplacement(
                                s -> s.replaceAll("<project_name>", projectName))
                        .build();
    }

    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals(COMMAND_NEW)) {
            args = tail(args);
            if (args.length == 0) {
                final InteractiveOptions options = new InteractiveOptions();
                final List<String> stringOptions = new ArrayList<>();
                stringOptions.add("-n");
                stringOptions.add(options.getProjectName());
                stringOptions.add("-p");
                stringOptions.add(options.getPackageName());
                options.getProjectDestination()
                        .ifPresent(
                                projectDest -> {
                                    stringOptions.add("-o");
                                    stringOptions.add(projectDest);
                                });
                args = stringOptions.toArray(new String[0]);
            }
        }

        CommandLine.run(new ProjectCreatorCLIRunner(), args);
    }

    void generate() {
        try {
            Project.builder()
                    .withProjectStructure(projectStructure)
                    .withTemplateProvider(templateProvider)
                    .build();
            exitSuccess(
                    "Project created with name: "
                            + projectStructure.getProjectName()
                            + " at location: "
                            + projectStructure.getProjectRoot());

        } catch (final Exception e) {
            exitError(e);
        }
    }
}
