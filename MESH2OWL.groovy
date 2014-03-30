import org.semanticweb.owlapi.io.* 
import org.semanticweb.owlapi.model.*
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary

def onturi = "http://phenomebrowser.net/ontologies/mesh/mesh.owl#"
def bioportaluri = "http://purl.bioontology.org/ontology/RH-MESH/"
def alturi1 = "http://rdf.imim.es/rh-mesh.owl#"

OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
OWLDataFactory factory = manager.getOWLDataFactory()
OWLOntology ontology = manager.createOntology(IRI.create(onturi))

OWLAxiom ax = null

def addAnno = {resource, prop, cont ->
  OWLAnnotation anno = factory.getOWLAnnotation(
    factory.getOWLAnnotationProperty(prop.getIRI()),
    factory.getOWLTypedLiteral(cont))
  def axiom = factory.getOWLAnnotationAssertionAxiom(resource.getIRI(),
						     anno)
  manager.addAxiom(ontology,axiom)
}
def addAnno2 = {resource, prop, cont ->
  OWLAnnotation anno = factory.getOWLAnnotation(
    factory.getOWLAnnotationProperty(prop.getIRI()),
    cont)
  def axiom = factory.getOWLAnnotationAssertionAxiom(resource.getIRI(),
						     anno)
  manager.addAxiom(ontology,axiom)
}




def mainfile = new File("desc2014")
def supplementfile = new File("supp2014.xml")
def treefile = new File("mtrees2014.bin")

def exactMatch = factory.getOWLAnnotationProperty(IRI.create("http://www.w3.org/2004/02/skos/core#exactMatch"))
def pAction = factory.getOWLObjectProperty(IRI.create(onturi+"has-pharmacological-action"))

println "Parsing tree file"
treefile.eachLine { line ->
  def l = line.split(";")
  def label = l[0]
  def id = l[1]
  def cl = factory.getOWLClass(IRI.create(onturi+id))
  addAnno2(cl, exactMatch, IRI.create(bioportaluri+id))
  addAnno2(cl, exactMatch, IRI.create(alturi1+id))
  addAnno(cl, OWLRDFVocabulary.RDFS_LABEL, label)
  if (id.indexOf(".")>-1) {
    def sn = id.substring(0,id.lastIndexOf("."))
    def sup = factory.getOWLClass(IRI.create(onturi+sn))
    ax = factory.getOWLSubClassOfAxiom(cl,sup)
    manager.addAxiom(ontology,ax)
  }
}

println "Parsing main file"
def slurper = new XmlSlurper().parse(mainfile)
slurper.DescriptorRecord.each {rec ->
  OWLClass cl = factory.getOWLClass(IRI.create(onturi+rec.DescriptorUI))
  addAnno2(cl, exactMatch, IRI.create(bioportaluri+rec.DescriptorUI))
  addAnno2(cl, exactMatch, IRI.create(alturi1+rec.DescriptorUI))
  def name = rec.DescriptorName.String.text()
  addAnno(cl, OWLRDFVocabulary.RDFS_LABEL, name)
  /* BEFORE: we make the molecule a SUBCLASS of its pharmacological actions */
  /* NOW (2014): we use an existential restriction from the molecule to its pharmacological actions */
  rec.PharmacologicalActionList.PharmacologicalAction.each { action -> 
    def aid = action.DescriptorReferredTo.DescriptorUI.text()
    OWLClass acl = factory.getOWLClass(IRI.create(onturi+aid))
    ax = factory.getOWLSubClassOfAxiom(cl,factory.getOWLObjectSomeValuesFrom(pAction,acl))
    manager.addAxiom(ontology,ax)
  }
  rec.TreeNumberList.TreeNumber.each { tree -> 
    def aid = tree.text()
    OWLClass acl = factory.getOWLClass(IRI.create(onturi+aid))
    ax = factory.getOWLSubClassOfAxiom(cl,acl)
    manager.addAxiom(ontology,ax)
  }
}


println "Parsing supplement file"
slurper = new XmlSlurper().parse(supplementfile)

//def outfile = new File(args[0])


slurper.SupplementalRecord.each { rec ->
  OWLClass cl = factory.getOWLClass(IRI.create(onturi+rec.SupplementalRecordUI))
  addAnno2(cl, exactMatch, IRI.create(bioportaluri+rec.SupplementalRecordUI))
  addAnno2(cl, exactMatch, IRI.create(alturi1+rec.SupplementalRecordUI))
  def name = rec.SupplementalRecordName.String.text()
  addAnno(cl, OWLRDFVocabulary.RDFS_LABEL, name)
  rec.HeadingMappedToList.HeadingMappedTo.each { superConcept ->
    def sid = superConcept.DescriptorReferredTo.DescriptorUI.text()
    if (sid.startsWith("*")) {
      sid = sid.substring(1)
    }
    def sup = factory.getOWLClass(IRI.create(onturi+sid))
    ax = factory.getOWLSubClassOfAxiom(cl,sup)
    manager.addAxiom(ontology,ax)
  }
}

manager.saveOntology(ontology, IRI.create("file:"+args[0]))
