
import sys
import re
import unittest 



def luhn_check1(cc_no):
    cc_no = [ int(d) for d in re.sub("[ \t]", "", cc_no) ]
    l,s,flag = len(cc_no),0,0
    for i in range(l-1,-1,-1):
        n = cc_no[i]
        s += sum(divmod((n*2),10)) if flag else n 
        flag = (flag+1) & 1
    return s%10 == 0 

def luhn_check(cc_no):
    cc_no = map(lambda x: int(x), re.sub("[ \t]", "", cc_no))[::-1]
    s = 0
    for i,d in enumerate(cc_no):
        if i%2==1:
            d = d*2-9 if d*2>9 else d*2
        s += d        
    return s%10 == 0
        

    

def main():
    
    print luhn_check(sys.argv[1])

if __name__ == "__main__":
    main()



class _(unittest.TestCase):
    
    def test_valid_numbers(self):
        
        good = [
            "4929985773906955",
            "49927398716",
            "1234567812345670",
            "345887019563059",
            "4539149173996601",
            "378385108533448",
            "4111 1111 1111 1111",
        ]
        
        assert all(map(lambda x: luhn_check(x), good))
        
        
    def test_invalid_numbers(self):
        
        bad = [
            "499273987164",
            "49927398717",
            "1234567812345678",
            "4111 1111 1111 1121",
            "4221 1111 1111 1111",
        ]
        
        assert all(map(lambda x: not luhn_check(x), bad))    

