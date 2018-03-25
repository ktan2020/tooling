#!/usr/bin/env python
# # -*- coding: utf-8 -*-

"""
23andme example

"""


import sys
import os
import time 
from selenium import webdriver

from selenium.webdriver.common.by import By
from selenium.webdriver.support.wait import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.support.select import Select



URL = "https://store.23andme.com/en-us/cart/"
MAX_TIMEOUT = 10


TEST_DATA = {
    'fname' : "tom",
    'lname' : "hardy",
    'address' : '253 Todds Lane',
    'city' : 'mountain view',
    'state' : 'CA',
    'zip' : '94040',
    'email' : 'tom.hardy@mail.com',
    'phone' : '254-201-4224',
    'card_no' : '4773840905058513',
    'card_exp' : '01/20',
    'card_cvv' : '123',
}


d = webdriver.Firefox()



try:
    
    d.get(URL)
        
    # Add ancestry kit 
    WebDriverWait(d, MAX_TIMEOUT).until(EC.element_to_be_clickable((By.ID, "button-add-ancestry-kit")))
    d.find_element_by_id("button-add-ancestry-kit").click()
    time.sleep(2)
    
    assert d.find_element_by_id("text-ancestry-kit-count").text == '1'
    assert len(d.find_elements_by_css_selector("li div.js-ancestry-kit-item.cart-item-row > div.item-personalize > div.item-personalize-field > input.js-kit-name")) == 1
    
    # Enter first name 
    d.find_elements_by_css_selector("li div.js-ancestry-kit-item.cart-item-row > div.item-personalize > div.item-personalize-field > input.js-kit-name")[0].send_keys(TEST_DATA['fname'])
    
    
    # Checkout 
    d.execute_script("window.scrollBy(0, 300);")
    d.find_element_by_css_selector("form.js-cart-form > input.submit.button-continue").click()
    
    
    # Step 1: Order Summary 
    WebDriverWait(d, MAX_TIMEOUT).until(lambda _: "Order Summary" in d.page_source)
    d.execute_script("window.scrollBy(0, 300);")
    
    d.find_element_by_id("js-shipping-firstname").send_keys(TEST_DATA['fname'])
    d.find_element_by_id("js-shipping-lastname").send_keys(TEST_DATA['lname'])
    d.find_element_by_id("js-shipping-address").send_keys(TEST_DATA['address'])
    d.find_element_by_id("js-shipping-city").send_keys(TEST_DATA['city'])
    Select(d.find_element_by_id("js-shipping-state")).select_by_value(TEST_DATA["state"])
    d.find_element_by_id("js-shipping-zip").send_keys(TEST_DATA['zip'])
    
    d.find_element_by_id("js-shipping-email").send_keys(TEST_DATA['email'])
    d.find_element_by_id("js-shipping-phone").send_keys(TEST_DATA['phone'])
    
    time.sleep(5)
    d.find_element_by_css_selector("button.spc-next-button").click()
    time.sleep(10)
    
    # XXX addresss verification - ship to unverified address 
    d.find_element_by_css_selector("button.spc-verification-div-button.mod-other").click()
    time.sleep(5)
    
    
    # Step 2. Shipping Method
    d.find_element_by_css_selector("button.spc-next-button.mod-ok").click() 
    time.sleep(5)
    
    
    # Step 3. Billing 
    WebDriverWait(d, MAX_TIMEOUT).until(EC.visibility_of(d.find_element_by_id("js-card-number")))
    d.switch_to.parent_frame()
    d.switch_to.frame("braintree-hosted-field-number")
    d.find_element_by_id("credit-card-number").send_keys(TEST_DATA['card_no'])
    d.switch_to.parent_frame()
    d.switch_to.frame("braintree-hosted-field-expirationDate")
    d.find_element_by_id("expiration").send_keys(TEST_DATA['card_exp'])
    d.switch_to.parent_frame()
    d.switch_to.frame("braintree-hosted-field-cvv")
    d.find_element_by_id("cvv").send_keys(TEST_DATA['card_cvv'])
    d.switch_to.parent_frame()
    
    d.find_element_by_css_selector("button.spc-next-button.mod-ok").click()
    time.sleep(5)
    
    
    # Order Review 
    WebDriverWait(d, MAX_TIMEOUT).until(EC.visibility_of_element_located((By.ID, "js-review")))
    # accept terms of service
    d.find_element_by_css_selector("label.spc-checkbox > input").click()
    d.find_element_by_css_selector("button.spc-summary-accept-button.spc-next-button.mod-ok").click()
    
    time.sleep(5)
    assert "unable to process this credit card" in d.page_source 
    d.save_screenshot("order.png")
    
    
except Exception as e:
    
    print str(e)
    import pdb; pdb.set_trace()    


finally:

    d.quit()
    
    
    




