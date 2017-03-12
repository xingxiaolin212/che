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
package org.eclipse.che.maven;


import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.plugin.testing.resources.TestResources;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Sergii Kabshniuk
 */
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
    public TestResources resources = new TestResources("src/test/resources/unit", "target/test-projects");

    /**
     * @throws Exception
     *         if any
     */
    @Test
    public void testSomething()
            throws Exception {

        File projectCopy = this.resources.getBasedir("project-to-test");
        File pom = new File(projectCopy, "pom.xml");
        assertNotNull(pom);
        assertTrue(pom.exists());

        GwtMojo gwtMojo = (GwtMojo)rule.lookupMojo("touch", pom);
        assertNotNull(gwtMojo);
        gwtMojo.execute();

    }

}
