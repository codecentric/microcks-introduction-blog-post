package de.codecentric.microcks_demo;

import org.junit.platform.suite.api.IncludeClassNamePatterns;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectPackages("de.codecentric.microcks_demo.tests")
@IncludeClassNamePatterns("de.codecentric.microcks_demo.tests.*Test")
public class TestSuite {
    // NOOP
}