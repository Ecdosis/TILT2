A revision of TILT, which was solely a Java applet, by splitting it into 
two halves. The first half handles all image-processing on the server 
using Java, and the second half handles the GUI using Javascript and 
HTML5 (and a canvas).

REQUIREMENTS

TILT requires installation of aspell and libaspell-dev on Ubuntu, or their
equivalents on other platforms.

You must also install AeseSpeller from 
https://github.com/schmidda/AeseSpeller.

For local use you will need to install a web-server. If you use Apache 
and mod-proxy you should add the folllowing line to proxy.conf:

ProxyPass /tilt/ http://localhost:8082/tilt/ retry=0

TILT runs on TCP port 8082, so this should be free of other applications 
listening on that port.

DOCUMENTATION

The full documentation is in dist/javadoc, including the simple API.

TILT requires the HTML 5 library provided by the HTML project in this 
repository space.

The TILT editing GUI is currently under development. The interface for 
this is at http://ecdosis.net/polygon/tilt.html
