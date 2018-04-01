#!/usr/bin/env python
# # -*- coding: utf-8 -*-

"""
Context menu example

"""


import time
from selenium import webdriver
from selenium.webdriver.common.keys import Keys
from selenium.webdriver.common.alert import Alert
from selenium.webdriver.common.action_chains import ActionChains


d = webdriver.Firefox()
d.get('https://swisnl.github.io/jQuery-contextMenu/demo.html')

# context click on "right click me" button
ActionChains(d).context_click(d.find_element_by_css_selector('span.context-menu-one.btn.btn-neutral')).perform()

# select "Cut" option
d.find_element_by_css_selector('li.context-menu-item.context-menu-icon.context-menu-icon-cut').click()

time.sleep(2)

# dismiss Alert box
assert "cut" in Alert(d).text 
Alert(d).dismiss()

# context click again 
ActionChains(d).context_click(d.find_element_by_css_selector('span.context-menu-one.btn.btn-neutral')).perform()

# select "Paste" option
d.find_element_by_css_selector('li.context-menu-item.context-menu-icon.context-menu-icon-paste').click()

time.sleep(2)

# dismiss Alert box
assert "paste" in Alert(d).text
Alert(d).dismiss()

d.quit()
