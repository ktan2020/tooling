
import os, sys, time
from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.support.wait import WebDriverWait

MAX_TIMEOUT = 5

URL = "https://ca-test.adyen.com/ca/ca/login.shtml"
d = webdriver.Firefox()


try:
    d.get(URL)

    d.find_element_by_id("accountTextInput").send_keys("XXXXX")
    d.find_element_by_id("j_username").send_keys("YYYYY")
    d.find_element_by_id("j_password").send_keys("ZZZZZ")
    d.find_element_by_css_selector("input.csr3-button.primary.util-full-width").click()

    time.sleep(MAX_TIMEOUT)
    d.find_element_by_css_selector("#menu > ul.nav > li:nth-child(2)").click()

    WebDriverWait(d, MAX_TIMEOUT).until(EC.visibility_of_element_located((By.ID, "paymentTable")))
    rows = d.find_elements_by_css_selector("tbody#paymentTable > tr")


    print "# of rows: %s" % len(rows)

    for row in rows:
     
        print " | ".join(map(lambda x: x.text.strip() if x else "", row.find_elements_by_xpath(".//td")))

except:
    import pdb; pdb.set_trace()
    
finally:    
    d.quit()
