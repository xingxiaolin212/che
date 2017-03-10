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
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Execute all necessary action for Che gwt compilation.
 *
 * @author Sergii Kabshniuk
 */
//@Mojo(name = "generate", requiresDependencyResolution = ResolutionScope.RUNTIME)
@Mojo(name = "generate",
      defaultPhase = LifecyclePhase.PACKAGE,
      requiresProject = true,
      requiresDependencyCollection = ResolutionScope.RUNTIME)
public class GwtMojo extends AbstractMojo {

//    /**
//     * Project providing artifact id, version and dependencies.
//     */
//    @Parameter(defaultValue = "${project}", readonly = true)
//    private MavenProject project;
//
//    /**
//     * build directory used to write the intermediate bom file.
//     */
//    @Parameter(defaultValue = "${project.build.directory}")
//    private File targetDirectory;
//
//    @Component
//    private MavenProjectHelper projectHelper;
//
//    @Parameter(property = "project.compileClasspathElements", required = true, readonly = true)
//    private List<String> classpath;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Generating GWT");
    }
}
