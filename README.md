# HttpClientLibrary
COMP 445 -- Lab Assignment 1: Implement a simple HTTP client application

Directives: [Directives.pdf](https://github.com/viveanban/HTTPClient/blob/master/Directives.pdf)

Test the program with the following:

```
post -h Content-Type:application/json -d '{"Assignment": 1}' http://httpbin.org/post
```

```
post -h Content-Type:application/json -f C:\Users\tlgmz\Desktop\test.txt http://httpbin.org/post
```

```
get -v https://httpbin.org/get?course:COMP+445
```

```
get -v -o "C:\Users\tlgmz\Desktop\test.txt" http://httpbin.org/get?course=networking&assignment=1
```