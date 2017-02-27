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

def url = 'http://10.81.0.162:9200'
def http = new HTTPBuilder(url)

def builder = new groovy.json.JsonBuilder()

String[] fields = ['label', 'ontology', 'oboid', 'definition', 'synonym', 'AberOWL-catch-all', 'AberOWL-subclass', 'AberOWL-equivalent']

Map boostVals = ['label'             : 100,
		 'ontology'          : 1000, // when ontology is added to query, sort by ontology
		 'oboid'             : 10000, // definitely want the matching id returned first when searching for ID
		 'definition'        : 3,
		 'synonym'           : 75,
		 'AberOWL-subclass'  : 25, // less than synonym/label, but more than definition
		 'AberOWL-equivalent': 25, // less than synonym/label, but more than definition
		 'AberOWL-catch-all' : 0.01
		]
		
def queryList = []

def oQuery = "entity"
def ontUri = "sio"

oQuery.split().each {
  def omap = [:]
  omap["dis_max"] = [:]
  omap["dis_max"]["queries"] = []
  fields.each { f ->
    def q = [ "match" : [ "${f}" : ["query" : "${it}", "boost" : boostVals[f]]]]
    omap.dis_max.queries << q
  }
  queryList << omap
}

if (ontUri && ontUri != '') {
  queryList << ["match" : ["ontology":"${ontUri}"]]
}

def fQuery = ["query": ["bool":["must":[]]]]
fQuery.from = 0
fQuery.size = 2
queryList.each { 
  fQuery.query.bool.must << it
}

//println new JsonBuilder(fQuery).toPrettyString()
http.post( path: '/aberowl/owlclass/_search', body: new JsonBuilder(fQuery).toPrettyString() ) { resp, reader ->
  def slurper = new JsonSlurper()
  reader.hits.hits.each { println it._source.label }
}

