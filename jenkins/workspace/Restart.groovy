@Grapes([
  @Grab(group='commons-io', module='commons-io', version='2.4'),
  @Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.7' )
])

import groovyx.net.http.HTTPBuilder
import java.sql.Timestamp
import java.lang.reflect.Modifier
import groovyx.net.http.ContentType
import org.apache.commons.io.FileUtils
import java.nio.file.*
import java.io.File
import java.util.zip.GZIPInputStream
import groovy.json.*

ONTDIR = "/home/hohndor/aberowl-meta/aberowl-server/onts/"
REPODIR = "/home/hohndor/aberowl-meta/ontologies/"
String BIO_API_ROOT = 'http://data.bioontology.org/'
String BIO_API_KEY = '24e0413e-54e0-11e0-9d7b-005056aa3316'
List<String> ABEROWL_API = ['http://aber-owl.net/service/api/']

def luceneIndex = "/home/hohndor/aberowl-meta/lucene/"

def oid = args[0]
def bpath = REPODIR + oid + "/" // base [path
def slurper = new JsonSlurper()
def oRec = slurper.parse(new File(bpath + "config.json"))

if (!oRec.uptodate) {
  println "Restarting server..."
  def newDate = oRec.releaseInProgress
  oRec.releaseInProgress = 0
  oRec.lastSubDate = newDate
  def count = oRec.submissions.size() + 1
  oRec.submissions[newDate.toString()] = oid+"_"+count+".ont"
  
  Files.move(new File(REPODIR+oid+"/new/"+oid+"-raw.owl").toPath(),new File(REPODIR+oid+"/release/"+oid+"_"+count+".ont").toPath(),StandardCopyOption.REPLACE_EXISTING)
  
  PrintWriter fout = new PrintWriter(new BufferedWriter(new FileWriter(new File(bpath + "config.json"))))
  fout.println(JsonOutput.toJson(oRec))
  fout.flush()
  fout.close()
}
