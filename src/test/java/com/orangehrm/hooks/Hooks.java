package com.orangehrm.hooks;

import com.orangehrm.driver.DriverManager;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cucumber lifecycle hooks shared by every scenario.
 * <p>
 * Runs before/after each scenario (not once per suite) so every scenario
 * gets a fresh, isolated browser session — important for negative and
 * validation scenarios where leftover form state could hide a bug.
 */
public class Hooks {

    private static final Logger LOGGER = LoggerFactory.getLogger(Hooks.class);

    @Before
    public void setUp(Scenario scenario) {
        LOGGER.info("Starting scenario: {}", scenario.getName());
        // Touching getDriver() here (rather than in each step class) guarantees
        // the browser is launched before any step in the scenario runs.
        DriverManager.getDriver();
    }

    @After
    public void tearDown(Scenario scenario) {
        try {
            WebDriver driver = DriverManager.getDriver();
            if (scenario.isFailed() && driver instanceof TakesScreenshot) {
                LOGGER.warn("Scenario failed: {} - attaching failure screenshot", scenario.getName());
                try {
                    byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
                    scenario.attach(screenshot, "image/png", scenario.getName() + "-failure");
                } catch (WebDriverException e) {
                    // Screenshot capture is a best-effort side-effect - a crashed/invalid
                    // driver session here must not mask the original scenario failure or
                    // prevent the driver from being quit below.
                    LOGGER.warn("Failed to capture failure screenshot for scenario: {}", scenario.getName(), e);
                }
            }
        } finally {
            DriverManager.quitDriver();
            LOGGER.info("Finished scenario: {} - status: {}", scenario.getName(), scenario.getStatus());
        }
    }
}
