@Grapes([
	  @Grab(group='commons-io', module='commons-io', version='2.4'),
          @Grab(group='redis.clients', module='jedis', version='2.5.2'),
	  @Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.7' )
])

import redis.clients.jedis.*
import groovyx.net.http.HTTPBuilder
import java.sql.Timestamp
import java.lang.reflect.Modifier
import groovyx.net.http.ContentType
import org.apache.commons.io.FileUtils
import java.nio.file.*
import java.io.File
import java.util.zip.GZIPInputStream
import groovy.json.*
import redis.clients.jedis.*


ONTDIR = "/home/hohndor/aberowl-meta/aberowl-server/onts/"
REPODIR = "/home/hohndor/aberowl-meta/ontologies/"
String BIO_API_ROOT = 'http://data.bioontology.org/'
String BIO_API_KEY = '24e0413e-54e0-11e0-9d7b-005056aa3316'
String ABEROWL_API = 'http://localhost:30000/api/'

def luceneIndex = "/home/hohndor/aberowl-meta/lucene/"

DB_PREFIX = 'ontos:'
def db = new JedisPool(new JedisPoolConfig(), "localhost").getResource()

def oid = args[0]
def bpath = REPODIR + oid + "/" // base [path
def slurper = new JsonSlurper()
def oRec = slurper.parseText(db.get(DB_PREFIX+oid))

if (!oRec.uptodate ) {
  println "Restarting server..."
  def newDate = oRec.releaseInProgress
  oRec.releaseInProgress = 0
  oRec.lastSubDate = newDate
  def count = oRec.submissions.size() + 1
  oRec.submissions[newDate.toString()] = oid+"_"+count+".ont"
  oRec.uptodate = true

  try {
    Files.move(new File(REPODIR+oid+"/new/"+oid+"-raw.owl").toPath(),new File(REPODIR+oid+"/release/"+oid+"_"+count+".ont").toPath(),StandardCopyOption.REPLACE_EXISTING)
  } catch (Exception E) {

  }

  try {
    ['ln', '-sf', new File(REPODIR+oid+"/release/"+oid+"_"+count+".ont").getAbsolutePath(), new File(REPODIR+oid+"/live/"+oid+".owl")].execute().waitFor()
  } catch (Exception E) {

  }

  db.set(DB_PREFIX + oid, JsonOutput.toJson(oRec))
  db.close()
  new HTTPBuilder().get( uri: ABEROWL_API + 'reloadOntology.groovy', query: [ 'name': oid ] ) { r, s ->
    println "Updated " + oid + ": $s"
  }


}
