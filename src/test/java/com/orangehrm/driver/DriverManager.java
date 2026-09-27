package com.orangehrm.driver;

import com.orangehrm.config.ConfigReader;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * Owns the WebDriver instance lifecycle for a single test thread.
 * <p>
 * A {@link ThreadLocal} is used so the framework is safe to run in parallel
 * (e.g. via Cucumber's parallel JUnit runners or Surefire forking) without
 * tests stepping on each other's browser sessions.
 */
public final class DriverManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(DriverManager.class);
    private static final ThreadLocal<WebDriver> DRIVER_THREAD_LOCAL = new ThreadLocal<>();

    private DriverManager() {
    }

    public static WebDriver getDriver() {
        if (DRIVER_THREAD_LOCAL.get() == null) {
            initDriver();
        }
        return DRIVER_THREAD_LOCAL.get();
    }

    private static void initDriver() {
        ConfigReader config = ConfigReader.getInstance();
        String browser = config.getBrowser().toLowerCase();
        LOGGER.info("Initializing WebDriver for browser: {} (headless={})", browser, config.isHeadless());
        WebDriver driver;

        switch (browser) {
            case "firefox":
                WebDriverManager.firefoxdriver().setup();
                FirefoxOptions firefoxOptions = new FirefoxOptions();
                if (config.isHeadless()) {
                    firefoxOptions.addArguments("-headless");
                }
                driver = new FirefoxDriver(firefoxOptions);
                break;

            case "edge":
                WebDriverManager.edgedriver().setup();
                EdgeOptions edgeOptions = new EdgeOptions();
                if (config.isHeadless()) {
                    edgeOptions.addArguments("--headless=new");
                }
                driver = new EdgeDriver(edgeOptions);
                break;

            case "chrome":
            default:
                WebDriverManager.chromedriver().setup();
                ChromeOptions chromeOptions = new ChromeOptions();
                chromeOptions.addArguments("--remote-allow-origins=*");
                chromeOptions.addArguments("--start-maximized");
                chromeOptions.addArguments("--disable-notifications");
                if (config.isHeadless()) {
                    chromeOptions.addArguments("--headless=new");
                    chromeOptions.addArguments("--window-size=1920,1080");
                }
                driver = new ChromeDriver(chromeOptions);
                break;
        }

        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(config.getImplicitWait()));
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(config.getPageLoadTimeout()));
        if (!config.isHeadless()) {
            driver.manage().window().maximize();
        }

        DRIVER_THREAD_LOCAL.set(driver);
    }

    public static void quitDriver() {
        WebDriver driver = DRIVER_THREAD_LOCAL.get();
        if (driver != null) {
            LOGGER.info("Quitting WebDriver session");
            driver.quit();
            DRIVER_THREAD_LOCAL.remove();
        }
    }
}
