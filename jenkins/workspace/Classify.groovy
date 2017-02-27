@Grapes([
	  @Grab(group='org.semanticweb.elk', module='elk-owlapi', version='0.4.2'),
          @Grab(group='net.sourceforge.owlapi', module='owlapi-api', version='4.2.3'),
          @Grab(group='net.sourceforge.owlapi', module='owlapi-apibinding', version='4.2.3'),
	  @Grab(group='redis.clients', module='jedis', version='2.6.2'),
          @Grab(group='net.sourceforge.owlapi', module='owlapi-impl', version='4.2.3'),
          @Grab(group='net.sourceforge.owlapi', module='owlapi-parsers', version='4.2.3'),
	])

import org.semanticweb.owlapi.model.parameters.*
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.elk.owlapi.ElkReasonerConfiguration
import org.semanticweb.elk.reasoner.config.*
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.reasoner.*
import org.semanticweb.owlapi.reasoner.structural.StructuralReasoner
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.io.*;
import org.semanticweb.owlapi.owllink.*;
import org.semanticweb.owlapi.util.*;
import org.semanticweb.owlapi.search.*;
import org.semanticweb.owlapi.manchestersyntax.renderer.*;
import org.semanticweb.owlapi.reasoner.structural.*
import groovy.json.*
import redis.clients.jedis.*


ONTDIR = "/home/hohndor/aberowl-meta/aberowl-server/onts/"
REPODIR = "/home/hohndor/aberowl-meta/ontologies/"
String BIO_API_ROOT = 'http://data.bioontology.org/'
String BIO_API_KEY = '24e0413e-54e0-11e0-9d7b-005056aa3316'
List<String> ABEROWL_API = ['http://aber-owl.net/service/api/']

def oid = args[0]
def bpath = REPODIR + oid + "/" // base [path
def slurper = new JsonSlurper()

DB_PREFIX = 'ontos:'
def db = new JedisPool(new JedisPoolConfig(), "localhost").getResource()

def oRec = slurper.parseText(db.get(DB_PREFIX + oid))

if (!oRec.uptodate) {
  println "Classifying ontology..."
  try {
    OWLOntologyManager manager = OWLManager.createOWLOntologyManager()
    OWLOntology ont = manager.loadOntologyFromOntologyDocument(new File(REPODIR+oid+"/new/"+oid+"-raw.owl"))
    OWLDataFactory fac = manager.getOWLDataFactory()
    ConsoleProgressMonitor progressMonitor = new ConsoleProgressMonitor()
    OWLReasonerConfiguration config = new SimpleConfiguration(progressMonitor)
    ElkReasonerFactory f1 = new ElkReasonerFactory()
    OWLReasoner reasoner = f1.createReasoner(ont,config)
    def incon = reasoner.getEquivalentClasses(fac.getOWLNothing()).size() - 1
    oRec.inconsistentClasses = incon
    oRec.unclassifiable = false
  } catch (Exception E) {
    E.printStackTrace()
    oRec.unclassifiable = true
  }
  

  db.set(DB_PREFIX + oid, JsonOutput.toJson(oRec))
  db.close()
  /*
  PrintWriter fout = new PrintWriter(new BufferedWriter(new FileWriter(new File(bpath + "config.json"))))
  fout.println(JsonOutput.toJson(oRec))
  fout.flush()
  fout.close()
  */
} else {
  println "Skipping classification..."
}
