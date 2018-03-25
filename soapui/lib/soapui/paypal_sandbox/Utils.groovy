
package soapui.paypal_sandbox

import org.apache.commons.lang3.*
import groovy.time.*

import static com.codeborne.selenide.Selenide.*
import static com.codeborne.selenide.Condition.*
import static com.codeborne.selenide.Selectors.*
import static com.codeborne.selenide.WebDriverRunner.*

import com.codeborne.selenide.*
import org.openqa.selenium.*
import org.openqa.selenium.support.ui.*


class Utils {
    
    /**
     * PayerID property will be returned.
     */
    static def get_payer_id(url, username, password) {
        log.info("[ url:${url}, username:${username}, password:${password} ]")
        
        def timeStart = new Date()
        def (q, m) = ["",null]
        
        
        if (SystemUtils.IS_OS_LINUX) {
            // Workflow for Linux via python script (headless mode)
            def proc = ["${System.getenv('FRAMEWORK_HOME')}" + "/bin/paypal_sb_redirect", "${url}", "${username}", "${password}"].execute()
            proc.in.eachLine {
                println it
                if (it.contains("[Redirect URL]")) { 
                    q = new URI(it.replace("[Redirect URL]:","").trim().split(":")[1]).query
                    m = q?.split("&").inject([:]) { map, kv ->
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
            m = _paypal_sandbox_redirect_(url, username, password)
        }
        
        println "Elapsed time: ${TimeCategory.minus(new Date(), timeStart)}"
        log.info("Done!")
        
        return m['PayerID']
    }
    
    
    /**
     * No payer id will be returned
     */
    static def process_transaction(url, username, password) {
        log.info("[ url:${url}, username:${username}, password:${password} ]")
        
        def timeStart = new Date()
        def (q, m) = ["",null]
        def proc
        def returnCode = -1 // return code with any value other than 0 is FAIL
        
        if (SystemUtils.IS_OS_LINUX) {
            // Workflow for Linux via python script (headless mode)
            proc = ["${System.getenv('FRAMEWORK_HOME')}" + "/bin/paypal_sb_redirect", "${url}", "${username}", "${password}"].execute()
            proc.in.eachLine { println it } 
            proc.waitFor()
            returnCode = proc.exitValue()
        } else {
            // For Windows and Mac machines use Selenide 
            _paypal_sandbox_redirect_(url, username, password)
        }
        
        println "Elapsed time: ${TimeCategory.minus(new Date(), timeStart)}"
        log.info("Done!")
        
        return returnCode
    }
    

    private static def _paypal_sandbox_redirect_(String url, String username, String password) {
        def old_timeout = Configuration.timeout
        def (q, m) = ["",null]
        
        // temporarily set default timeout to 60 secs 
        Configuration.timeout = 60_000 
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
            
            
        def urlContains = { String substring ->
            return new ExpectedCondition<Boolean>() {
                @Override
                public Boolean apply(WebDriver driver) {
                    def currentUrl = driver.getCurrentUrl()
                    return currentUrl != null && currentUrl.contains(substring)
                }
                
                @Override
                public String toString() {
                    "url contains \"${substring}\"."
                }
            }
        }
        
        def titleDoesNotContain = { String title ->
            return new ExpectedCondition<Boolean>() {
                //private String currentTitle = ""
                
                @Override
                public Boolean apply(WebDriver driver) {
                    //currentTitle = driver.getTitle()
                    def currentTitle = driver.getTitle()
                    return currentTitle != null &&  !currentTitle.contains(title)
                }
                
                @Override
                public String toString() {
                    //"title to contain \"${title}\". Current title is: \"${currentTitle}\""                 
                    "title to contain \"${title}\"."
                }
            }
        }
        
        def url_to_map = { String URL ->
            q = new URI(URL.trim().split(":")[1]).query
            m = q?.split("&").inject([:]) { map, kv ->
                def _ = kv.split("=")
                map[_[0]] = _[1]
                map 
            }
            println "q: ${q}, m: ${m}"
            m
        }
       
        /*
        def wd = WebDriverRunner.getWebDriver()
        def (int w, int h) = [java.awt.Toolkit.getDefaultToolkit().getScreenSize().getWidth(), java.awt.Toolkit.getDefaultToolkit().getScreenSize().getHeight()]
        wd.manage().window().setSize(new Dimension(w, h))
        wd.manage().window().setPosition(new Point(w/2 as int , h/2 as int))
        wd.get(url)
        */
        
        
        open(url)
        
        
        try {
        
            if (WebDriverRunner.source().contains("xptSandbox")) {
                log.info "--- Old PayPal site ---"
                
                $(byId("login_email")).setValue(username)
                $(byId("login_password")).setValue(password)
                
                log.info "Clicking on 'Log In'"
                $(byId("submitLogin")).click()
                
                log.info "Clicking on 'Agree and Pay'"         
                $(byId("continue")).click()
            } else {  
                log.info "+++ New PayPal site +++"
                
                $(byId("injectedUnifiedLogin")).should(exist)
                
                switchTo().frame($(byName("injectedUl")))
                
                $(byId("email")).should(visible)
                $(byId("password")).should(visible)
                
                executeJavaScript("arguments[0].value='${username}';", $("#email")) 
                executeJavaScript("arguments[0].value='${password}';", $("#password"))
                
                sleep(5_000) // !!! Need this to stabilize JS frontend !!!
                
                log.info "Clicking on 'Log In'" 
                $(byId("btnLogin")).click()
                
                switchTo().defaultContent()
                
                $("#spinner-message").should(disappear) // wait for spinner to disappear
                
                log.info "Clicking on 'Agree & Pay'"
                $(byId("confirmButtonTop")).click()
            }
            Wait().until(urlContains("norton.com"))
            
        } catch (e) {
        
            log.severe "XXX Caught an Exception !!! XXX "
            log.severe "Here's the trace: "  + e.toString()
            /*
            sleep(60_000)
            System.console().readLine("Press Enter to continue ...")
            */
            WebDriverRunner.closeWebDriver()
            
        } 
         
        Configuration.timeout = old_timeout
        println "[Redirect URL]: ${WebDriverRunner.url()}; [Title]: ${title()}"
        
        m = url_to_map(WebDriverRunner.url())
        close()

        return m
    }
    
}

