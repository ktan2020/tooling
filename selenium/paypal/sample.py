#!/usr/bin/env python
# # -*- coding: utf-8 -*-

"""
Paypal sandbox example

"""


import sys
import os
import time 
from selenium import webdriver

from selenium.webdriver.common.by import By
from selenium.webdriver.support.wait import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC


URL = "https://demo.paypal.com/us/demo/navigation?merchant=bigbox&page=merchantHome&device=desktop"
MAX_TIMEOUT = 10


def high_light_element(driver, element, duration = 5):
    import time
    original_style = element.get_attribute('style')

    driver.execute_script("arguments[0].setAttribute(arguments[1], arguments[2])",
        element,
        "style",
        "border: 2px solid red; border-style: dashed;")
    
    if duration > 0:
        time.sleep(duration)
        driver.execute_script("arguments[0].setAttribute(arguments[1], arguments[2])",
            element,
            "style",
            original_style)



d = webdriver.Firefox()

try:
    d.get(URL)

    time.sleep(3)


    # Tour dialogbox
    d.find_element_by_link_text("Skip tour").click()
    time.sleep(3)

    # Main page 
    #WebDriverWait(d, MAX_TIMEOUT).until(EC.visibility_of_element_located((By.CSS_SELECTOR, "div.boxed-content > div.primary.home-page")))

    # Select Black Camera => Black Cameras listing => Black Camera Detail page
    d.find_element_by_css_selector("div#black-camera").click()
    time.sleep(MAX_TIMEOUT)

    # Black Camera detail page 
    d.find_element_by_id("click-category-camera1").click()
    time.sleep(MAX_TIMEOUT)

    # Add Black Camera to cart 
    d.find_element_by_id("click-product-camera1").click()
    #d.find_element_by_link_text("Add to Cart").click()
    time.sleep(MAX_TIMEOUT)

    # shopping cart row 
    price_locator = "div.cart-list.boxed-content.narrow-margin table tr td.cart-price"
    assert d.find_element_by_css_selector(price_locator).text == "$300.00"

    quant_locator = "div.cart-list.boxed-content.narrow-margin table tr td.cart-qty span"
    assert d.find_element_by_css_selector(quant_locator).text == '1'

    total_price_locator = "div.cart-list.boxed-content.narrow-margin table tr td.cart-total"
    assert d.find_element_by_css_selector(total_price_locator).text == "$300.00"

    # proceed to checkout 
    d.find_element_by_id("proceed-to-checkout-btn").click()
    time.sleep(3)

    # checkout as guest 
    d.find_element_by_id("click-merchantLogin-guest").click()
    time.sleep(3)

    # Shipping page
    WebDriverWait(d, MAX_TIMEOUT).until(lambda _: "Shipping" in d.page_source)
    d.find_element_by_id("click-shipping-continue2").click()
    time.sleep(3)

    # Continue
    WebDriverWait(d, MAX_TIMEOUT).until(lambda _: "Please choose a payment method" in d.page_source)
    d.find_element_by_id("click-merchantCreditCard-cont2").click()
    time.sleep(3)

    # Paypal login
    d.find_element_by_css_selector("input.seeDemoButton.highlightable.hotspot.incontextButton").click()
    time.sleep(3)

    # Confirm 
    d.find_element_by_css_selector("input#confirmButtonTop.seeDemoButton.highlightable.hotspot.incontextButton").click()

    # Order complete 
    WebDriverWait(d, MAX_TIMEOUT).until(lambda _: "Confirmation" in d.page_source)
    WebDriverWait(d, MAX_TIMEOUT).until(lambda _: "Your order is complete" in d.page_source)
    order_number = d.find_element_by_css_selector("div.confirmation-details p strong.order-number").text
    
    print "Order Number: %s" % order_number


except:
    print sys.exc_info()[0]
    
    import pdb; pdb.set_trace()
    
    
finally:    
    d.quit()


print "Done!"

