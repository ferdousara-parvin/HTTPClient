# Server.HttpServerLibrary
COMP 445 -- Lab Assignment 2: Implement a simple HTTP server application and build a simple remote file manager on top of the HTTP protocol in the server side.

#### Assumptions:
1. URL will be in the format: [<protocol>://]<host>/<path>


#### Test the program with the following:

```
Httpfs:
-v -p 80 -d /
-v
-p 678
-p 80 -d /src
-d C://Users//tlgmz//Desktop
```

```
HttpCli:
  Command line:
  post -v -d '{\"Assignment\": 2}' http://localhost:8080/test/hello.txt
  get http://localhost:8080/test
  get http://localhost:8080/test/hello.txt
```
