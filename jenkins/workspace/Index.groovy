@Grapes([
          @Grab(group='javax.servlet', module='javax.servlet-api', version='3.1.0'),
          @Grab(group='org.eclipse.jetty', module='jetty-server', version='9.3.0.M2'),
          @Grab(group='org.eclipse.jetty', module='jetty-servlet', version='9.3.0.M2'),
          @Grab(group='redis.clients', module='jedis', version='2.5.2'),
          @Grab(group='com.google.code.gson', module='gson', version='2.3.1'),
          @Grab(group='org.apache.lucene', module='lucene-queryparser', version='5.2.1'),
          @Grab(group='com.googlecode.json-simple', module='json-simple', version='1.1.1'),
          @Grab(group='org.slf4j', module='slf4j-log4j12', version='1.7.10'),

	  @Grab(group='org.semanticweb.elk', module='elk-owlapi', version='0.4.2'),
          @Grab(group='net.sourceforge.owlapi', module='owlapi-api', version='4.2.3'),
          @Grab(group='net.sourceforge.owlapi', module='owlapi-apibinding', version='4.2.3'),
          @Grab(group='net.sourceforge.owlapi', module='owlapi-impl', version='4.2.3'),
          @Grab(group='net.sourceforge.owlapi', module='owlapi-parsers', version='4.2.3'),

	  @Grab(group='com.google.guava', module='guava', version='19.0'),

          @Grab(group='org.codehaus.gpars', module='gpars', version='1.1.0'),
          @Grab(group='org.apache.lucene', module='lucene-core', version='5.2.1'),
          @Grab(group='org.apache.lucene', module='lucene-analyzers-common', version='5.2.1'),
          @Grab(group='aopalliance', module='aopalliance', version='1.0'),
	  @GrabConfig(systemClassLoader=true)
	])
@Grab(group='javax.el', module='javax.el-api', version='3.0.0')

import groovy.json.*

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

import org.apache.lucene.analysis.*
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.analysis.core.WhitespaceAnalyzer
import org.apache.lucene.document.*
import org.apache.lucene.index.*
import org.apache.lucene.store.*
import org.apache.lucene.util.*
import org.apache.lucene.search.*
import org.apache.lucene.queryparser.*
import org.apache.lucene.queryparser.simple.*
import org.apache.lucene.search.highlight.*
import org.apache.lucene.index.IndexWriterConfig.OpenMode

import java.nio.file.*


void reloadOntologyIndex(String oid, IndexWriter index, def oRec) {

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

  index.deleteDocuments(new Term('ontology', oid))

  // Add record for the ontology itself
  def oDoc = new Document()
  // Storing seperate lower case versions of the field seems dumb
  oDoc.add(new Field('ontology', oid, TextField.TYPE_STORED))
  oDoc.add(new Field('lontology', oid.toLowerCase(), TextField.TYPE_STORED))
  oDoc.add(new Field('type', 'ontology', TextField.TYPE_STORED))
  oDoc.add(new Field('name', oRec.name, TextField.TYPE_STORED))
  oDoc.add(new Field('lname', oRec.name.toLowerCase(), TextField.TYPE_STORED))
  if (oRec.description) {
    oDoc.add(new Field('ldescription', oRec.description.toLowerCase(), TextField.TYPE_STORED))
    oDoc.add(new Field('description', oRec.description, TextField.TYPE_STORED))
  }
  index.addDocument(oDoc)

  // Readd all classes for this ont

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
    def doc = new Document()
    Field f = null
    //To indicate that it is a old version
    f = new Field('ontology', oid, TextField.TYPE_STORED)
    doc.add(f)
    // make ontologies searchable
    f = new Field('AberOWL-catch-all', oid.toLowerCase(), TextField.TYPE_STORED)
    doc.add(f)
    f = new Field('type', 'class', TextField.TYPE_STORED)
    doc.add(f)
    f = new Field('class', cIRI, TextField.TYPE_STORED)
    doc.add(f)

    /* get the axioms */
    EntitySearcher.getSuperClasses(iClass, iOnt).each { cExpr -> // OWL Class Expression
      //if (! cExpr.isClassExpressionLiteral()) {
      f = new Field('AberOWL-subclass', manSyntaxRenderer.render(cExpr), TextField.TYPE_STORED)
      doc.add(f)
      f = new Field('AberOWL-catch-all', manSyntaxRenderer.render(cExpr), TextField.TYPE_STORED)
      doc.add(f)
      //}
    }
    EntitySearcher.getEquivalentClasses(iClass, iOnt).each { cExpr -> // OWL Class Expression
      //if (! cExpr.isClassExpressionLiteral()) {
      f = new Field('AberOWL-equivalent', manSyntaxRenderer.render(cExpr), TextField.TYPE_STORED)
      doc.add(f)
      f = new Field('AberOWL-catch-all', manSyntaxRenderer.render(cExpr), TextField.TYPE_STORED)
      doc.add(f)
      //}
    }
    EntitySearcher.getDisjointClasses(iClass, iOnt).each { cExpr -> // OWL Class Expression
      //if (! cExpr.isClassExpressionLiteral()) {
      f = new Field('AberOWL-disjoint', manSyntaxRenderer.render(cExpr), TextField.TYPE_STORED)
      doc.add(f)
      f = new Field('AberOWL-catch-all', manSyntaxRenderer.render(cExpr), TextField.TYPE_STORED)
      doc.add(f)
      //}
    }


    def deprecated = false
    def annoMap = [:].withDefault { new TreeSet() }
    EntitySearcher.getAnnotations(iClass, iOnt).each { anno ->
      if (anno.isDeprecatedIRIAnnotation()) {
	deprecated = true
      }
      def aProp = anno.getProperty()
      if (!(aProp in labels || aProp in definitions || aProp in synonyms)) {
	if (anno.getValue() instanceof OWLLiteral) {
	  def aVal = anno.getValue().getLiteral()?.toLowerCase()
	  def aLabels = []
	  if (EntitySearcher.getAnnotations(aProp, iOnt, df.getRDFSLabel()).size() > 0) {
	    EntitySearcher.getAnnotations(aProp, iOnt, df.getRDFSLabel()).each { l ->
	      def lab = l.getValue().getLiteral().toLowerCase()
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
	f = new Field(k, val, TextField.TYPE_STORED)
	doc.add(f)
	f = new Field("AberOWL-catch-all", val, TextField.TYPE_STORED)
	doc.add(f)
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
    if (oboId.length() > 0) {
      oboId = oboId.replaceAll("_", ":").toLowerCase()
      f = new Field('oboid', oboId, StringField.TYPE_STORED)
      doc.add(f)
    }


    def xrefs = []
    /* this was a workaround, bug should be fixed now in the ontologies
       EntitySearcher.getAnnotationAssertionAxioms(iClass, iOnt).each {
       if(it.getProperty().getIRI() == new IRI('http://www.geneontology.org/formats/oboInOwl#hasDbXref')) {
       it.getAnnotations().each {
       def label = it.getValue().getLiteral().toLowerCase()
       if(!xrefs.contains(label)) {
       xrefs << label
       }
       }
       }
       }
    */
    synonyms.each {
      EntitySearcher.getAnnotationAssertionAxioms(iClass, iOnt).each { ax ->
	if (ax.getProperty() == it) {
	  //	EntitySearcher.getAnnotations(iClass, iOnt, it).each { annotation -> // OWLAnnotation
	  if (ax.getValue() instanceof OWLLiteral) {
	    def val = (OWLLiteral) ax.getValue()
	    def label = val.getLiteral().toLowerCase()

	    f = new Field('synonym', label, TextField.TYPE_STORED)
	    doc.add(f)
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
	    def label = val.getLiteral().toLowerCase()
	    if (label) {
	      f = new Field('label', label, TextField.TYPE_STORED)
	      doc.add(f)
	      hasLabel = true
	      if (firstLabelRun) {
		lastFirstLabel = label;
	      }
	    }
	  }
	}
      }
      if (lastFirstLabel) {
	f = new Field('first_label', lastFirstLabel, TextField.TYPE_STORED)
	doc.add(f)
	firstLabelRun = false
      }
    }
    definitions.each {
      EntitySearcher.getAnnotations(iClass, iOnt, it).each { annotation -> // OWLAnnotation
	if (annotation.getValue() instanceof OWLLiteral) {
	  def val = (OWLLiteral) annotation.getValue()
	  def label = val.getLiteral().toLowerCase()
	  f = new Field('definition', label, TextField.TYPE_STORED)
	  doc.add(f)
	  if (annotation != null) {
	    //	    dCount += 1
	  }
	}
      }
    }
    if (!hasLabel) {
      f = new Field('label', iClass.getIRI().getFragment().toString().toLowerCase(), TextField.TYPE_STORED)
      doc.add(f) // add remainder
    }
    if (!lastFirstLabel) {
      f = new Field('first_label', iClass.getIRI().getFragment().toString().toLowerCase(), TextField.TYPE_STORED)
      doc.add(f)
    }
    if (!deprecated) {
      index.addDocument(doc)
    }
  }

  iOnt.getObjectPropertiesInSignature(true).each { iClass ->
    def cIRI = iClass.getIRI().toString()
    def firstLabelRun = true
    def lastFirstLabel = null
    def doc = new Document()
    doc.add(new Field('ontology', oid, TextField.TYPE_STORED))
    doc.add(new Field('class', cIRI, TextField.TYPE_STORED))

    def xrefs = []
    EntitySearcher.getAnnotationAssertionAxioms(iClass, iOnt).each {
      if (it.getProperty().getIRI() == new IRI('http://www.geneontology.org/formats/oboInOwl#hasDbXref')) {
	it.getAnnotations().each {
	  def label = it.getValue().getLiteral().toLowerCase()
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
	def aVal = anno.getValue().getLiteral()?.toLowerCase()
	def aLabels = []
	if (EntitySearcher.getAnnotations(aProp, iOnt, df.getRDFSLabel()).size() > 0) {
	  EntitySearcher.getAnnotations(aProp, iOnt, df.getRDFSLabel()).each { l ->
	    def lab = l.getValue().getLiteral().toLowerCase()
	    annoMap[lab].add(aVal)
	  }
	} else {
	  annoMap[aProp.toString()].add(aVal)
	}
      }
    }

    annoMap.each { k, v ->
      v.each { val ->
	doc.add(new Field(k, val, TextField.TYPE_STORED))
      }
    }

    labels.each {
      EntitySearcher.getAnnotations(iClass, iOnt, it).each { annotation -> // OWLAnnotation
	if (annotation.getValue() instanceof OWLLiteral) {
	  def val = (OWLLiteral) annotation.getValue()
	  def label = val.getLiteral().toLowerCase()

	  if (!xrefs.contains(label)) {
	    doc.add(new Field('label', label, TextField.TYPE_STORED))
	    if (firstLabelRun) {
	      lastFirstLabel = label;
	    }
	  }
	}
      }

      if (lastFirstLabel) {
	doc.add(new Field('first_label', lastFirstLabel, TextField.TYPE_STORED))
	firstLabelRun = false
      }
    }
    definitions.each {
      EntitySearcher.getAnnotations(iClass, iOnt, it).each { annotation -> // OWLAnnotation
	if (annotation.getValue() instanceof OWLLiteral) {
	  def val = (OWLLiteral) annotation.getValue()
	  def label = val.getLiteral().toLowerCase()

	  doc.add(new Field('definition', label, TextField.TYPE_STORED))
	  if (annotation != null) {
	    //	    dCount += 1
	  }
	}
      }
    }

    doc.add(new Field('label', iClass.getIRI().getFragment().toString().toLowerCase(), TextField.TYPE_STORED)) // add remainder
    if (!lastFirstLabel) {
      doc.add(new Field('first_label', iClass.getIRI().getFragment().toString().toLowerCase(), TextField.TYPE_STORED))
    }
    index.addDocument(doc)
  }
}



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
  println "Reindexing..."
  // Index things
  Directory index = new NIOFSDirectory(new File(luceneIndex).toPath())
  IndexSearcher searcher
  IndexWriterConfig iwc
  IndexWriter writer
  
  iwc = new IndexWriterConfig(new WhitespaceAnalyzer())
  iwc.setOpenMode(OpenMode.CREATE_OR_APPEND)
  writer = new IndexWriter(index, iwc)
  
  reloadOntologyIndex(oid, writer, oRec)
} else {
  println "Skipping indexing..."
}

