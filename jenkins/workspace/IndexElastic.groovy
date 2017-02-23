@Grapes([
	  @Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.7.1' ),
	  @Grab(group='org.semanticweb.elk', module='elk-owlapi', version='0.4.2'),
          @Grab(group='net.sourceforge.owlapi', module='owlapi-api', version='4.2.3'),
          @Grab(group='net.sourceforge.owlapi', module='owlapi-apibinding', version='4.2.3'),
          @Grab(group='net.sourceforge.owlapi', module='owlapi-impl', version='4.2.3'),
          @Grab(group='net.sourceforge.owlapi', module='owlapi-parsers', version='4.2.3'),
	])

import groovy.json.*
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method
import groovyx.net.http.ContentType

import org.apache.http.client.methods.*
import org.apache.http.entity.*
import org.apache.http.impl.client.*

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

import java.nio.file.*

import org.apache.logging.log4j.*


void reloadOntologyIndex(String oid, def http, def oRec) {

  OWLOntologyManager manager = OWLManager.createOWLOntologyManager()
  OWLOntology ont = manager.loadOntologyFromOntologyDocument(new File(REPODIR+oid+"/new/"+oid+"-raw.owl"))
  OWLDataFactory fac = manager.getOWLDataFactory()
  ConsoleProgressMonitor progressMonitor = new ConsoleProgressMonitor()
  OWLReasonerConfiguration config = new SimpleConfiguration(progressMonitor)
  ElkReasonerFactory f1 = new ElkReasonerFactory()
  OWLReasoner reasoner = f1.createReasoner(ont,config)
  def oReasoner = reasoner
  def df = fac
  
  def labels = [
    df.getRDFSLabel(),
    df.getOWLAnnotationProperty(new IRI('http://www.w3.org/2004/02/skos/core#prefLabel')),
    df.getOWLAnnotationProperty(new IRI('http://purl.obolibrary.org/obo/IAO_0000111'))
  ]
  def synonyms = [
    df.getOWLAnnotationProperty(new IRI('http://www.w3.org/2004/02/skos/core#altLabel')),
    df.getOWLAnnotationProperty(new IRI('http://purl.obolibrary.org/obo/IAO_0000118')),
    df.getOWLAnnotationProperty(new IRI('http://www.geneontology.org/formats/oboInOwl#hasExactSynonym')),
    df.getOWLAnnotationProperty(new IRI('http://www.geneontology.org/formats/oboInOwl#hasSynonym')),
    df.getOWLAnnotationProperty(new IRI('http://www.geneontology.org/formats/oboInOwl#hasNarrowSynonym')),
    df.getOWLAnnotationProperty(new IRI('http://www.geneontology.org/formats/oboInOwl#hasBroadSynonym'))
  ]
  def definitions = [
    df.getOWLAnnotationProperty(new IRI('http://purl.obolibrary.org/obo/IAO_0000115')),
    df.getOWLAnnotationProperty(new IRI('http://www.w3.org/2004/02/skos/core#definition')),
    df.getOWLAnnotationProperty(new IRI('http://purl.org/dc/elements/1.1/description')),
    df.getOWLAnnotationProperty(new IRI('http://www.geneontology.org/formats/oboInOwl#hasDefinition'))
  ]

  def builder = new groovy.json.JsonBuilder()  

  // Add record for the ontology itself
  def oDoc = builder {
    ontology oid
    lontology oid.toLowerCase()
    type "ontology"
    name oRec.name
    lname oRec.name.toLowerCase()
    if (oRec.description) {
      ldescription oRec.description.toLowerCase()
      description oRec.description
    }
  }
  index("ontology", builder)
}


ONTDIR = "/home/hohndor/aberowl-meta/aberowl-server/onts/"
REPODIR = "/home/hohndor/aberowl-meta/ontologies/"
String BIO_API_ROOT = 'http://data.bioontology.org/'
String BIO_API_KEY = '24e0413e-54e0-11e0-9d7b-005056aa3316'
List<String> ABEROWL_API = ['http://aber-owl.net/service/api/']

indexName = "/home/hohndor/aberowl-meta/lucene/"

def oid = args[0]
def bpath = REPODIR + oid + "/" // base [path
def slurper = new JsonSlurper()
def oRec = slurper.parse(new File(bpath + "config.json"))

if (!oRec.uptodate || true ) {
  println "Reindexing..."

  reloadOntologyIndex(oid, http, oRec)  

} else {
  println "Skipping indexing..."
}

def index(def type, def json) {
  def url = 'http://10.81.0.162:9200'
  def http = new HTTPBuilder(url)
  http.post( path: '/aberowl/'+type+'/', body: json.toString() ) { resp ->
      println "POST Success: ${resp.statusLine}"
  }
}
