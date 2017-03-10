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
package org.junit.runners;

import org.junit.Rule;
import org.junit.rules.RunRules;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ITestResult;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Created by sj on 10.03.17.
 */
public class RuleListener implements IInvokedMethodListener {
    private static final Logger LOG = LoggerFactory.getLogger(RuleListener.class);

    @Override
    public void beforeInvocation(IInvokedMethod method, ITestResult testResult) {
        if (method.isTestMethod()) {
            Object testInstance = method.getTestMethod().getInstance();
            TestClass testClass = new TestClass(testInstance.getClass());
            List<TestRule> result = testClass.getAnnotatedMethodValues(testInstance,
                                                                       Rule.class, TestRule.class);

            result.addAll(testClass.getAnnotatedFieldValues(testInstance,
                                                            Rule.class, TestRule.class));

            Method testMethod = method.getTestMethod().getConstructorOrMethod().getMethod();
            Description descr = Description.createTestDescription(testClass.getJavaClass(),
                                                                  method.getTestMethod().getMethodName(),
                                                                  testMethod
                                                                          .getAnnotations());


            RunRules runRules = new RunRules(new Statement() {
                @Override
                public void evaluate() throws Throwable {

                }
            }, result, descr);
            try {
                runRules.evaluate();
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }


        }

    }

    @Override
    public void afterInvocation(IInvokedMethod method, ITestResult testResult) {

    }
}
