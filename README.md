

# imapnio
A Java library supporting NIO (Non-blocking I/O) based IMAP client.

IMAP NIO client provides a framework to allow access to an IMAP message store using None-blocking I/O mechanism.  This design scales well for thousands of connections per machine and reduces contention when using a large number of threads and CPUs.


## Table of Contents

- [Background](#background)
- [Install](#install)
- [Configuration](#configuration)
- [Usage](#usage)
- [Contribute](#contribute)
- [License](#license)


## Background

Traditional accessing IMAP message store uses [JavaMail API](https://www.oracle.com/technetwork/java/javamail/index.html), which requires blocking I/O where a thread is blocked when performing I/O with the other end.This project was developed to allow for relieving the waiting thread to perform other tasks.  This design efficiently improves the threads utilization and allows the consuming application to maximize the hardware throughput and capacity.

Following is a list of distinguishing features in this project:
- Highly customizable thread model and server/client idle max limit.
- Leverages the well-established framework - [Netty](https://netty.io/)
- Future-based design enables a clean separation of IMAP client threads pool versus consumers threads pool. 
- IMAPv4, [RFC3501](https://tools.ietf.org/html/rfc3501) support.
- ID command, [RFC2971](https://tools.ietf.org/html/rfc2971) support.
- IDLE command, [RFC2177](https://tools.ietf.org/html/rfc2177) support
- MOVE command, [RFC6851](https://tools.ietf.org/html/rfc6851) support
- UNSELECT command, [RFC3691](https://tools.ietf.org/html/rfc3691) support

This project is ideal for applications that have a high requirement to optimize the threads utilization for improving its overall resource capacity.  Mostly appropriate for consumers performing a great amount of sessions and communications with the IMAP server.

Following external references will establish the foundation for understanding this project:
[Netty](https://netty.io/)
[IMAP4rev1](https://tools.ietf.org/html/rfc3501)

 
## Install

If you've never used git before, please take a moment to familiarize yourself with [what it is and how it works](https://git-scm.com/book/en/v2/Getting-Started-Git-Basics). To install this project, you'll [need to have git installed and set up](https://git-scm.com/book/en/v2/Getting-Started-Installing-Git) on your local development environment.

Install by running the following command.

```
git clone https://github.com/lafaspot/imapnio.git
```

This will create a directory called imapnio and download the contents of this repo to it.


## Configuration
This is a Java library.  After downloading it,  compile it using the following command.
$ mvn clean install

Then update your project's pom.xml file dependencies, as follows:
```
  <dependency>
      <groupId>com.github.krdev.imapnio</groupId>
      <artifactId>imapnio.core</artifactId>
      <version>1.0.23</version>
  </dependency>
```
Finally, import the relevant classes and use this library according to the usage section below.

- For contributors run deploy to do a push to nexus servers
```
$ mvn clean deploy -Dgpg.passphrase=[pathPhrase]
```

## Usage

Following code examples provide a list of usages on how to connect and communicate with any IMAP server.

### Create a client
```java
  // Create a IMAPClient instance with number of threads to handle the server requests
  final int numOfThreadsServed = 5;
  final IMAPClient imapClient = new IMAPClient(numOfThreadsServed);
```
### Establish a session with an IMAP server
```java
  // Create a new session via the imap client created above and connect to that server.  For the illustration purpose, 
  // "imaps://imap.server.com:993" is used
  final IMAPSession session = imapClient.createSession(new URI("imaps://imap.server.com:993"), new GenericListener(), new LogManager());
  // connect to the remote server
  session.connect();
```

### Execute the IMAP command to IMAP server
Following codes uses a Capability command as an example.

```java
  // Instantiates your own IMAPCommandListener instance
  final IMAPCommandListener listener = new MyOwnIMAPCommandListener();
  // fire CAPABILITY command with the tag name you want to track
  final String tagName = "A0001";
  session.executeCapabilityCommand(tagName, listener);
```

### Handle the response from IMAP server
onMessage() method will be called on the registered IMAPCommandListener.  
Following example shows how to read IMAPResponse sent from the server.

```java
  @Override
  public void onMessage(final IMAPSession session, final IMAPResponse response) {
    System.out.println("Response from IMAPServer is==>tag:" + response.getTag() + ",getRest():"
        + response.getRest() + ",toString():" + response.toString());
  }
```


## Contribute

Please refer to the [contributing.md](Contributing.md) for information about how to get involved. We welcome issues, questions, and pull requests. Pull Requests are welcome.


## Maintainers

Luis Alves: lafa@verizonmedia.com


## License

This project is licensed under the terms of the [Apache 2.0](LICENSE-Apache-2.0) open source license. Please refer to [LICENSE](LICENSE) for the full terms.
