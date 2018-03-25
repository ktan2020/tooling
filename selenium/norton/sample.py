#!/usr/bin/env python
# # -*- coding: utf-8 -*-

"""
https://us.norton.com/
https://buy.norton.com/estore/checkOut

"""

import sys, random, time
from datetime import date
from collections import namedtuple
import unittest 
from selenium import webdriver 
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.support.select import Select


URL = "https://buy.norton.com/ps?selSKU=21351058&ctry=US&lang=en&tppc=2CEBCA86-1C27-46A2-2295-DA7F5ED757EF&ptype=cart&trf_id=nortoncom"
MAX_TIMEOUT = 10


customer_details = namedtuple('customer_details', [
	'cardholder_name',
	'card_number',
	'expMonth',
	'expYear',
	'cvv',
	'email_address',
	'password',
	'address',
	'postal_code',
	'city',
	'state',
])


def highlight_element(driver, element, duration = 5):
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



class mytest(unittest.TestCase):
	
	def setUp(self):
		self.driver = webdriver.Firefox()
		
		self.customer = customer_details(
			cardholder_name = "dick tracy",
			card_number = "4111111111111111",
			expMonth = "%02d" % date.today().month,
			expYear = date.today().year+2,
			cvv = "123",
			email_address = "".join([ chr(65+int(random.uniform(1,26))) for i in range(1,10)]) + "@mail.com",
			password = "Test1234%",
			address = "123 somewhere st",
			postal_code = "95111",
			city = "SAN JOSE",
			state = "CA",
		)
		
		
	def tearDown(self):
		if sys.exc_info()[0]:
			self.driver.save_screenshot("%s.png" % self._testMethodName)
		self.driver.quit()	
	
	
	def test_place_dummy_order(self):
		driver = self.driver
		driver.get(URL)

		
		# Proceed to Checkout
		driver.find_element_by_css_selector("input#checkout.toBilling").click()
		
		WebDriverWait(driver, MAX_TIMEOUT).until(EC.visibility_of_element_located((By.ID, "registerToggleButton")))
		driver.find_element_by_id("registerToggleButton").click()
		
		time.sleep(random.randint(MAX_TIMEOUT/2,MAX_TIMEOUT))
		driver.find_element_by_id("registerUsername").send_keys(self.customer.email_address)
		time.sleep(random.randint(MAX_TIMEOUT/2,MAX_TIMEOUT))
		driver.find_element_by_id("confirmEmailAddress").send_keys(self.customer.email_address)
		time.sleep(random.randint(MAX_TIMEOUT/2,MAX_TIMEOUT))
		driver.find_element_by_id("registerPassword").send_keys(self.customer.password)
		time.sleep(random.randint(MAX_TIMEOUT/2,MAX_TIMEOUT))
		
		driver.find_element_by_id("btn-sign-up").click()
		
		
		# Billing Information page 
		WebDriverWait(driver, MAX_TIMEOUT).until(EC.visibility_of_element_located((By.CSS_SELECTOR, "input.paymentMethodCode")))
		driver.find_element_by_css_selector("input.paymentMethodCode").click()
		
		time.sleep(10)
		driver.find_element_by_id("cardHolderName").send_keys(self.customer.cardholder_name)
		driver.find_element_by_id("cardNumber").send_keys(self.customer.card_number)

		Select(driver.find_element_by_id("expMonthDropDown")).select_by_value(self.customer.expMonth)
		time.sleep(1)
		Select(driver.find_element_by_id("expYearDropDown")).select_by_value(self.customer.expYear)
		time.sleep(1)
		driver.find_element_by_id("cvv").send_keys(self.customer.cvv)
		time.sleep(1)
		
		driver.find_element_by_id("address").send_keys(self.customer.address)
		time.sleep(1)
		driver.find_element_by_id("zipCode").send_keys(self.customer.postal_code)
		time.sleep(1)
		driver.find_element_by_id("city").send_keys(self.customer.city)
		time.sleep(1)
		Select(driver.find_element_by_id("stateDropDown")).select_by_value(self.customer.state)
		
		
		# Next button
		driver.find_element_by_css_selector("a#billingInfoNextButton > input.enterBilling").click()
		
		
		# Agree & Place Your Order page 
		WebDriverWait(driver, MAX_TIMEOUT).until(EC.visibility_of_element_located((By.ID, "confirmSubmit")))
		driver.find_element_by_id("confirmSubmit").click()
		
		assert "unable to process" in driver.page_source




