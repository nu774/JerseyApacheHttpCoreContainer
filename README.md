# Java21 Virtual Thread + Apache HttpCore + Jersey example

"Classic" API of Apache HttpCore is built upon blocking I/O, so it can be a good guinea pig for experimenting with Java21 Virtual Thread.
Unfortunately, they don't provide an easy way to swap internal thread executors, so I had to modify a few of the classes in org.apache.hc.core5.http.impl.bootstrap package.
On top of that, I implemented jersey's container spi, and some of simple application resources to play with.

As a result, you can freely block in the JAX-RS resource handlers or inside of StreamingOutput. Full-duplex streaming example like EchoResource or Base64Resource can easily be implemented with classic blocking InputStream/OutputStream, and they can accept tons of concurrent clients thanks to Virtual Thread. No more Suspended or AsyncResponse needed (although it still works).
