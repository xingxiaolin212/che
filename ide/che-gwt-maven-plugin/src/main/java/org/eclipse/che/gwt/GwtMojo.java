/*******************************************************************************
 * Copyright (c) 2012-2017 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.gwt;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;
import java.util.List;

/**
 * Execute all necessary action for Che gwt compilation.
 *
 * @author Sergii Kabshniuk
 */
//@Mojo(name = "generate", requiresDependencyResolution = ResolutionScope.RUNTIME)
@Mojo(name = "generate",
      defaultPhase = LifecyclePhase.PACKAGE,
      requiresProject = true,
      requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class GwtMojo extends AbstractMojo {

    /**
     * Project providing artifact id, version and dependencies.
     */
//    @Parameter(property = "project", defaultValue = "${project}", readonly = true, required = true)
//    protected MavenProject project;

    /**
     * build directory used to write the intermediate bom file.
     */
    @Parameter(defaultValue = "${project.build.directory}")
    private File targetDirectory;

//    @Component
//    private MavenProjectHelper projectHelper;

    @Parameter(property = "project.compileClasspathElements", required = true, readonly = true)
    private List<String> classpath;

    /**
     * Use of classpath instead of classloader
     */
    private boolean useClassPath;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Generating GWT");
       // getLog().info("project - " + project);
        getLog().info("targetDirectory - " + targetDirectory);
       // getLog().info("projectHelper - " + projectHelper);
        getLog().info("classpath - " + classpath);
        getLog().info("useClassPath - " + useClassPath);
    }

    /**
     * Allow to configure generator to use classpath instead of classloader
     *
     * @param useClassPath
     *         true if want to use classpath loading
     */
    public void setUseClassPath(boolean useClassPath) {
        this.useClassPath = useClassPath;
    }
}
