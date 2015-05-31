# aberowl-meta

## Dependencies

* Redis server - Install, start with service redis-server restart or similar. Should work with default config.
* NodeJS and NPM (npm may or may not be bundled with node)
* Groovy
* Apache

## Installation

./install

You will then need to 

## Running

To run the server:

```bash
  cd aberowl-server
  groovy AberOWLServer.groovy
```

To run the web front-end:

```bash
  cd aberowl-web
  bin/www
```

To run the sync:

```bash
  cd aberowl-sync
  groovy RemoteOntologyDiscover.groovy
  groovy RemoteOntologyUpdate.groovy
```
