

# IMAP NIO client in Java
Java IMAP nio client that is designed to scale well for thousands of connections per machine and reduce contention when using large number of threads and cpus.


## Table of Contents

- [Install](#install)
- [Usage](#usage)
- [Contribute](#contribute)
- [License](#license)

## Install

This is packaged as a java library and can be imported as maven or gradle dependency in your project. There are no special installation instructions.
$ mvn clean install

- For contibutors run deploy to do a push to nexus servers
$ mvn clean deploy -Dgpg.passphrase=[pathPhrase]


## Usage

Compile and use it as a regular IMAP client.

```java
    // Create a new session
    final IMAPSession session = theClient.createSession(new URI("imaps://imap.server.com:993"), new GenericListener(), logManager);
    // connect to the remote server
    session.connect();
    // fire OAUTH2 command
    session.executeOAuth2Command(...);
```

## Contribute

Please refer to the [contributing.md](Contributing.md) for information about how to get involved. We welcome issues, questions, and pull requests. Pull Requests are welcome.

## Maintainers

Luis Alves: (lafa at verizonmedia.com)

## License

This project is licensed under the terms of the [Apache 2.0](LICENSE-Apache-2.0) open source license. Please refer to [LICENSE](LICENSE) for the full terms.
