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

def query = """
{
  "settings" : {
    "index" : {
      "number_of_shards" : 5,
      "number_of_replicas" : 0,
      "mapping.total_fields.limit": 100000
    }
  },
  "mappings" : {
    "owlclass" : {
      "properties" : {
        "ontology" : { "type" : "keyword" },
        "oboid" : { "type" : "keyword" },
        "class" : { "type" : "keyword" }
      }
    },
    "property" : {
      "properties" : {
        "ontology" : { "type" : "keyword" },
        "oboid" : { "type" : "keyword" },
        "class" : { "type" : "keyword" }
      }
    },
    "ontology" : {
      "properties" : {
        "ontology" : { "type" : "keyword" },
        "oboid" : { "type" : "keyword" }
      }
    }
  }
}
"""

/*
def query = """
{   
    "ontology": "ABD",
    "AberOWL-catch-all": [
        "abd",
        "<div id='man-owlclass' data-iri='http://brd.bsvgateway.org/api/organism/'>Organism</div>",
        "Organism"
    ],
    "type": "class",
    "class": "http://brd.bsvgateway.org/api/organism/?id=390",
    "AberOWL-subclass": [
        "<div id='man-owlclass' data-iri='http://brd.bsvgateway.org/api/organism/'>Organism</div>"
    ],
    "rdfs:subClassOf": [
        "Organism"
    ],
    "oboid": "id=390",
    "first_label": "Onchocerca"
    }
"""
*/

println( ["curl", "-X", "DELETE", "10.81.0.162:9200/aberowl/?pretty"].execute().text )
println( ["curl", "-X", "PUT", "10.81.0.162:9200/aberowl/?pretty", "-H", "Content-Type: application/json", "-d", "${query}"].execute().text )


//def response = ["curl", "-XPUT", "10.81.0.162:9200/aberowl/?pretty", "-H", "'Content-Type: application/json'", "-d'", "'"+query+"'"].execute().text
