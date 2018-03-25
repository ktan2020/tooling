
package soapui.adyen

import org.apache.commons.lang3.*
import groovy.time.*
import groovy.util.logging.Slf4j

import static com.codeborne.selenide.Selenide.*
import static com.codeborne.selenide.Condition.*
import static com.codeborne.selenide.Selectors.*
import static com.codeborne.selenide.WebDriverRunner.*

import com.codeborne.selenide.*
import org.openqa.selenium.*
import org.openqa.selenium.support.ui.*



@Slf4j
class Utils {
  
  
    static def urlContains(String fraction) {
        return new ExpectedCondition<Boolean>() {
            //private String currentUrl = ""
            
            @Override
            public Boolean apply(WebDriver driver) {
                def currentUrl = driver.getCurrentUrl()
                return currentUrl != null && currentUrl.contains(fraction)
            }
            
            @Override
            public String toString() {
                //return String.format("url to contain \"%s\". Current url: \"%s\"", fraction, currentUrl)
                "url to contain \"${fraction}\"."
            }
        }
    }
    
    static def urlDoesNotContain(String fraction) {
        return new ExpectedCondition<Boolean>() {
            //private String currentUrl = ""
            
            @Override
            public Boolean apply(WebDriver driver) {
                def currentUrl = driver.getCurrentUrl()
                return currentUrl != null && !currentUrl.contains(fraction)
            }
            
            @Override
            public String toString() {
                //return String.format("url to contain \"%s\". Current url: \"%s\"", fraction, currentUrl)
                "url not to contain \"${fraction}\"."
            }
        }
    }
    
    
    static def url_to_map(String URL) {
        def q = new URI(URL.trim().split(":")[1]).query
        def m = q?.split("&").inject([:]) { map, kv ->
            def _ = kv.split("=")
            map[_[0]] = _[1]
            map 
        }
        println "q: ${q}, m: ${m}"
        m
    }

 
  
  
    /**
     *
     */
    static def sofo_auth(url, bic, account, pin) {
        log.info("[ url:${url}, bic:${bic}, account:${account}, pin:${pin} ]")
        
        def timeStart = new Date()
        def (q, m) = ["",null]
        
        
        if (SystemUtils.IS_OS_LINUX) {
            // Workflow for Linux via python script (headless mode)  
            def proc = ["${System.getenv('FRAMEWORK_HOME')}" + "/bin/adyen_sofo_auth", "${url}", "${bic}", "${account}", "${pin}"].execute()
            proc.in.eachLine {
                println it
                if (it.contains("[Redirect URL]")) { 
                    q = new URI(it.replace("[Redirect URL]:","").trim().split(":")[1]).query
                    m = q.split("&").inject([:]) { map, kv ->
                        def _ = kv.split("=")
                        map[_[0]] = _[1]
                        map 
                    }
                    println "q: ${q}, m: ${m}"
                }
            } 
            proc.waitFor()
        } else {
            // For Windows and Mac machines use Selenide 
            m = _sofo_auth_(url, bic, account, pin)
        }
        
        println "${TimeCategory.minus(new Date(), timeStart)}"
        log.info("Done!")
        
        return m['pspReference']
    }
    
    private static def _sofo_auth_(url, bic, account, pin) {
        Configuration.browser = "marionette"
        
        def ff_path = null
        
        // Set firefox binary path 
        if (SystemUtils.IS_OS_MAC) {
            ff_path = "${System.getenv('FIREFOX_HOME') + File.separatorChar + 'Firefox.app' + File.separatorChar + 'Contents' + File.separatorChar + 'MacOS' + File.separatorChar + 'firefox'}"
        } else if (SystemUtils.IS_OS_WINDOWS) {
            ff_path = "${System.getenv('FIREFOX_HOME') + File.separatorChar + 'firefox.exe'}"
        } 
        
        log.info("@@@@ Setting <webdriver.firefox.bin> to [${ff_path}] @@@@")
        System.setProperty("webdriver.firefox.bin", ff_path)
        
               
        open(url)
        
        $(byId("MultipaysSessionSenderCountryId")).waitUntil(visible, Configuration.timeout*3)
        
        $(byId("BankCodeSearch")).shouldBe(visible)
        $(byId("BankCodeSearch")).setValue(bic).pressEnter()
        
        $(byId("BackendFormLOGINNAMEUSERID")).waitUntil(visible, Configuration.timeout*3)
        $(byId("BackendFormLOGINNAMEUSERID")).setValue(account)
        $(byId("BackendFormUSERPIN")).setValue(pin).pressEnter()
        
        sleep(1_000) // XXX
        
        $(byCssSelector("input#account-1")).waitUntil(visible, Configuration.timeout*3)
        $(byCssSelector("input#account-1")).click()
        $(byCssSelector(".button-right.primary.has-indicator")).click() // have to click button here not just enter key
        
        $(byId("BackendFormTan")).waitUntil(visible, Configuration.timeout*3)
        $(byId("BackendFormTan")).setValue("12345").pressEnter()
        
        sleep(Configuration.timeout) // XXX
        Wait().until(urlContains("pspReference"))
        
        def m = url_to_map(WebDriverRunner.url())  
        close()
        
        return m  
    }

    
    /**
     *
     */
    static def ideal_auth(url) {
        log.info("[ url:${url} ]")
        
        def timeStart = new Date()
        def (q, m) = ["",null]
        
        
        if (SystemUtils.IS_OS_LINUX) {
            // Workflow for Linux via python script (headless mode) 
            def proc = ["${System.getenv('FRAMEWORK_HOME')}" + "/bin/adyen_ideal_auth", "${url}"].execute()
            proc.in.eachLine {
                println it
                if (it.contains("[Redirect URL]")) { 
                    q = new URI(it.replace("[Redirect URL]:","").trim().split(":")[1]).query
                    m = q.split("&").inject([:]) { map, kv ->
                        def _ = kv.split("=")
                        map[_[0]] = _[1]
                        map 
                    }
                    println "q: ${q}, m: ${m}"
                }
            } 
            proc.waitFor()
        } else {
            // For Windows and Mac machines use Selenide 
            m = _ideal_auth_(url)
        }
        
        println "${TimeCategory.minus(new Date(), timeStart)}"
        log.info("Done!")
        
        return m['pspReference']
    } 
    
    private static def _ideal_auth_(url) {
        Configuration.browser = "marionette"
        
        def ff_path = null
        
        // Set firefox binary path 
        if (SystemUtils.IS_OS_MAC) {
            ff_path = "${System.getenv('FIREFOX_HOME') + File.separatorChar + 'Firefox.app' + File.separatorChar + 'Contents' + File.separatorChar + 'MacOS' + File.separatorChar + 'firefox'}"
        } else if (SystemUtils.IS_OS_WINDOWS) {
            ff_path = "${System.getenv('FIREFOX_HOME') + File.separatorChar + 'firefox.exe'}"
        } 
        
        log.info("@@@@ Setting <webdriver.firefox.bin> to [${ff_path}] @@@@")
        System.setProperty("webdriver.firefox.bin", ff_path)
        
               
        open(url)
        
        $(byCssSelector("input.btnLink")).shouldBe(visible)
        
        $(byCssSelector("input.btnLink")).click()
        
        Wait().until(urlContains("pspReference"))
        
        def m = url_to_map(WebDriverRunner.url())    
        close()
     
        return m
    }
        
    
    /**
     * 
     */
    static def giropay_auth(url,name,acct_no,bank_id,sc,esc) {
        log.info("[ url:${url}, name:${name}, acct_no:${acct_no}, bank_id:${bank_id}, sc:${sc}, esc:${esc} ]")
        
        def timeStart = new Date()
        def (q, m) = ["",null]
        
        
        if (SystemUtils.IS_OS_LINUX) {
            // Workflow for Linux via python script (headless mode) 
            def proc = ["${System.getenv('FRAMEWORK_HOME')}" + "/bin/adyen_giropay_auth", "${url}", "${name}", "${acct_no}", "${bank_id}", "${sc}", "${esc}"].execute()
            proc.in.eachLine {
                println it
                if (it.contains("[Redirect URL]")) { 
                    q = new URI(it.replace("[Redirect URL]:","").trim().split(":")[1]).query
                    m = q.split("&").inject([:]) { map, kv ->
                        def _ = kv.split("=")
                        map[_[0]] = _[1]
                        map 
                    }
                    println "q: ${q}, m: ${m}"
                }
            } 
            proc.waitFor()
        } else {
            // For Windows and Mac machines use Selenide 
            m = _giro_auth_(url, name, acct_no, bank_id, sc, esc) 
        }
        
        println "${TimeCategory.minus(new Date(), timeStart)}"
        log.info("Done!")
        
        return m['pspReference']
    }
    
    
    /**
     *
     */
    static def giropay_auth(url,name,iban,sc,esc) {
        log.info("[ url:${url}, name:${name}, iban:${iban}, sc:${sc}, esc:${esc} ]")
        
        def timeStart = new Date()
        def (q, m) = ["",null]
        

        if (SystemUtils.IS_OS_LINUX) {
            // Workflow for Linux via python script (headless mode)    
            def proc = ["${System.getenv('FRAMEWORK_HOME')}" + "/bin/adyen_giropay_auth", "${url}", "${name}", "${iban}", "${sc}", "${esc}"].execute()
            proc.in.eachLine {
                println it
                if (it.contains("[Redirect URL]")) { 
                    q = new URI(it.replace("[Redirect URL]:","").trim().split(":")[1]).query
                    m = q.split("&").inject([:]) { map, kv ->
                        def _ = kv.split("=")
                        map[_[0]] = _[1]
                        map 
                    }
                    println "q: ${q}, m: ${m}"
                }
            } 
            proc.waitFor()
        } else {
            // For Windows and Mac machines use Selenide 
            m = _giro_auth_(url, name, iban, sc, esc)
        }
        
        println "Elapsed time: ${TimeCategory.minus(new Date(), timeStart)}"
        log.info("Done!")
        
        return m['pspReference']
    }
    
    
    private static def _giro_auth_(url,name,acct_no,bank_id,sc,esc) {
        Configuration.browser = "marionette"
        
        def ff_path = null
        
        // Set firefox binary path 
        if (SystemUtils.IS_OS_MAC) {
            ff_path = "${System.getenv('FIREFOX_HOME') + File.separatorChar + 'Firefox.app' + File.separatorChar + 'Contents' + File.separatorChar + 'MacOS' + File.separatorChar + 'firefox'}"
        } else if (SystemUtils.IS_OS_WINDOWS) {
            ff_path = "${System.getenv('FIREFOX_HOME') + File.separatorChar + 'firefox.exe'}"
        } 
        
        log.info("@@@@ Setting <webdriver.firefox.bin> to [${ff_path}] @@@@")
        System.setProperty("webdriver.firefox.bin", ff_path)
        
               
        open(url)
        
        $(byId("giropay.accountHolderName")).shouldBe(visible)
        
        $(byId("giropay.accountHolderName")).setValue(name)
        $(byId("giropay.bankAccountNumber")).setValue(acct_no)
        $(byId("giropay.bankLocationId")).setValue(bank_id)
        $(byId("mainSubmit")).submit()
        
        $(byName("sc")).waitUntil(visible, Configuration.timeout)
        $(byName("extensionSc")).waitUntil(visible, Configuration.timeout)
        
        $(byName("sc")).setValue(sc)
        $(byName("extensionSc")).setValue(esc)
        $(byXpath(".//input[@type='submit']")).click()
        
        Wait().until(urlContains("pspReference"))
                
        def m = url_to_map(WebDriverRunner.url())   
        close()        
                
        return m
    }
    
    
    private static def _giro_auth_(url,name,iban,sc,esc) {
        Configuration.browser = "marionette"
        
        def ff_path = null
        
        // Set firefox binary path 
        if (SystemUtils.IS_OS_MAC) {
            ff_path = "${System.getenv('FIREFOX_HOME') + File.separatorChar + 'Firefox.app' + File.separatorChar + 'Contents' + File.separatorChar + 'MacOS' + File.separatorChar + 'firefox'}"
        } else if (SystemUtils.IS_OS_WINDOWS) {
            ff_path = "${System.getenv('FIREFOX_HOME') + File.separatorChar + 'firefox.exe'}"
        } 
        
        log.info("@@@@ Setting <webdriver.firefox.bin> to [${ff_path}] @@@@")
        System.setProperty("webdriver.firefox.bin", ff_path)
        
               
        open(url)
        
        $(byId("giropay.accountHolderName")).shouldBe(visible)
        
        $(byId("giropay.accountHolderName")).setValue(name)
        $(byId("giropay.iban")).setValue(iban)
        $(byId("mainSubmit")).submit()
        
        $(byName("sc")).waitUntil(visible, Configuration.timeout)
        $(byName("extensionSc")).waitUntil(visible, Configuration.timeout)
        
        $(byName("sc")).setValue(sc)
        $(byName("extensionSc")).setValue(esc)
        $(byXpath(".//input[@type='submit']")).click()
        
        Wait().until(urlContains("pspReference"))
                
        def m = url_to_map(WebDriverRunner.url())
        close()        
                
        return m
    }
    
    
}
