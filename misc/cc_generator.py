
import time
import atexit
import sys
import argparse
from getopt import getopt
from selenium import webdriver

URL = "http://card-generator.com/"
d = None


@atexit.register
def exit():
    global d 
    if d: 
        d.quit()


parser = argparse.ArgumentParser()
parser.add_argument("-f", action="store_true", help="Use firefox")
args = parser.parse_args()

if args.f:
    d = webdriver.Firefox()
else:
    d = webdriver.PhantomJS()


def main():
    global d
    
    d.get(URL)
    
    opts = [] 
    
    try:
        opts, args = getopt(sys.argv[1:], "vma")
    except:
        pass
    
    for o,a in opts:
        if o in ("-v"):
            d.find_element_by_link_text("Generate Visa").click()
        elif o in ("-m"):
            d.find_element_by_link_text("Generate Mastercard").click()
        elif o in ("-a"):
            d.find_element_by_link_text("Generate American Express").click()
            
    time.sleep(2)

    card_details = d.find_element_by_css_selector("#votes p.card2").find_elements_by_tag_name("b")
    print tuple(map(lambda x: str(x.text) if x else "", card_details))


if __name__ == "__main__":
    main()
