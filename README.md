# aberowl-meta

## Dependencies

* Redis server - Install, start with service redis-server restart or similar. Should work with default config.
* NodeJS and NPM (npm may or may not be bundled with node)
* Groovy
* Apache

## Installation

./install

You will then need to reverse proxy the servers through apache. You can do this by editing the apache config 
file. The relevant one is usually /etc/apache2/sites-enabled/000-default.conf

You need to add:

```apache
  ProxyPass /service/ http://my.domain.name.uk:8080/
  ProxyPassReverse /service/ http://my.domain.name.uk:8080/
  
  ProxyPass / http://my.domain.name.uk:3000/
  ProxyPassReverse / http://my.domain.name.uk:3000/
```

Then, you need to add the module proxy will to apache:
```apache
  sudo a2enmod proxy
```
Finally, you have to install database-redis:
```apache
  1) npm install databank -g redis.
  2) cd node_modules/databank/
     npm install databank-redis
```
Note that the order is important. Apache must be restarted after changes.

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
