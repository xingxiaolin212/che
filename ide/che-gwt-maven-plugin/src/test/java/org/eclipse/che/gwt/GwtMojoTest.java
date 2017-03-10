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

import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.plugin.testing.resources.TestResources;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

//ßimport org.testng.annotations.Test;

/**
 * Created by sj on 10.03.17.
 */
//@Listeners(RuleListener.class)
public class GwtMojoTest {

    /**
     * Rule to manage the mojo (inject, get variables from mojo)
     */
    @Rule
    public MojoRule rule = new MojoRule();

    /**
     * Resources of each test mapped on the name of the method
     */
    @Rule
    public TestResources resources = new TestResources();

    /**
     * Helper method used to inject data in mojo
     *
     * @param mojo
     *         the mojo
     * @param baseDir
     *         root dir on which we extract files
     * @throws IllegalAccessException
     *         if unable to set variables
     */
    protected void configure(GwtMojo mojo, File baseDir) throws Exception {
        this.rule.setVariableValueToObject(mojo, "targetDirectory", this.resources.getBasedir(""));
        this.rule.setVariableValueToObject(mojo, "useClassPath", true);
    }

    @Test
    public void testshouldTest() throws Exception {
        File projectCopy = this.resources.getBasedir("project");
        File pom = new File(projectCopy, "pom.xml");
        assertNotNull(pom);
        assertTrue(pom.exists());

        GwtMojo mojo = (GwtMojo)this.rule.lookupMojo("generate", pom);
        configure(mojo, projectCopy);
        mojo.execute();
    }
}
