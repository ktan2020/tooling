import SimpleHTTPServer
import SocketServer
import sys
from optparse import OptionParser

class myHandler(SimpleHTTPServer.SimpleHTTPRequestHandler):
   def do_GET(self):
       print self.path
       self.send_response(301)
       new_path = 'http://%s%s'%(o.ip, self.path)
       self.send_header('Location', new_path)
       self.end_headers()

p =  OptionParser()
p.add_option("--ip", dest="ip")
p.add_option("--port", dest="port", type=int, default=8080)
(o,p) = p.parse_args()

if o.ip == None:
    print "XXX FATAL : IP address to redirect to is mandatory! XXX"
    sys.exit(1) 
    
handler = SocketServer.TCPServer(("", o.port), myHandler)
print "serving at port %s" % o.port
handler.serve_forever() 
