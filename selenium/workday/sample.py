#!/usr/bin/env python
# # -*- coding: utf-8 -*-

"""
Workday example

"""


import time, sys, argparse, re
from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.common.keys import Keys
from selenium.webdriver.common.action_chains import ActionChains
from selenium.webdriver.support.wait import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.firefox.options import Options



URL = "https://workday.wd5.myworkdayjobs.com/Workday"
MAX_TIMEOUT = 30



parser = argparse.ArgumentParser()
parser.add_argument("-r", metavar="REGEX", action="store", help="Regex term to search with. Case insensitive.")
parser.add_argument("--phantom", action="store_true", help="Use PhantomJS as browser.")
args = parser.parse_args()

if args.r == None:
    sys.stderr.write("WARN: no regex term was specified. I will return all listings! This could be huge ... \n")
    sys.stderr.write("\n")
    regexp = None
else:
    regexp = re.compile(args.r, re.I) 

if args.phantom:
    d = webdriver.PhantomJS()
else:    
    o = Options()
    o.set_headless(True)
    d = webdriver.Firefox(options=o)
d.maximize_window()
d.get(URL)

WebDriverWait(d, MAX_TIMEOUT).until(EC.visibility_of_element_located((By.CSS_SELECTOR, "div[data-automation-id='faceted_search']")))


try:

    l = 0
    while True:
        
        l = len(d.find_elements_by_xpath(".//li[@data-automation-id='compositeContainer']"))
        print " ... # of rows read: %d ... " % l
        
        d.execute_script(("window.scrollTo(0, document.body.scrollHeight);"))

        WebDriverWait(d, MAX_TIMEOUT).until(EC.invisibility_of_element_located((By.ID, "spinnerContainer")))

        time.sleep(3)
        
        # no more new elements    
        if l == len(d.find_elements_by_xpath(".//li[@data-automation-id='compositeContainer']")):
            break
    
    print
    print "Total # of 'compositeContainer' nodes: [%s]" % l
    print "Total # of jobs: <<< %s >>>" % d.find_element_by_css_selector("span[id='wd-FacetedSearchResultList-PaginationText-facetSearchResultList.newFacetSearch.Report_Entry']").text
    print
    
    desc = map(lambda x: x.text, d.find_elements_by_css_selector("div#monikerList[data-automation-id='compositeHeader']"))
    loc = map(lambda x: x.text, d.find_elements_by_css_selector("div#monikerList[data-automation-id='compositeHeader'] + span[data-automation-id='compositeSubHeaderOne']"))
    
    assert len(desc) == len(loc)
    
    
    for _ in zip(desc, loc):
        if regexp:
            if re.search(regexp, _[0]) or re.search(regexp, _[1]):
                print _
        else:
            print _
    
except Exception as e:
    
    sys.stderr.write(str(e))
    sys.stderr.write("\n")
    
    import pdb; pdb.set_trace()
    
    
finally:    
    
    d.quit()


print "Done!"
