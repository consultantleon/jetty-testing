# Jetty testing
- Sample jetty server + jetty server
- server implements a security handler which checks for a token (no token validation! if no token
   found 401 is issued)
- client implements an AuthFilter which on 401 response requests a token from a (keycloak) auth
    server and repeats the requests
- requests are sent using jersey async() by AppClient, the main reason is to prevent
    FutureResponseListener which puts a hardcoded 2MB limit on the response size!
- to prevent to run into the same 2MB limit during the AuthFilter.repeatRequest, async() mode
    is used as well there
- as processing freezes in repeatRequest since async() mode is used, a separate repeat client has
    been introduced. Having a separate client for the repeat requests prevents this freezing.test
- the problem of earlyEOF remains, from the moment async() mode is used, requests with a 401 response
    (and no content) randomly start to fail
- conclusion today 20200609: jersey() async mode is unreliable and I can't get my finger on the exact issue
    but there definitely is some MT issue underneath. The only way to process reliably is by removing
    the async mode. The only reason I use async() mode is the hardcoded response size limit of
    FutureResponseListener. Further study leads me to find a way to use InputStreamResponseListener
    in a customised version of JettyConnector.

## Custom JettyConnector with fix for #4476 (JettyConnectorCustom)

Modified version of jersey-jetty-connector 2.30.1 JettyConnector - fixing a small race condition
in anticipation of a fix for https://github.com/eclipse-ee4j/jersey/issues/4476

# How to use

- Start AppServer (it'll keep running 'forever' until killed)
- Start AppClient
    this will issue 100x a PUT /api/compute request to the server, each time using
    a fresh client and issuing the request without token (the AuthFilter will fetch a token each time
    and retry the request each time - this test was not made to be efficient but to repeatedly test
    the full scenario of client init - request - acquire token - repeat request)
