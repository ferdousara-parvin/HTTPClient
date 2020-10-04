# HttpClientLibrary
COMP 445 -- Lab Assignment 1: Implement a simple HTTP client application

Directives: [Directives.pdf](https://github.com/viveanban/HTTPClient/blob/master/Directives.pdf)

#### Assumptions:
1. URL will be the last argument (and should be enclosed in a quotation mark).
2. All options will come before the URL and can be in any order.
3. URL's written on a Windows command-line needs to have escaped quotation marks.
4. All arguments separated by a space should be enclosed by quotation marks.


#### Test the program with the following:

```
Post with both -d and -f options
java HttpCli post -h Content-Type:application/json -d '{\"Assignment\": 1}' -f C:\Users\Viveka\Desktop\input.txt "http://httpbin.org/post"
```
```
Get with -f option
java HttpCli get -h Content-Type:application/json -f C:\Users\Viveka\Desktop\input.txt "http://httpbin.org/post"
```
```
Post with -d option
java HttpCli post -h Content-Type:application/json -d '{\"Assignment\": 1}' "http://httpbin.org/post"
```

```
Post with -f option
java HttpCli post -h Content-Type:application/json -f C:\Users\tlgmz\Desktop\input.txt "http://httpbin.org/post"
java HttpCli post -h Content-Type:application/json -f C:\Users\Viveka\Desktop\input.txt "http://httpbin.org/post"
```

```
Get with Query Params
java HttpCli get -v https://httpbin.org/get?course=COMP+445
```

```
Get with -o option
java HttpCli get -v -o "C:\Users\tlgmz\Desktop\output.txt" "http://httpbin.org/get?course=networking&assignment=1"
java HttpCli get -v -o "C:\Users\Viveka\Desktop\output.txt" "http://httpbin.org/get?course=networking&assignment=1"
```
```
Get with multiple headers
java HttpCli get -h Content-Type:application/json -h Connection:keep-alive "https://httpbin.org/get?course=COMP+445"
```
```
Get with redirect
java HttpCli get -v "http://www.wikipedia.net/"
java HttpCli get -v http://concordia.ca/
```
