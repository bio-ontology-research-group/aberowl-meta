@Grab(group='redis.clients', module='jedis', version='2.5.2')

import groovy.json.*
import redis.clients.jedis.*


String DB_PREFIX_ORIG = 'ontologies:'
String DB_PREFIX = 'ontos:'
def db = new JedisPool(new JedisPoolConfig(), "localhost")

def ont = args[0]
def target = args[1]

def d = db.getResource()
def id = DB_PREFIX_ORIG + ont
def item = d.get(id)
JsonSlurper slurper = new JsonSlurper()
def rec = slurper.parseText(item)
rec.submissions = {}
rec.lastSubDate = 0

d.set(DB_PREFIX + ont, JsonOutput.toJson(rec))
d.close()
/*
PrintWriter fout = new PrintWriter(new BufferedWriter(new FileWriter(new File(target + "/config.json"))))
fout.println(JsonOutput.toJson(rec))
fout.flush()
fout.close()
*/
