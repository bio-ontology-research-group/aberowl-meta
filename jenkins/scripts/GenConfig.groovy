@Grab(group='redis.clients', module='jedis', version='2.5.2')

import groovy.json.*
import redis.clients.jedis.*


String DB_PREFIX = 'ontologies:'
def db
db = new JedisPool(new JedisPoolConfig(), "localhost");

def ont = args[0]
def target = args[1]

def d = db.getResource()
def id = DB_PREFIX + ont
def item = d.get(id)
d.close()

PrintWriter fout = new PrintWriter(new BufferedWriter(new FileWriter(new File(target + "/config.json"))))
fout.println(JsonOutput.toJson(item))
fout.flush()
fout.close()
