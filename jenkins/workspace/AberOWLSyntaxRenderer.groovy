//package src

import static org.semanticweb.owlapi.manchestersyntax.parser.ManchesterOWLSyntax.*;
import static org.semanticweb.owlapi.util.CollectionFactory.sortOptionally;

import java.io.Writer;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import org.semanticweb.owlapi.manchestersyntax.parser.ManchesterOWLSyntax;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.CollectionFactory;
import org.semanticweb.owlapi.util.ShortFormProvider;
import org.semanticweb.owlapi.vocab.SWRLBuiltInsVocabulary;
import org.semanticweb.owlapi.vocab.XSDVocabulary;
import org.semanticweb.owlapi.manchestersyntax.renderer.AbstractRenderer

public class AberOWLSyntaxRenderer extends AbstractRenderer implements OWLObjectVisitor {

    /**
     * @param writer
     *        writer
     * @param entityShortFormProvider
     *        entityShortFormProvider
     */
    public AberOWLSyntaxRenderer(Writer writer, ShortFormProvider entityShortFormProvider) {
        super(writer, entityShortFormProvider);
    }

    @Nonnull
    protected static <T extends OWLObject> List<T> sort(
        @Nonnull Collection<T> objects) {
        return CollectionFactory.sortOptionally(objects);
    }

    protected void write(@Nonnull Set<? extends OWLObject> objects,
        @Nonnull ManchesterOWLSyntax delimeter, boolean newline) {
        int tab = getIndent();
        pushTab(tab);
        for (Iterator<? extends OWLObject> it = sort(objects).iterator(); it
            .hasNext();) {
            it.next().accept(this);
            if (it.hasNext()) {
                if (newline && isUseWrapping()) {
                    writeNewLine();
                }
                write(delimeter);
            }
        }
        popTab();
    }

    protected void writeCommaSeparatedList(
        @Nonnull Set<? extends OWLObject> objects) {
        for (Iterator<? extends OWLObject> it = sort(objects).iterator(); it.hasNext();) {
            it.next().accept(this);
            if (it.hasNext()) {
                write(", ");
            }
        }
    }

    protected void write(@Nonnull Set<? extends OWLClassExpression> objects,
        boolean newline) {
        boolean first = true;
        for (Iterator<? extends OWLObject> it = sort(objects).iterator(); it
            .hasNext();) {
            OWLObject desc = it.next();
            if (!first) {
                if (newline && isUseWrapping()) {
                    writeNewLine();
                }
                write(" <div id='man-and'>"+ AND + "</div> ");
            }
            first = false;
            if (desc instanceof OWLAnonymousClassExpression) {
                write("(");
            }
            desc.accept(this);
            if (desc instanceof OWLAnonymousClassExpression) {
                write(")");
            }
        }
    }

    private void writeRestriction(
        @Nonnull OWLQuantifiedDataRestriction restriction,
        @Nonnull ManchesterOWLSyntax keyword) {
        restriction.getProperty().accept(this);
        write("<div id='man-keyword'>"+keyword+"</div>");
        restriction.getFiller().accept(this);
    }

    private void writeRestriction(
        @Nonnull OWLQuantifiedObjectRestriction restriction,
        @Nonnull ManchesterOWLSyntax keyword) {
        restriction.getProperty().accept(this);
        write("<div id='man-keyword'>"+keyword+"</div>");
        boolean conjunctionOrDisjunction = false;
        if (restriction.getFiller() instanceof OWLAnonymousClassExpression) {
            if (restriction.getFiller() instanceof OWLObjectIntersectionOf
                || restriction.getFiller() instanceof OWLObjectUnionOf) {
                conjunctionOrDisjunction = true;
                incrementTab(4);
                writeNewLine();
            }
            write("(");
        }
        restriction.getFiller().accept(this);
        if (restriction.getFiller() instanceof OWLAnonymousClassExpression) {
            write(")");
            if (conjunctionOrDisjunction) {
                popTab();
            }
        }
    }

    private <V extends OWLObject> void writeRestriction(
        @Nonnull OWLHasValueRestriction<V> restriction,
        @Nonnull OWLPropertyExpression p) {
        p.accept(this);
        write("<div id='man-hasvalue'>"+VALUE+"</div>");
        restriction.getFiller().accept(this);
    }

    private <F extends OWLPropertyRange> void writeRestriction(
        @Nonnull OWLCardinalityRestriction<F> restriction,
        @Nonnull ManchesterOWLSyntax keyword,
        @Nonnull OWLPropertyExpression p) {
        p.accept(this);
        write("<div id='man-keyword'>"+keyword+"</div>");
        write("<div id='man-number'>"+Integer.toString(restriction.getCardinality())+"</div>");
        writeSpace();
        if (restriction.getFiller() instanceof OWLAnonymousClassExpression) {
            write("(");
        }
        restriction.getFiller().accept(this);
        if (restriction.getFiller() instanceof OWLAnonymousClassExpression) {
            write(")");
        }
    }

    // Class expressions
    @Override
    public void visit(OWLClass ce) {
      write("<div id='man-owlclass' data-iri='"+ce.getIRI().toString()+"'>"+getShortFormProvider().getShortForm(ce)+"</div>");
    }

    @Override
    public void visit(@Nonnull OWLObjectIntersectionOf ce) {
        write(ce.getOperands(), true);
    }

    @Override
    public void visit(@Nonnull OWLObjectUnionOf ce) {
        boolean first = true;
        for (Iterator<? extends OWLClassExpression> it = sortOptionally(ce.getOperands())
            .iterator(); it.hasNext();) {
            OWLClassExpression op = it.next();
            if (!first) {
                write(" <div id='man-or'>" + OR + "</div> ");
            }
            first = false;
            if (op.isAnonymous()) {
                write("(");
            }
            op.accept(this);
            if (op.isAnonymous()) {
                write(")");
            }
        }
    }

    @Override
    public void visit(@Nonnull OWLObjectComplementOf ce) {
      write("<div id='man-not'>" + NOT + "</div>" + ce.isAnonymous() ? " " : "");
        if (ce.isAnonymous()) {
            write("(");
        }
        ce.getOperand().accept(this);
        if (ce.isAnonymous()) {
            write(")");
        }
    }

    @Override
    public void visit(OWLObjectSomeValuesFrom ce) {
        writeRestriction(ce, SOME);
    }

    @Override
    public void visit(OWLObjectAllValuesFrom ce) {
        writeRestriction(ce, ONLY);
    }

    @Override
    public void visit(@Nonnull OWLObjectHasValue ce) {
        writeRestriction(ce, ce.getProperty());
    }

    @Override
    public void visit(@Nonnull OWLObjectMinCardinality ce) {
        writeRestriction(ce, MIN, ce.getProperty());
    }

    @Override
    public void visit(@Nonnull OWLObjectExactCardinality ce) {
        writeRestriction(ce, EXACTLY, ce.getProperty());
    }

    @Override
    public void visit(@Nonnull OWLObjectMaxCardinality ce) {
        writeRestriction(ce, MAX, ce.getProperty());
    }

    @Override
    public void visit(@Nonnull OWLObjectHasSelf ce) {
        ce.getProperty().accept(this);
        write("<div id='man-keyword'>"+SOME+"</div>");
        write("<div id='man-self'>"+SELF+"</div>");
    }

    @Override
    public void visit(@Nonnull OWLObjectOneOf ce) {
        write("<div id='man-set'>{</div>");
        write(ce.getIndividuals(), ONE_OF_DELIMETER, false);
        write("<div id='man-set'>}</div>");
    }

    @Override
    public void visit(OWLDataSomeValuesFrom ce) {
        writeRestriction(ce, SOME);
    }

    @Override
    public void visit(OWLDataAllValuesFrom ce) {
        writeRestriction(ce, ONLY);
    }

    @Override
    public void visit(@Nonnull OWLDataHasValue ce) {
        writeRestriction(ce, ce.getProperty());
    }

    @Override
    public void visit(@Nonnull OWLDataMinCardinality ce) {
        writeRestriction(ce, MIN, ce.getProperty());
    }

    @Override
    public void visit(@Nonnull OWLDataExactCardinality ce) {
        writeRestriction(ce, EXACTLY, ce.getProperty());
    }

    @Override
    public void visit(@Nonnull OWLDataMaxCardinality ce) {
        writeRestriction(ce, MAX, ce.getProperty());
    }

    // Entities stuff
    @Override
    public void visit(OWLObjectProperty property) {
      write("<div title='"+property.getIRI()+"' id='man-property' data-iri='"+property.getIRI()+"'>"+getShortFormProvider().getShortForm(property)+ "</div>");
    }

    @Override
    public void visit(OWLDataProperty property) {
      write("<div title='"+property.getIRI()+"' id='man-property' data-iri='"+property.getIRI()+"'>"+getShortFormProvider().getShortForm(property)+ "</div>");
    }

    @Override
    public void visit(OWLNamedIndividual individual) {
      write("<div title='"+individual.getIRI()+"' id='man-individual' data-iri='"+individual.getIRI()+"'>"+getShortFormProvider().getShortForm(individual)+ "</div>");
    }

    @Override
    public void visit(OWLAnnotationProperty property) {
        write(getShortFormProvider().getShortForm(property));
    }

    @Override
    public void visit(OWLDatatype node) {
        write(getShortFormProvider().getShortForm(node));
    }

    @Override
    public void visit(@Nonnull OWLAnonymousIndividual individual) {
        write(individual.toStringID());
    }

    @Override
    public void visit(@Nonnull IRI iri) {
      write("<div title='"+iri+"' id='man-iri' 'data-iri="+iri.toString()+"'>"+iri.toQuotedString()+ "</div>");
    }

    @Override
    public void visit(@Nonnull OWLAnnotation node) {
        writeAnnotations(node.getAnnotations());
        node.getProperty().accept(this);
        writeSpace();
        node.getValue().accept(this);
    }

    // Data stuff
    @Override
    public void visit(@Nonnull OWLDataComplementOf node) {
        write(NOT);
        if (node.getDataRange().isDatatype()) {
            node.getDataRange().accept(this);
        } else {
            write("(");
            node.getDataRange().accept(this);
            write(")");
        }
    }

    @Override
    public void visit(@Nonnull OWLDataOneOf node) {
        write("{");
        write(node.getValues(), ONE_OF_DELIMETER, false);
        write("}");
    }

    @Override
    public void visit(@Nonnull OWLDataIntersectionOf node) {
        write("(");
        write(node.getOperands(), AND, false);
        write(")");
    }

    @Override
    public void visit(@Nonnull OWLDataUnionOf node) {
        write("(");
        write(node.getOperands(), OR, false);
        write(")");
    }

    @Override
    public void visit(@Nonnull OWLDatatypeRestriction node) {
        node.getDatatype().accept(this);
	write("[");
        write(node.getFacetRestrictions(), FACET_RESTRICTION_SEPARATOR, false);
	write("]");
    }

    @Override
    public void visit(@Nonnull OWLLiteral node) {
        // xsd:decimal is the default datatype for literal forms like "33.3"
        // with no specified datatype
        if (XSDVocabulary.DECIMAL.getIRI().equals(node.getDatatype().getIRI())) {
            write(node.getLiteral());
        } else if (node.getDatatype().isFloat()) {
            write(node.getLiteral());
            write("f");
        } else if (node.getDatatype().isInteger()) {
            write(node.getLiteral());
        } else if (node.getDatatype().isBoolean()) {
            write(node.getLiteral());
        } else {
            pushTab(getIndent());
            writeLiteral(node.getLiteral());
            if (node.hasLang()) {
                write("@");
                write(node.getLang());
            } else if (!node.isRDFPlainLiteral()) {
                write("^^");
                node.getDatatype().accept(this);
            }
            popTab();
        }
    }

    private void writeLiteral(@Nonnull String literal) {
        write("\"");
        for (int i = 0; i < literal.length(); i++) {
            char ch = literal.charAt(i);
            if (ch == '"') {
                write('\\');
            } else if (ch == '\\') {
                write('\\');
            }
            write(ch);
        }
        write("\"");
    }

    @Override
    public void visit(@Nonnull OWLFacetRestriction node) {
        write(node.getFacet().getSymbolicForm());
        writeSpace();
        node.getFacetValue().accept(this);
    }

    // Property expression stuff
    @Override
    public void visit(@Nonnull OWLObjectInverseOf property) {
        write(INVERSE);
        write("(");
        property.getInverse().accept(this);
        write(")");
    }

    // Annotation stuff
    // Stand alone axiom representation
    // We render each axiom as a one line frame
    private boolean wrapSave;
    private boolean tabSave;

    private void setAxiomWriting() {
        wrapSave = isUseWrapping();
        tabSave = isUseTabbing();
        setUseWrapping(false);
        setUseTabbing(false);
    }

    private void restore() {
        setUseTabbing(tabSave);
        setUseWrapping(wrapSave);
    }

    @Override
    public void visit(@Nonnull OWLSubClassOfAxiom axiom) {
        setAxiomWriting();
        axiom.getSubClass().accept(this);
        write(SUBCLASS_OF);
        axiom.getSuperClass().accept(this);
        restore();
    }

    @Override
    public void visit(@Nonnull OWLNegativeObjectPropertyAssertionAxiom axiom) {
        setAxiomWriting();
        write(NOT);
        write("(");
        axiom.getSubject().accept(this);
        write(" ");
        axiom.getProperty().accept(this);
        write(" ");
        axiom.getObject().accept(this);
        write(")");
        restore();
    }

    @Override
    public void visit(@Nonnull OWLAsymmetricObjectPropertyAxiom axiom) {
        setAxiomWriting();
        writeSectionKeyword(ASYMMETRIC);
        axiom.getProperty().accept(this);
        restore();
    }

    @Override
    public void visit(@Nonnull OWLReflexiveObjectPropertyAxiom axiom) {
        setAxiomWriting();
        writeSectionKeyword(REFLEXIVE);
        axiom.getProperty().accept(this);
        restore();
    }

    private void writeBinaryOrNaryList(
        @Nonnull ManchesterOWLSyntax binaryKeyword,
        @Nonnull Set<? extends OWLObject> objects,
        @Nonnull ManchesterOWLSyntax naryKeyword) {
        if (objects.size() == 2) {
            Iterator<? extends OWLObject> it = sort(objects).iterator();
            it.next().accept(this);
            write(binaryKeyword);
            it.next().accept(this);
        } else {
            writeSectionKeyword(naryKeyword);
            writeCommaSeparatedList(objects);
        }
    }

    @Override
    public void visit(@Nonnull OWLDisjointClassesAxiom axiom) {
        setAxiomWriting();
        writeBinaryOrNaryList(DISJOINT_WITH, axiom.getClassExpressions(),
            DISJOINT_CLASSES);
        restore();
    }

    @Override
    public void visit(@Nonnull OWLDataPropertyDomainAxiom axiom) {
        setAxiomWriting();
        axiom.getProperty().accept(this);
        write(DOMAIN);
        axiom.getDomain().accept(this);
        restore();
    }

    @Override
    public void visit(@Nonnull OWLObjectPropertyDomainAxiom axiom) {
        setAxiomWriting();
        axiom.getProperty().accept(this);
        write(DOMAIN);
        axiom.getDomain().accept(this);
        restore();
    }

    @Override
    public void visit(@Nonnull OWLEquivalentObjectPropertiesAxiom axiom) {
        setAxiomWriting();
        writeBinaryOrNaryList(EQUIVALENT_TO, axiom.getProperties(),
            EQUIVALENT_PROPERTIES);
        restore();
    }

    @Override
    public void visit(@Nonnull OWLNegativeDataPropertyAssertionAxiom axiom) {
        setAxiomWriting();
        write(NOT);
        write("(");
        axiom.getSubject().accept(this);
        write(" ");
        axiom.getProperty().accept(this);
        write(" ");
        axiom.getObject().accept(this);
        write(")");
        restore();
    }

    @Override
    public void visit(@Nonnull OWLDifferentIndividualsAxiom axiom) {
        setAxiomWriting();
        writeBinaryOrNaryList(DIFFERENT_FROM, axiom.getIndividuals(),
            DIFFERENT_INDIVIDUALS);
        restore();
    }

    @Override
    public void visit(@Nonnull OWLDisjointDataPropertiesAxiom axiom) {
        setAxiomWriting();
        writeBinaryOrNaryList(DISJOINT_WITH, axiom.getProperties(),
            DISJOINT_PROPERTIES);
        restore();
    }

    @Override
    public void visit(@Nonnull OWLDisjointObjectPropertiesAxiom axiom) {
        setAxiomWriting();
        writeBinaryOrNaryList(DISJOINT_WITH, axiom.getProperties(),
            DISJOINT_PROPERTIES);
        restore();
    }

    @Override
    public void visit(@Nonnull OWLObjectPropertyRangeAxiom axiom) {
        setAxiomWriting();
        axiom.getProperty().accept(this);
        write(RANGE);
        axiom.getRange().accept(this);
        restore();
    }

    @Override
    public void visit(@Nonnull OWLObjectPropertyAssertionAxiom axiom) {
        setAxiomWriting();
        axiom.getSubject().accept(this);
        write(" ");
        axiom.getProperty().accept(this);
        write(" ");
        axiom.getObject().accept(this);
        restore();
    }

    @Override
    public void visit(@Nonnull OWLFunctionalObjectPropertyAxiom axiom) {
        setAxiomWriting();
        writeSectionKeyword(FUNCTIONAL);
        axiom.getProperty().accept(this);
        restore();
    }

    @Override
    public void visit(@Nonnull OWLSubObjectPropertyOfAxiom axiom) {
        setAxiomWriting();
        axiom.getSubProperty().accept(this);
        write(SUB_PROPERTY_OF);
        axiom.getSuperProperty().accept(this);
        restore();
    }

    @Override
    public void visit(@Nonnull OWLDisjointUnionAxiom axiom) {
        setAxiomWriting();
        axiom.getOWLClass().accept(this);
        write(DISJOINT_UNION_OF);
        writeCommaSeparatedList(axiom.getClassExpressions());
        restore();
    }

    private void writeFrameType(OWLObject object) {
        setAxiomWriting();
        if (object instanceof OWLOntology) {
            writeFrameKeyword(ONTOLOGY);
            OWLOntology ont = (OWLOntology) object;
            if (!ont.isAnonymous()) {
                write("<");
                write(ont.getOntologyID().getOntologyIRI().get().toString());
                write(">");
            }
        } else {
            if (object instanceof OWLClassExpression) {
                writeFrameKeyword(CLASS);
            } else if (object instanceof OWLObjectPropertyExpression) {
                writeFrameKeyword(OBJECT_PROPERTY);
            } else if (object instanceof OWLDataPropertyExpression) {
                writeFrameKeyword(DATA_PROPERTY);
            } else if (object instanceof OWLIndividual) {
                writeFrameKeyword(INDIVIDUAL);
            } else if (object instanceof OWLAnnotationProperty) {
                writeFrameKeyword(ANNOTATION_PROPERTY);
            }
        }
        object.accept(this);
    }

    @Override
    public void visit(@Nonnull OWLDeclarationAxiom axiom) {
        setAxiomWriting();
        writeFrameType(axiom.getEntity());
        restore();
    }

    @Override
    public void visit(@Nonnull OWLAnnotationAssertionAxiom axiom) {
        setAxiomWriting();
        axiom.getSubject().accept(this);
        write(" ");
        axiom.getAnnotation().accept(this);
        restore();
    }

    @Override
    public void visit(@Nonnull OWLAnnotationPropertyDomainAxiom axiom) {
        setAxiomWriting();
        axiom.getProperty().accept(this);
        write(DOMAIN);
        axiom.getDomain().accept(this);
    }

    @Override
    public void visit(@Nonnull OWLAnnotationPropertyRangeAxiom axiom) {
        setAxiomWriting();
        axiom.getProperty().accept(this);
        write(RANGE);
        axiom.getRange().accept(this);
    }

    @Override
    public void visit(@Nonnull OWLSubAnnotationPropertyOfAxiom axiom) {
        setAxiomWriting();
        axiom.getSubProperty().accept(this);
        write(SUB_PROPERTY_OF);
        axiom.getSuperProperty().accept(this);
    }

    @Override
    public void visit(@Nonnull OWLSymmetricObjectPropertyAxiom axiom) {
        setAxiomWriting();
        writeSectionKeyword(SYMMETRIC);
        axiom.getProperty().accept(this);
        restore();
    }

    @Override
    public void visit(@Nonnull OWLDataPropertyRangeAxiom axiom) {
        setAxiomWriting();
        axiom.getProperty().accept(this);
        writeSectionKeyword(RANGE);
        axiom.getRange().accept(this);
        restore();
    }

    @Override
    public void visit(@Nonnull OWLFunctionalDataPropertyAxiom axiom) {
        setAxiomWriting();
        writeSectionKeyword(FUNCTIONAL);
        axiom.getProperty().accept(this);
        restore();
    }

    @Override
    public void visit(@Nonnull OWLEquivalentDataPropertiesAxiom axiom) {
        setAxiomWriting();
        writeFrameKeyword(EQUIVALENT_PROPERTIES);
        writeCommaSeparatedList(axiom.getProperties());
        restore();
    }

    @Override
    public void visit(@Nonnull OWLClassAssertionAxiom axiom) {
        setAxiomWriting();
        axiom.getIndividual().accept(this);
        write(TYPE);
        axiom.getClassExpression().accept(this);
        restore();
    }

    @Override
    public void visit(@Nonnull OWLEquivalentClassesAxiom axiom) {
        setAxiomWriting();
        writeBinaryOrNaryList(EQUIVALENT_TO, axiom.getClassExpressions(),
            EQUIVALENT_CLASSES);
        restore();
    }

    @Override
    public void visit(@Nonnull OWLDataPropertyAssertionAxiom axiom) {
        setAxiomWriting();
        axiom.getSubject().accept(this);
        write(" ");
        axiom.getProperty().accept(this);
        write(" ");
        axiom.getObject().accept(this);
        restore();
    }

    @Override
    public void visit(@Nonnull OWLTransitiveObjectPropertyAxiom axiom) {
        setAxiomWriting();
        writeSectionKeyword(TRANSITIVE);
        axiom.getProperty().accept(this);
        restore();
    }

    @Override
    public void visit(@Nonnull OWLIrreflexiveObjectPropertyAxiom axiom) {
        setAxiomWriting();
        writeSectionKeyword(IRREFLEXIVE);
        axiom.getProperty().accept(this);
        restore();
    }

    @Override
    public void visit(@Nonnull OWLSubDataPropertyOfAxiom axiom) {
        setAxiomWriting();
        axiom.getSubProperty().accept(this);
        writeSectionKeyword(SUB_PROPERTY_OF);
        axiom.getSuperProperty().accept(this);
        restore();
    }

    @Override
    public void visit(@Nonnull OWLInverseFunctionalObjectPropertyAxiom axiom) {
        setAxiomWriting();
        writeSectionKeyword(INVERSE_FUNCTIONAL);
        axiom.getProperty().accept(this);
        restore();
    }

    @Override
    public void visit(@Nonnull OWLSameIndividualAxiom axiom) {
        setAxiomWriting();
        writeBinaryOrNaryList(SAME_AS, axiom.getIndividuals(), SAME_INDIVIDUAL);
        restore();
    }

    @Override
    public void visit(@Nonnull OWLSubPropertyChainOfAxiom axiom) {
        setAxiomWriting();
        for (Iterator<OWLObjectPropertyExpression> it = axiom
            .getPropertyChain().iterator(); it.hasNext();) {
            it.next().accept(this);
            if (it.hasNext()) {
                write(" o ");
            }
        }
        write(SUB_PROPERTY_OF);
        axiom.getSuperProperty().accept(this);
        restore();
    }

    @Override
    public void visit(@Nonnull OWLInverseObjectPropertiesAxiom axiom) {
        setAxiomWriting();
        axiom.getFirstProperty().accept(this);
        write(INVERSE_OF);
        axiom.getSecondProperty().accept(this);
        restore();
    }

    @Override
    public void visit(@Nonnull SWRLRule rule) {
        setAxiomWriting();
        for (Iterator<SWRLAtom> it = rule.getBody().iterator(); it.hasNext();) {
            it.next().accept(this);
            if (it.hasNext()) {
                write(", ");
            }
        }
        write(" -> ");
        for (Iterator<SWRLAtom> it = rule.getHead().iterator(); it.hasNext();) {
            it.next().accept(this);
            if (it.hasNext()) {
                write(", ");
            }
        }
        restore();
    }

    @Override
    public void visit(@Nonnull OWLHasKeyAxiom axiom) {
        setAxiomWriting();
        axiom.getClassExpression().accept(this);
        write(HAS_KEY);
        write(axiom.getObjectPropertyExpressions(), COMMA, false);
        write(axiom.getDataPropertyExpressions(), COMMA, false);
    }

    // SWRL
    @Override
    public void visit(@Nonnull SWRLClassAtom node) {
        if (node.getPredicate().isAnonymous()) {
            write("(");
        }
        node.getPredicate().accept(this);
        if (node.getPredicate().isAnonymous()) {
            write(")");
        }
        write("(");
        node.getArgument().accept(this);
        write(")");
    }

    @Override
    public void visit(@Nonnull SWRLDataRangeAtom node) {
        node.getPredicate().accept(this);
        write("(");
        node.getArgument().accept(this);
        write(")");
    }

    @Override
    public void visit(@Nonnull SWRLObjectPropertyAtom node) {
        node.getPredicate().accept(this);
        write("(");
        node.getFirstArgument().accept(this);
        write(", ");
        node.getSecondArgument().accept(this);
        write(")");
    }

    @Override
    public void visit(@Nonnull SWRLDataPropertyAtom node) {
        node.getPredicate().accept(this);
        write("(");
        node.getFirstArgument().accept(this);
        write(", ");
        node.getSecondArgument().accept(this);
        write(")");
    }

    @Override
    public void visit(@Nonnull SWRLBuiltInAtom node) {
        SWRLBuiltInsVocabulary voc = SWRLBuiltInsVocabulary.getBuiltIn(node
            .getPredicate());
        if (voc != null) {
            write(voc.getPrefixedName());
        } else {
            write(node.getPredicate().toQuotedString());
        }
        write("(");
        for (Iterator<SWRLDArgument> it = sort(node.getArguments()).iterator(); it
            .hasNext();) {
            it.next().accept(this);
            if (it.hasNext()) {
                write(", ");
            }
        }
        write(")");
    }

    @Override
    public void visit(@Nonnull SWRLVariable node) {
        write("?");
        // do not save the namespace if it's the conventional one
        if ("urn:swrl#".equals(node.getIRI().getNamespace())) {
            write(node.getIRI().prefixedBy(""));
        } else {
            write(node.getIRI().toQuotedString());
        }
    }

    @Override
    public void visit(@Nonnull SWRLIndividualArgument node) {
        node.getIndividual().accept(this);
    }

    @Override
    public void visit(@Nonnull SWRLLiteralArgument node) {
        node.getLiteral().accept(this);
    }

    @Override
    public void visit(@Nonnull SWRLSameIndividualAtom node) {
        write(SAME_AS);
        write("(");
        node.getFirstArgument().accept(this);
        write(", ");
        node.getSecondArgument().accept(this);
        write(")");
    }

    @Override
    public void visit(@Nonnull SWRLDifferentIndividualsAtom node) {
        write(DIFFERENT_FROM);
        write("(");
        node.getFirstArgument().accept(this);
        write(", ");
        node.getSecondArgument().accept(this);
        write(")");
    }

    @Override
    public void visit(OWLDatatypeDefinitionAxiom axiom) {}

    protected void writeAnnotations(@Nonnull Set<OWLAnnotation> annos) {
        if (annos.isEmpty()) {
            return;
        }
        writeNewLine();
        write(ANNOTATIONS.toString());
        write(": ");
        pushTab(getIndent());
        for (Iterator<OWLAnnotation> annoIt = sort(annos).iterator(); annoIt
            .hasNext();) {
            OWLAnnotation anno = annoIt.next();
            anno.accept(this);
            if (annoIt.hasNext()) {
                write(", ");
                writeNewLine();
            }
        }
        writeNewLine();
        writeNewLine();
        popTab();
    }

    // Ontology
    @Override
    public void visit(OWLOntology ontology) {}
}
