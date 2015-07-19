AberOWL-meta
=========================

This repository contains a collection of install scripts and subdirectories for setting up the AberOWL framework.

## What is AberOWL?

AberOWL is a framework for ontology-based access to biological data.

## Documentation

Documentation is available at [aber-owl.net](http://aber-owl.net/help).

## Dependencies

### Redis

Redis is an in-memory database that persists on disk, and allows for fast ontoloy indexing.

Follow these simple steps below to compile Redis from source.

```bash
wget http://download.redis.io/redis-stable.tar.gz
tar xvzf redis-stable.tar.gz
cd redis-stable
make
```

And then just simply start Redis with the following command.

```bash
redis-server restart
```

### node, node.js and npm

Node.js is a JavaScript platform for server-side programming, and NPM is a JavaScript package manager.

Ubuntu 14.04 comes bundled with a distro-sable version of node, node,js and nPM that can be installed directly from the default repositories.

Simply run.

```bash
sudo apt-get install node nodejs npm
```

### Groovy

Groovy is an alternative language for the JVM with a concise Java-friendly syntax, dynamic and static features, powerful DSL capabilities

Ubuntu 14.04 comes bundled with Groovy 1.8.6, and can be installed directory from the default repositiories using the following command.

```bash
sudo apt-get install groovy
```

### Apache

Apache is a popular open-source web server.

To install apache, use this command.

```bash
sudo apt-get install apache2
```

## Installation

AberOWL is being actively developed, and you can clone its repository.

Clone the repository:

```bash
git clone https://github.com/bio-ontology-research-group/aberowl-meta
```
And then use the preconfigured install script to automatically set up the service:

```bash
./install
```
You will then need to reverse proxy the servers through apache. You can do this by editing the apache config file.

The relevant one is usually found at  ``/etc/apache2/sites-enabled/000-default.conf``.

You need to add:

    ProxyPassReverse /service/ http://my.domain.name.uk:8080/
    ProxyPass /service/ http://my.domain.name.uk:8080/

    ProxyPass / http://my.domain.name.uk:3000/
    ProxyPassReverse / http://my.domain.name.uk:3000/


Then, you need to add the module proxy to apache:
```bash
sudo a2enmod proxy
```
Finally, you have to install databank-redis:
```bash
cd aberowl-web
npm install databank
cd node_modules/databank/
npm install databank-redis
```

Note that the order is important. Apache must be restarted after changes.

```bash
sudo service apache2 restart
```

## Configuration

Everything should work by default, however listed are a few configuration points throughout the applications:

### AberOWL-sync

* Configure the location of the AberOWL-server API endpoint using the *ABEROWL_API* constant at the top of each
RemoteOntologyDiscover.groovy and RemoteOntologyUpdate.groovy

### AberOWL-server

* The port listened on can be changed at the top of AberOWLServer.groovy. Note that changes will have to be
accounted for in the Apache configuration.

### AberOWL-web

* The port listened on can be changed at the top of bin/www. Note that changes will have to be accounted for in the
Apache configuration.

## Running

Once you are in the ```aberowl-meta``` directory, To run the server simply run:

```bash
cd aberowl-server ;  groovy AberOWLServer.groovy
```

To run the web front-end:

```bash
cd aberowl-web ; bin/www
```

To run the sync, follow these commands:

```bash
cd aberowl-sync ; groovy RemoteOntologyDiscover.groovy & groovy RemoteOntologyUpdate.groovy
```

## Contributions

You can open an issue on our [issues page](https://github.com/bio-ontology-research-group/aberowl-meta/issues), or ask directly on our [mailing list](http://groups.google.co.uk/d/forum/aber-owl).


## License

Code released under the Apache 2.0 license. For more information, please see [the LICENSE file](./LICENSE).
