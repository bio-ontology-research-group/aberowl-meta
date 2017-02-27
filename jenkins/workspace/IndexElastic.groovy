@Grapes([
	  @Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.7.1' ),
	  @Grab(group='org.semanticweb.elk', module='elk-owlapi', version='0.4.2'),
          @Grab(group='net.sourceforge.owlapi', module='owlapi-api', version='4.2.3'),
          @Grab(group='redis.clients', module='jedis', version='2.5.2'),
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

import redis.clients.jedis.*

import java.nio.file.*

import org.apache.logging.log4j.*


void reloadOntologyIndex(String oid, def oRec) {

  OWLOntologyManager manager = OWLManager.createOWLOntologyManager()
  OWLOntology ont
  try {
    ont = manager.loadOntologyFromOntologyDocument(new File(REPODIR+oid+"/new/"+oid+"-raw.owl"))
  } catch (Exception E) {
    ont = manager.loadOntologyFromOntologyDocument(new File(REPODIR+oid+"/live/"+oid+".owl"))
  }
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


  //  index.deleteDocuments(new Term('ontology', oid))
  def omap = [:]
  omap.query = [:]
  omap.query.term = [:]
  omap.query.term.ontology = oid
  //  delete(omap)

  // Add record for the ontology itself
  omap = [:]
  omap.ontology = oid
  omap.lontology = oid.toLowerCase()
  omap.type = "ontology"
  omap.name = oRec.name
  omap.lname = oRec.name.toLowerCase()
  if (oRec.description) {
    omap.ldescription = oRec.description.toLowerCase()
    omap.description = oRec.description
  }
  index("ontology", omap)

  // Re-add all classes for this ont

  OWLOntologyImportsClosureSetProvider mp = new OWLOntologyImportsClosureSetProvider(manager, ont)
  OWLOntologyMerger merger = new OWLOntologyMerger(mp, false)
  def iOnt = merger.createMergedOntology(manager, IRI.create("http://test.owl"))

  // set up the renderer for the axioms
  def sProvider = new AnnotationValueShortFormProvider(
    Collections.singletonList(df.getRDFSLabel()),
    Collections.<OWLAnnotationProperty, List<String>> emptyMap(),
    manager);
  def manSyntaxRenderer = new AberOWLSyntaxRendererImpl()
  manSyntaxRenderer.setShortFormProvider(sProvider)

  iOnt.getClassesInSignature(true).each { iClass -> // OWLClass
    def cIRI = iClass.getIRI().toString()
    def firstLabelRun = true
    def lastFirstLabel = null
    def deprecated = false
    oDoc = [:].withDefault { [] }
    oDoc["ontology"] = oid
    oDoc."AberOWL-catch-all" << oid.toLowerCase()
    oDoc."type" ="class"
    oDoc."class" = cIRI

    /* get the axioms */
    EntitySearcher.getSuperClasses(iClass, iOnt).each { cExpr -> // OWL Class Expression
      oDoc."AberOWL-subclass" << manSyntaxRenderer.render(cExpr)
      oDoc.'AberOWL-catch-all' << manSyntaxRenderer.render(cExpr)
    }
    EntitySearcher.getEquivalentClasses(iClass, iOnt).each { cExpr -> // OWL Class Expression
      oDoc."AberOWL-equivalent" << manSyntaxRenderer.render(cExpr)
      oDoc.'AberOWL-catch-all' << manSyntaxRenderer.render(cExpr)
    }
    EntitySearcher.getDisjointClasses(iClass, iOnt).each { cExpr -> // OWL Class Expression
      oDoc."AberOWL-disjoint" << manSyntaxRenderer.render(cExpr)
      oDoc.'AberOWL-catch-all'<< manSyntaxRenderer.render(cExpr)
    }

    def annoMap = [:].withDefault { new TreeSet() }
    EntitySearcher.getAnnotations(iClass, iOnt).each { anno ->
      if (anno.isDeprecatedIRIAnnotation()) {
	deprecated = true
      }
      def aProp = anno.getProperty()
      if (!(aProp in labels || aProp in definitions || aProp in synonyms)) {
	if (anno.getValue() instanceof OWLLiteral) {
	  def aVal = anno.getValue().getLiteral()
	  def aLabels = []
	  if (EntitySearcher.getAnnotations(aProp, iOnt, df.getRDFSLabel()).size() > 0) {
	    EntitySearcher.getAnnotations(aProp, iOnt, df.getRDFSLabel()).each { l ->
	      def lab = l.getValue().getLiteral()
	      annoMap[lab].add(aVal)
	    }
	  } else {
	    annoMap[aProp.toString()?.replaceAll("<", "")?.replaceAll(">", "")].add(aVal)
	  }
	}
      }
    }
    annoMap.each { k, v ->
      v.each { val ->
	oDoc[k] << val
	oDoc."AberOWL-catch-all" << val
      }
    }

    // generate OBO-style ID for the index
    def oboId = ""
    if (cIRI.lastIndexOf("/") > -1) {
      oboId = cIRI.substring(cIRI.lastIndexOf("/") + 1)
    }
    if (cIRI.lastIndexOf("#") > -1) {
      oboId = cIRI.substring(cIRI.lastIndexOf("#") + 1)
    }
    if (cIRI.lastIndexOf('?') > -1) {
      oboId = cIRI.substring(cIRI.lastIndexOf('?') + 1)
    }
    if (oboId.length() > 0) {
      oboId = oboId.replaceAll("_", ":").toLowerCase()
      oDoc."oboid" = oboId
    }


    def xrefs = []
    synonyms.each {
      EntitySearcher.getAnnotationAssertionAxioms(iClass, iOnt).each { ax ->
	if (ax.getProperty() == it) {
	  //	EntitySearcher.getAnnotations(iClass, iOnt, it).each { annotation -> // OWLAnnotation
	  if (ax.getValue() instanceof OWLLiteral) {
	    def val = (OWLLiteral) ax.getValue()
	    def label = val.getLiteral()
	    oDoc."synonym" << label
	  }
	}
      }
    }
    def hasLabel = false
    labels.each {
      EntitySearcher.getAnnotationAssertionAxioms(iClass, iOnt).each { ax ->
	if (ax.getProperty() == it) {
	  //	EntitySearcher.getAnnotations(iClass, iOnt, it).each { annotation -> // OWLAnnotation
	  if (ax.getValue() instanceof OWLLiteral) {
	    def val = (OWLLiteral) ax.getValue()
	    def label = val.getLiteral()
	    if (label) {
	      //"label" "\""+label+"\""
	      oDoc."label" << label
	      hasLabel = true
	      if (firstLabelRun) {
		lastFirstLabel = label;
	      }
	    }
	  }
	}
      }
      if (lastFirstLabel) {
	oDoc."first_label" = lastFirstLabel
	firstLabelRun = false
      }
    }
    definitions.each {
      EntitySearcher.getAnnotations(iClass, iOnt, it).each { annotation -> // OWLAnnotation
	if (annotation.getValue() instanceof OWLLiteral) {
	  def val = (OWLLiteral) annotation.getValue()
	  def label = val.getLiteral()
	  oDoc."definition" << label
	}
      }
    }
    if (!hasLabel) {
      oDoc."label" << iClass.getIRI().getFragment().toString()
    }
    if (!lastFirstLabel) {
      oDoc."first_label" = iClass.getIRI().getFragment().toString()
    }
    if (!deprecated) {
      index("owlclass", oDoc)
    }
  }
  
  iOnt.getObjectPropertiesInSignature(true).each { iClass ->
    def cIRI = iClass.getIRI().toString()
    def firstLabelRun = true
    def lastFirstLabel = null
    oDoc = [:].withDefault { [] }
    oDoc['ontology'] = oid
    oDoc['class'] = cIRI

    def xrefs = []
    EntitySearcher.getAnnotationAssertionAxioms(iClass, iOnt).each {
      if (it.getProperty().getIRI() == new IRI('http://www.geneontology.org/formats/oboInOwl#hasDbXref')) {
	it.getAnnotations().each {
	  def label = it.getValue().getLiteral()
	  if (!xrefs.contains(label)) {
	    xrefs << label
	  }
	}
      }
    }

    def annoMap = [:].withDefault { new TreeSet() }
    EntitySearcher.getAnnotations(iClass, iOnt).each { anno ->
      def aProp = anno.getProperty()
      if (anno.getValue() instanceof OWLLiteral) {
	def aVal = anno.getValue().getLiteral()
	def aLabels = []
	if (EntitySearcher.getAnnotations(aProp, iOnt, df.getRDFSLabel()).size() > 0) {
	  EntitySearcher.getAnnotations(aProp, iOnt, df.getRDFSLabel()).each { l ->
	    def lab = l.getValue().getLiteral()
	    annoMap[lab].add(aVal)
	  }
	} else {
	  annoMap[aProp.toString()].add(aVal)
	}
      }
    }

    annoMap.each { k, v ->
      v.each { val ->
	oDoc[k] << val
      }
    }

    labels.each {
      EntitySearcher.getAnnotations(iClass, iOnt, it).each { annotation -> // OWLAnnotation
	if (annotation.getValue() instanceof OWLLiteral) {
	  def val = (OWLLiteral) annotation.getValue()
	  def label = val.getLiteral()

	  if (!xrefs.contains(label)) {
	    oDoc['label'] << label
	    if (firstLabelRun) {
	      lastFirstLabel = label;
	    }
	  }
	}
      }

      if (lastFirstLabel) {
	oDoc['first_label'] = lastFirstLabel
	firstLabelRun = false
      }
    }
    definitions.each {
      EntitySearcher.getAnnotations(iClass, iOnt, it).each { annotation -> // OWLAnnotation
	if (annotation.getValue() instanceof OWLLiteral) {
	  def val = (OWLLiteral) annotation.getValue()
	  def label = val.getLiteral()

	  oDoc['definition'] << label
	  if (annotation != null) {
	    //	    dCount += 1
	  }
	}
      }
    }

    oDoc['label'] << iClass.getIRI().getFragment().toString()
    if (!lastFirstLabel) {
      oDoc['first_label'] = iClass.getIRI().getFragment().toString()
    }
    index("property", oDoc)
  }


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

DB_PREFIX = 'ontos:'
def db = new JedisPool(new JedisPoolConfig(), "localhost").getResource()

def oRec = slurper.parseText(db.get(DB_PREFIX + oid))
			     //def oRec = slurper.parseText(new File(bpath + "config.json"))

url = 'http://10.81.0.162:9200'
http = new HTTPBuilder(url)

if ( !oRec.uptodate  || true) {
  println "Reindexing..."
  reloadOntologyIndex(oid, oRec)  
} else {
  println "Skipping indexing..."
}

http.shutdown()

def index(def type, def json) {
  
  // delete if exists
  if (type == "owlclass" || type == "property") {
    def m = ["query": ["bool":["must":[]]]]
    def ll = []
    ll << ["term" : ["class" : json["class"]]]
    ll << ["term" : ["ontology" : json["ontology"]]]
    ll.each {
      m.query.bool.must << it
    }
    try {
      http.post( contentType: ContentType.JSON, path:'/aberowl/'+type+'/_delete_by_query', body:new JsonBuilder(m).toString()) { resp, reader ->
      }
    } catch (Exception E) {
      E.printStackTrace()
      //      println m.toString()
    }
  }
  def j = new groovy.json.JsonBuilder(json)
  try {

    http.handler.failure = { resp, reader ->
      [response:resp, reader:reader]
    }
    http.handler.success = { resp, reader ->
      [response:resp, reader:reader]
    }
    def response = http.post( path: '/aberowl/'+type+'/', body: j.toPrettyString() )
    //    println response['reader']
  } catch (Exception E) {
    E.printStackTrace()
    println "Failed: "+j.toPrettyString()
  }
}

def delete(def m) {
  def url = 'http://10.81.0.162:9200'
  def http = new HTTPBuilder(url)
  //  println new JsonBuilder(m).toPrettyString()
  try {
    http.post( contentType: ContentType.JSON, path:'/aberowl/_delete_by_query', body:new JsonBuilder(m).toString() )
  } catch (Exception E) {
    E.printStackTrace()
    println m.toString()
  }
}
