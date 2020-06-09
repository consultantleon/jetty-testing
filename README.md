# Jetty testing
- Sample jetty server + jetty server
- server implements a security handler which checks for a token (no token validation! if no token
   found 401 is issued)
- client implements an AuthFilter which on 401 response requests a token from a (keycloak) auth
    server and repeats the requests

## The JettyConnector has been customised (JettyConnectorCustom)

For synchronous invocations an InputStreamResponseListener is used to support efficient content transfer without the 2MB
restriction of the FutureResponseListener and without the need to switch to async mode as that
proved to work too unreliably

# How to use

- Start AppServer (it'll keep running 'forever' until killed)
- Start AppClient
    this will issue 100x a PUT /api/compute request to the server, each time using
    a fresh client and issuing the request without token (the AuthFilter will fetch a token each time
    and retry the request each time - this test was not made to be efficient but to repeatedly test
    the full scenario of client init - request - acquire token - repeat request)
