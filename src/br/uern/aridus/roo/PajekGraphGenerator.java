package br.uern.aridus.roo;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.semanticweb.owl.align.AlignmentException;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.engine.binding.Binding;

import fr.inrialpes.exmo.ontowrap.OntowrapException;

public class PajekGraphGenerator {

	public static void main(String[] args) throws AlignmentException, IOException, URISyntaxException, OntowrapException, NoSuchAlgorithmException {
		Analyzer analizer = new Analyzer();
		int o1, o2, req;

		for (int m = 0; m < analizer.aservClient.methodNames.size(); m++) {
			String methodName = analizer.aservClient.methodNames.get(m);
			analizer.aligns = new ArrayList<String>();
			File dir = new File("alignments");
			if (dir.exists() && dir.canRead() && dir.isDirectory()) {
				File[] files = dir.listFiles();
				int f = 0;
				while (f < files.length) {
					String sfname = (files[f].toURI()).toString();
					if(sfname.indexOf(methodName) >= 0)
						analizer.aligns.add(sfname);
					f++;
				}
			}
			
			System.out.println(analizer.aservClient.requests.size() + " alinhamentos com o método "+ methodName);
			Iterator<Integer> it = analizer.aservClient.requests.iterator();
			
			if(Boolean.valueOf(analizer.configurations.getProperty("PajekGraph")))
				generatorPajekNetwork("reports/PajekGraph-"+methodName+".net",
						analizer.configurations.getProperty("uriBase"),
						analizer.listURI, analizer.aligns,
						Boolean.valueOf(analizer.configurations.getProperty("Filter")));
		}
	}

	// filename: graph-g1-alignName.net
	public static void generatorPajekNetwork(String filename, String uriBase,
			List<URI> ontos, List<String> aligns, boolean filter)
			throws IOException {

		HashMap<String, Integer> hash1 = new HashMap<String, Integer>();
		HashMap<Integer, String> hash2 = new HashMap<Integer, String>();
		FileWriter fw = new FileWriter(new File(filename));

		// for bc (background color)
		String bc = "Brown";

		// for ic (internal color)
		List<String> colors = new ArrayList<String>();
		colors.add(0, "Yellow");
		colors.add(1, "Orange");
		colors.add(2, "Maroon");
		colors.add(3, "Red");
		colors.add(4, "Blue");
		colors.add(5, "Green");
		colors.add(6, "Cyan");
		colors.add(7, "RubineRed");
		colors.add(8, "Mulberry");
		colors.add(9, "LimeGreen");
		colors.add(10, "Purple");
		colors.add(11, "CadetBlue");
		colors.add(12, "TealBlue");
		colors.add(13, "OliveGreen");
		colors.add(14, "Magenta");
		colors.add(15, "MidnightBlue");
		colors.add(16, "Dandelion");
		colors.add(17, "WildStrawberry");
		colors.add(18, "ForestGreen");
		colors.add(19, "Salmon");
		colors.add(20, "GreenYellow");
		colors.add(21, "CornflowerBlue");
		colors.add(22, "Tan");

		
		// Adicionando os V�rtices
		// *Vertices <Qtde>
		// ID Label ic Color1 c Colors
		int ccount = 0;
		
		Iterator<URI> io = ontos.iterator();
		int ip = -1;
		while (io.hasNext()) {
			ip++;
			System.out.println("Processando ontologia "+ ip);
			URI u = io.next();
			if (u != null) {
				String uri = u.toString();
				OntModel model = ModelFactory.createOntologyModel();
				model.read(uri);
				// criar prefixo
				String prefix = "o" + ip;
				// obter lista de conceitos
				ListConceptsDiff getConcepts = new ListConceptsDiff();
				List<String> concepts = getConcepts.listConcepts(model);
				if (filter && ip!=0) {
					String modelBase = ontos.get(0).toString();
					OntModel ontBase = ModelFactory.createOntologyModel();
					ontBase.read(modelBase);
					List<String> mBase = ListConceptsDiff.listConcepts(ontBase);
					concepts = getConcepts.listConceptsFilter(concepts, mBase);
				}

				// adicionar a lista de conceitos a hashtable (prefix:conceitos)
				Iterator<String> iconcepts = concepts.iterator();
				while (iconcepts.hasNext()) {
					String c = new String(prefix + ":" + iconcepts.next());
					hash1.put(c, ccount);
					hash2.put(ccount, c);
					ccount++;
				}
			}
		}

		System.out.println("Adicionando vértices...");
		fw.write("*Vertices " + ccount + "\n");
		for (int i = 1; i <= ccount; i++) {
			String c = hash2.get(i - 1);
			fw.write(i + " \"" + c + "\" ic "
					+ colors.get(Integer.valueOf(c.substring(1, 2))) + " bc "
					+ bc + "\n");
		}

		// Adicionando os Arcs
		System.out.println("Adicionando arcos...");
		Iterator<String> ialign = aligns.iterator();
		Model m = ModelFactory.createOntologyModel();
		while (ialign.hasNext()) {
			m.read((ialign.next()).toString());
		}
	
		Query q = QueryFactory
				.create("PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
						+ "PREFIX align: <http://knowledgeweb.semanticweb.org/heterogeneity/alignment#>\n"
						+ "SELECT DISTINCT ?e1 ?e2 ?m "
						+ "WHERE{"
						+ "	?a rdf:type align:Alignment . "
						+ "	?a align:map ?c . "
						+ "	?c align:entity1 ?e1 ."
						+ "	?c align:entity2 ?e2 ."
						+ "  ?c align:measure ?m "
						+ "}");

		QueryExecution exec = QueryExecutionFactory.create(q, m);
		ResultSet res = exec.execSelect();
		fw.write("*Arcs\n");
		while (res.hasNext()) {
			Binding sol = res.nextBinding();
			String e1 = sol.get(Var.alloc("e1")).toString();
			String e2 = sol.get(Var.alloc("e2")).toString();
			float me = Float.valueOf(sol.get(Var.alloc("m")).getLiteralValue()
					.toString());

			String prefix = "o"
					+ e1.substring(uriBase.length(), uriBase.length() + 1);
			e1 = prefix + ":" + e1.substring(uriBase.length() + 2, e1.length());
			prefix = "o" + e2.substring(uriBase.length(), uriBase.length() + 1);
			e2 = prefix + ":" + e2.substring(uriBase.length() + 2, e2.length());
			Integer i1, i2;
			i1 = hash1.get(e1);
			i2 = hash1.get(e2);
			if (i1 != null && i2 != null) {
				i1++;
				i2++;
				fw.write(i1 + " " + i2 + " "
						+ (String.format("%.4f", me)).replace(",", ".") + "\n");
			}
		}
		fw.flush();
		fw.close();
		System.out.println("Pajek Graph concluído!");
	}
}
