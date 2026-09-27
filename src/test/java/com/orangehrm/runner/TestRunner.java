package com.orangehrm.runner;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

/**
 * JUnit entry point that executes the Cucumber feature files.
 * <p>
 * Run the whole suite with:  mvn test
 * Run a single test case with a tag, e.g.:  mvn test -Dcucumber.filter.tags="@TS_LOG_001"
 */
@RunWith(Cucumber.class)
@CucumberOptions(
        features = "src/test/resources/features",
        glue = {"com.orangehrm.stepdefinitions", "com.orangehrm.hooks"},
        plugin = {
                "pretty",
                "summary",
                "html:target/cucumber-reports/cucumber-html-report.html",
                "json:target/cucumber-reports/cucumber.json",
                "junit:target/cucumber-reports/cucumber-junit.xml"
        },
        monochrome = true
)
public class TestRunner {
}
