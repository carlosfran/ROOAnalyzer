package br.uern.aridus.roo;

/**
 * Lista os conceitos das ontologias que não estão na ontologia base (considerada a 0).
 */
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntProperty;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

public class ListConceptsDiff {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {

		Properties ontologies = new Properties();
		ontologies.load(new FileReader(new File("ontologies.properties")));
		
		List<String> models = new ArrayList<String>();
		Enumeration<Object> keys = ontologies.keys();
		
		while(keys.hasMoreElements()){
			String k = (String) keys.nextElement();
			String s = (String) ontologies.get(k);
			models.add(s);
		}
		
		Collections.reverse(models);
		String modelBase = models.remove(0);
		System.out.println(modelBase);
		
		OntModel ont1 = ModelFactory.createOntologyModel();
		ont1.read(modelBase);
		List<String> model = listConcepts(ont1);
		
		int i = 0;
		while(i < models.size()){
			System.out.println("Conceitos de "+ models.get(i));
			ont1 = ModelFactory.createOntologyModel();
			ont1.read(models.get(i));
			ArrayList<String> list = (ArrayList<String>) listConcepts(ont1);
			Iterator<String> ita = list.iterator();
			while(ita.hasNext()){
				String s = ita.next();
				if(!model.contains(s))
					System.out.println(s);
			}
			i++;
		}
	}
	
	public static List<String> listConceptsFilter(List<String> concepts, List<String> model){		
		List<String> retlist = new ArrayList<String>();
		Iterator<String> ic = concepts.iterator();
		String c;
		while(ic.hasNext()){
			c = ic.next();
			if(!model.contains(c))
				retlist.add(c);
		}
		return retlist;
	}
	
	/*public static boolean compareConcepts(String c1, String c2){
		boolean ret = false;
		String base = "http://www.leeds.ac.uk/ontologies/Sustainability";
		
		if(c1.substring(base.length()+2, c1.length()).
				equalsIgnoreCase(c2.substring(base.length()+2, c2.length())))
			ret = true;
		return ret;
	}*/

	public static List<String> listConcepts(OntModel ontModel) {
		
		List<String> result = new ArrayList<String>();
		
		Iterator<OntClass> classIterator = ontModel.listClasses().toList().iterator();
		
		List<String> prop = new ArrayList<String>();
		List<String> classes = new ArrayList<String>();
		List<String> instances = new ArrayList<String>();
		
		List<OntClass> lclass = new ArrayList<OntClass>();
		
		while (classIterator.hasNext()){
			String className = "";
			OntClass ontClass = classIterator.next();
			if (ontClass.getLocalName() != null && !(ontClass.getLocalName()).isEmpty() && (!ontClass.getLocalName().equals("Thing"))) {
				className = ontClass.getLocalName();
				if(!classes.contains(className) && !(ontClass.getURI().toString()).contains("http://www.ordnancesurvey.co.uk/ontologies/osmethodology/1.0/"))
					classes.add(className.toString());
			}
			
//			System.out.println(ontClass.toString());
			ExtendedIterator<OntClass> classs = ontClass.listSubClasses();
			while (classs.hasNext()) {
				OntClass ontRoot = classs.next();
				lclass.add(ontRoot);

				if (ontRoot.getLocalName() != null && !(ontRoot.getLocalName()).isEmpty()) {
					className = ontRoot.getLocalName();
//					className = ontRoot.getURI().toString();
					if(!classes.contains(className.toString()) && !(ontClass.getURI().toString()).contains("http://www.ordnancesurvey.co.uk/ontologies/osmethodology/1.0/"))
						classes.add(className);
				}
			}
		}
		
		Iterator<OntClass> iclass = lclass.iterator();
		while (iclass.hasNext()){
			OntClass ontClass = iclass.next();
			ExtendedIterator<OntProperty> rootIterator = ontClass
					.listDeclaredProperties();
			while (rootIterator.hasNext()) {
				OntProperty ontRoot = rootIterator.next();
				String properties = ontRoot.toString();
				String propuri = ontRoot.getURI().toString();
				//if(!propuri.contains("http://www.ordnancesurvey.co.uk/ontologies/osmethodology/1.0/") && 
					//!propuri.contains("http://purl.org/dc/elements/1.1/")){
				if(!prop.contains(propuri)){
					//prop.add(propuri);
					int propertyLength = properties.length();
					int propertyIndex = properties.lastIndexOf("#");
					if (propertyIndex != -1) {
						String property = properties.substring(propertyIndex + 1,
								propertyLength);
						if(!prop.contains(property)){
							prop.add(property);
//							prop.add(ontRoot.getURI().toString());
							NodeIterator ni = ontRoot.listPropertyValues(ontRoot);
							while (ni.hasNext()) {
								RDFNode node = ni.next();
								String instance = node.toString();
								if(!instance.equalsIgnoreCase("null"))
									instances.add(instance);
							}
						}
					}
				}
			}
		}
		
		ExtendedIterator<Individual> instanceIterator = (ExtendedIterator<Individual>) ontModel.listIndividuals();
		while (instanceIterator.hasNext()) {
			OntResource ontRoot = instanceIterator.next();
			String instance = ontRoot.getLocalName();
//			String instance = ontRoot.getURI().toString();
//			System.out.println(instance);
			if(!instance.equalsIgnoreCase("null") && !instances.contains(instance))
				instances.add(instance);
		}
		
		result.addAll(classes);
		result.addAll(prop);
		result.addAll(instances);
		return result;
	}
	/*

	public static List<String> testMethod2(OntModel ontModel) {
		
		List<String> result = new ArrayList<String>();
		
		Iterator<OntClass> classIterator = ontModel.listClasses().toList().iterator();
		
		List<String> prop = new ArrayList<String>();
		List<String> classes = new ArrayList<String>();
		List<String> instances = new ArrayList<String>();
		
		List<OntClass> lclass = new ArrayList<OntClass>();
		
		while (classIterator.hasNext()){
			String className = "";
			OntClass ontClass = classIterator.next();
			if (ontClass.getLocalName() != null && !(ontClass.getLocalName()).isEmpty() && (!ontClass.getLocalName().equals("Thing"))) {
				className = ontClass.getURI().toString();
				if(!classes.contains(className) && !(ontClass.getURI().toString()).contains("http://www.ordnancesurvey.co.uk/ontologies/osmethodology/1.0/"))
					classes.add(className);
			}
			
//			System.out.println(ontClass.toString());
			ExtendedIterator<OntClass> classs = ontClass.listSubClasses();
			while (classs.hasNext()) {
				OntClass ontRoot = classs.next();
				lclass.add(ontRoot);

				if (ontRoot.getLocalName() != null && !(ontRoot.getLocalName()).isEmpty()) {
					className = ontRoot.getURI().toString();
					if(!classes.contains(className.toString()) && !(ontClass.getURI().toString()).contains("http://www.ordnancesurvey.co.uk/ontologies/osmethodology/1.0/"))
						classes.add(className);
				}
			}
		}
		
		Iterator<OntClass> iclass = lclass.iterator();
		while (iclass.hasNext()){
			OntClass ontClass = iclass.next();
			ExtendedIterator<OntProperty> rootIterator = ontClass
					.listDeclaredProperties();
			while (rootIterator.hasNext()) {
				OntProperty ontRoot = rootIterator.next();
				String properties = ontRoot.toString();
				String propuri = ontRoot.getURI().toString();
				if(!propuri.contains("http://www.ordnancesurvey.co.uk/ontologies/osmethodology/1.0/") && 
					!propuri.contains("http://purl.org/dc/elements/1.1/")){
				if(!prop.contains(propuri)){
					prop.add(propuri);
					int propertyLength = properties.length();
					int propertyIndex = properties.lastIndexOf("#");
					if (propertyIndex != -1) {
						String property = properties.substring(propertyIndex + 1,
								propertyLength);
						if(!prop.contains(property)){
//							prop.add(property);
							prop.add(ontRoot.getURI().toString());
							NodeIterator ni = ontRoot.listPropertyValues(ontRoot);
							while (ni.hasNext()) {
								RDFNode node = ni.next();
								String instance = node.toString();
								if(!instance.equalsIgnoreCase("null"))
									instances.add(instance);
							}
						}
					}
				}
				}
			}
		}
		
		ExtendedIterator<Individual> instanceIterator = (ExtendedIterator<Individual>) ontModel.listIndividuals();
		while (instanceIterator.hasNext()) {
			OntResource ontRoot = instanceIterator.next();
//			String instance = ontRoot.getLocalName();
			String instance = ontRoot.getURI().toString();
//			System.out.println(instance);
			if(!instance.equalsIgnoreCase("null") && !instances.contains(instance))
				instances.add(instance);
		}
		
		result.addAll(classes);
		result.addAll(prop);
		result.addAll(instances);
		return result;
	}*/
}