

# imapnio
A Java library that supports NIO (Non-blocking I/O) based IMAP clients.

The IMAP NIO client provides a framework to access an IMAP message store using None-blocking I/O mechanism.  This design scales well for thousands of connections per machine and reduces contention when using a large number of threads and CPUs.


## Table of Contents

- [Background](#background)
- [Install](#install)
- [Usage](#usage)
- [Release](#release)
- [Contribute](#contribute)
- [License](#license)


## Background

The traditional accessing IMAP message store uses [JavaMail API](https://www.oracle.com/technetwork/java/javamail/index.html), which requires a blocking I/O. In this case, threads are blocked when performing I/O with the other end. This project was developed to relieve the waiting thread to perform other tasks, and it's design efficiently improves thread utilization to maximize hardware throughput and capacity.

Some of the more distinguishing features of this library are:
- Highly customizable thread model and server/client idle max limit.
- Leverages the well-established framework [Netty](https://netty.io/)
- Future-based design enables a clean separation of IMAP client threads pool versus consumers threads pool. 
- IMAPv4, [RFC3501](https://tools.ietf.org/html/rfc3501) support.
- ID command, [RFC2971](https://tools.ietf.org/html/rfc2971) support.
- IDLE command, [RFC2177](https://tools.ietf.org/html/rfc2177) support
- MOVE command, [RFC6851](https://tools.ietf.org/html/rfc6851) support
- UNSELECT command, [RFC3691](https://tools.ietf.org/html/rfc3691) support

This project is ideal for applications that have a high requirement to optimize thread utilization and improve overall resource capacity. Specifically, this is best for situations where users perform a very high level of sessions and communication with the IMAP server.
 
## Install

This is a Java library. After downloading it, compile it using `mvn clean install`

Then, update your project's pom.xml file dependencies, as follows:

```
  <dependency>
      <groupId>com.yahoo.imapnio</groupId>
      <artifactId>imapnio.core</artifactId>
      <version>5.0.6</version>
  </dependency>
```
Finally, import the relevant classes and use this library according to the usage section below.

- For contributors run deploy to do a push to nexus servers

```
	$ mvn clean deploy -Dgpg.passphrase=[pathPhrase]
```

## Usage

The following code examples demonstrate basic functionality relate to connecting to and communicating with IMAP servers.

### Create a client
```java
  // Create a ImapAsyncClient instance with number of threads to handle the server requests
  final int numOfThreads = 5;
  final ImapAsyncClient imapClient = new ImapAsyncClient(numOfThreads);
```
### Establish a session with an IMAP server
```java
  // Create a new session via the ImapAsyncClient instance created above and connect to that server.  For the illustration purpose, 
  // "imaps://imap.server.com:993" is used
  final URI serverUri = new URI("imaps://imap.server.com:993");
  final ImapAsyncSessionConfig config = new ImapAsyncSessionConfig();
  config.setConnectionTimeoutMillis(5000);
  config.setReadTimeoutMillis(6000);
  final List<String> sniNames = null;

  final InetSocketAddress localAddress = null;
  final Future<ImapAsyncCreateSessionResponse> future = imapClient.createSession(serverUri, config, localAddress, sniNames, DebugMode.DEBUG_OFF);
  
  //this version is a future-based nio client.  Check whether future is done by following code.
  if (future.isDone()) {
	System.out.println("Future is done.");
  }
```

### Execute the IMAP command to IMAP server
Following codes uses a Capability command as an example.

```java
  // Executes the capability command
  final Future<ImapAsyncResponse> capaCmdFuture = session.execute(new CapaCommand());

```

### Handle the response from IMAP server
If the future of the executed command is done, obtain the response.
Following example shows how to read ImapAsyncResponse which wraps the content sent from the server.

```java
  if (capaCmdFuture.isDone()) {
	System.out.println("Capability command is done.");
	final ImapAsyncResponse resp = capaCmdFuture.get(5, TimeUnit.MILLISECONDS);
	final ImapResponseMapper mapper = new ImapResponseMapper();
	final Capability capa = mapper.readValue(resp.getResponseLines().toArray(new IMAPResponse[0]), Capability.class);
	final List<String> values = capa.getCapability("AUTH");
  }
```

## Release

This release, 2.0.x, is a major release.  Changes are:
- It supports non-blocking IO functionality through providing callers with java.util.concurrent.Future object.
- Listener-based non-blocking IO capability will not be supported in this release.

This release, 5.0.x, is a major release.  Changes are:
- It supports request and response bytes counting per command

## Contribute

Please refer to the [contributing.md](Contributing.md) for information about how to get involved. We welcome issues, questions, and pull requests. Pull Requests are welcome.


## Maintainers

Fan Su : fsu@yahooinc.com
Vikram Nagulakonda : nvikram@yahooinc.com


## License

This project is licensed under the terms of the [Apache 2.0](LICENSE-Apache-2.0) open source license. Please refer to [LICENSE](LICENSE) for the full terms.
