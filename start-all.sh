#!/bin/bash                                                                                                                                                                              
redis-server &
cd aberowl-server
export CLASSPATH=.
for i in lib/*; do export CLASSPATH=$CLASSPATH:$i; done
groovy AberOWLServer.groovy 55555 &
cd ..
cd pubmedsearch
groovy JettyServer.groovy &
cd ..
cd rdfsearch
groovy JettySearchBio2RDF.groovy &
cd ..
cd aberowl-web
PORT=3333 node prerender/server.js&
sleep 3
PRERENDER_SERVICE_URL=http://localhost:3333 ./bin/www
