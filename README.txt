Java imap nio client that is designed to scale well for thousands of connections per machine and reduce contention when using large number of threads and cpus.

- To build before you submit a PR
$ mvn clean install

- For contibutors run deploy to do a push to nexus servers
$ mvn clean deploy -Dgpg.passphrase=[pathPhrase]

- All Pull requests need to pass continous integration before being merged.
  Please goto https://travis-ci.org/lafaspot/imapnio
  
