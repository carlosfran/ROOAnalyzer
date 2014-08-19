package br.uern.aridus.roo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.lucene.analysis.StopAnalyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.snowball.SnowballAnalyzer;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.util.Version;
import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.OntologyNetwork;

import com.wcohen.ss.Levenstein;

import fr.inrialpes.exmo.align.impl.BasicOntologyNetwork;
import fr.inrialpes.exmo.align.parser.AlignmentParser;
import fr.inrialpes.exmo.ontosim.Measure;
import fr.inrialpes.exmo.ontosim.OntologySpaceMeasure;
import fr.inrialpes.exmo.ontosim.VectorSpaceMeasure;
import fr.inrialpes.exmo.ontosim.align.ASUnionPathCoverageMeasure;
import fr.inrialpes.exmo.ontosim.entity.EntityLexicalMeasure;
import fr.inrialpes.exmo.ontosim.entity.model.Entity;
import fr.inrialpes.exmo.ontosim.set.AverageLinkage;
import fr.inrialpes.exmo.ontosim.set.FullLinkage;
import fr.inrialpes.exmo.ontosim.set.Hausdorff;
import fr.inrialpes.exmo.ontosim.set.MaxCoupling;
import fr.inrialpes.exmo.ontosim.set.MaxSet;
import fr.inrialpes.exmo.ontosim.set.MaximumSet;
import fr.inrialpes.exmo.ontosim.set.SetMeasure;
import fr.inrialpes.exmo.ontosim.set.SingleLinkage;
import fr.inrialpes.exmo.ontosim.set.WeightedMaxSum;
import fr.inrialpes.exmo.ontosim.string.CommonWords;
import fr.inrialpes.exmo.ontosim.string.JWNLDistances;
import fr.inrialpes.exmo.ontosim.string.StringDistances;
import fr.inrialpes.exmo.ontosim.string.StringMeasureSS;
import fr.inrialpes.exmo.ontosim.vector.CosineVM;
import fr.inrialpes.exmo.ontosim.vector.JaccardVM;
import fr.inrialpes.exmo.ontosim.vector.KendallTau;
import fr.inrialpes.exmo.ontosim.vector.model.DocumentCollection;
import fr.inrialpes.exmo.ontowrap.LoadedOntology;
import fr.inrialpes.exmo.ontowrap.OntologyFactory;
import fr.inrialpes.exmo.ontowrap.OntowrapException;

public class Analyzer {

	static SnowballAnalyzer snowballAnalyzer = new SnowballAnalyzer(Version.LUCENE_30,
			"English", StopAnalyzer.ENGLISH_STOP_WORDS_SET);

	static Properties configurations = new Properties();
	static double[][] results = new double[7][7];
	AServClient aservClient;
	static List<URI> listURI = new ArrayList<URI>();
	static List<String> aligns = new ArrayList<String>();
	static List<LoadedOntology<?>> loadedOntos = new ArrayList<LoadedOntology<?>>();

	public Analyzer() throws FileNotFoundException, IOException,
			URISyntaxException, OntowrapException {
		aservClient = new AServClient();
		configurations.load(new FileReader("analyzers.properties"));
		OntologyFactory of = OntologyFactory.getFactory();
		Enumeration<Object> keys = this.aservClient.ontologies.keys();
		while (keys.hasMoreElements()) {
			String key = (String) keys.nextElement();
			String suri = (String) this.aservClient.ontologies.get(key);
			URI uri = new URI("file:////"+suri);
			listURI.add(uri);
			LoadedOntology<?> loadedOnto = of.loadOntology(uri);
			loadedOntos.add(loadedOnto);
		}
		Collections.reverse(listURI);
		Collections.reverse(loadedOntos);
	}

	public static void main(String[] args) throws OntowrapException,
			AlignmentException, IOException, NoSuchAlgorithmException,
			SQLException, URISyntaxException {

		Analyzer analyzer = new Analyzer();

		// Criar matriz de resultados
		int len = loadedOntos.size();

		String amethods = configurations.get("AnalizerMethods").toString();

		// Análise OntologySpace
		if (amethods.charAt(0) == '1') {
			System.out.print("\nExecutando análise ontology space...");
			results = new double[len][len];
			for (int i = 0; i < len; i++) {
				for (int j = 0; j < len; j++) {
					results[i][j] = -1;
					if (i != j) {
						analyzer.getOntologySpace(i, j, loadedOntos.get(i),
								loadedOntos.get(j));
					}
				}
			}
			System.out.println("OK!\nAnálise ontology space concluída!");
			System.out.println("\n\nResults of OntologySpace:");
			for (int i = 0; i < len; i++) {
				for (int j = 0; j < len; j++) {
					System.out.printf("%.5f, ", results[i][j]);
				}
				System.out.println();
			}
		}

		// Análise Vector Space
		if (amethods.charAt(1) == '1') {
			System.out.print("\nExecutando análise vector space...");
			for (int i = 0; i < len; i++) {
				for (int j = 0; j < len; j++) {
					if (i != j) {
						analyzer.getVectorSpace(i, j, loadedOntos,
								loadedOntos.get(i), loadedOntos.get(j));
					}
				}
			}
			System.out.println("OK!\nAnálise vector space concluída!");
		}

		// Análise Alignment Space
		if (amethods.charAt(2) == '1') {
			System.out.println("Executando análise alignment space...");
			File dir = new File("alignments");
			if (dir.exists() && dir.canRead() && dir.isDirectory()) {
				File[] files = dir.listFiles();
				int f = 0;
				while (f < files.length) {
					aligns.add((files[f].toURI()).toString());
					f++;
				}
			}

			int o1, o2, req;

			for (int m = 0; m < analyzer.aservClient.methodNames.size(); m++) {
				String methodName = analyzer.aservClient.methodNames.get(m);

				Iterator<Integer> it = analyzer.aservClient.requests.iterator();
				while (it.hasNext()) {
					req = it.next();
					o1 = req / 10;
					o2 = req % 10;

					analyzer.getAlignmentMeasures("reports/AlignmentMeasures-"
							+ o1 + "" + o2 + "-" + methodName + ".txt", o1, o2,
							aligns, listURI, loadedOntos.get(o1),
							loadedOntos.get(o2));
					System.out.println("AlignmentMeasure(" + o1 + "," + o2
							+ ") ... OK");
				}
			}
			System.out.println("Análise alignment space concluída!");
		}
	}

	public void getOntologySpace(int i1, int i2, LoadedOntology<?> o1,
			LoadedOntology<?> o2) throws IOException {

		String out;
		FileWriter fw = new FileWriter(new File("reports/OntologySpace-" + i1
				+ "" + i2 + ".txt"));

		System.out.println("\n == Ontology Space == ");
		OntologySpaceMeasure ontoSpace;
		Measure<Entity<?>> measure;
		SetMeasure<Entity<?>> setMeasure;

		CommonWords commonWords = new CommonWords();
		JWNLDistances jwnlDistances = new JWNLDistances();
		StringDistances stringDistances = new StringDistances();
		StringMeasureSS stringMeasuress = new StringMeasureSS(new Levenstein());

		// Hausdorsff
		measure = new EntityLexicalMeasure(stringMeasuress);
		setMeasure = new Hausdorff<Entity<?>>(measure);
		ontoSpace = new OntologySpaceMeasure(setMeasure);
		out = "EntityLexicalMeasure/StringMeasureSS/Hausdorff - Similarity(o"
				+ i1 + ", o" + i2 + "): " + ontoSpace.getSim(o1, o2);
		System.out.println(out);
		fw.append(out + "\n");
		fw.flush();

		measure = new EntityLexicalMeasure(commonWords);
		setMeasure = new Hausdorff<Entity<?>>(measure);
		ontoSpace = new OntologySpaceMeasure(setMeasure);
		out = "EntityLexicalMeasure/CommonWords/Hausdorff - Similarity(o" + i1
				+ ", o" + i2 + "): " + ontoSpace.getSim(o1, o2);
		System.out.println(out);
		fw.append(out + "\n");
		fw.flush();

		// MaxCoupling
		measure = new EntityLexicalMeasure(stringMeasuress);
		setMeasure = new MaxCoupling(measure);
		ontoSpace = new OntologySpaceMeasure(setMeasure);
		out = "EntityLexicalMeasure/StringMeasureSS/MaxCoupling - Similarity(o"
				+ i1 + ", o" + i2 + "): " + ontoSpace.getSim(o1, o2);
		System.out.println(out);
		fw.append(out + "\n");
		fw.flush();

		measure = new EntityLexicalMeasure(commonWords);
		setMeasure = new MaxCoupling(measure);
		ontoSpace = new OntologySpaceMeasure(setMeasure);
		out = "EntityLexicalMeasure/CommonWords/MaxCoupling - Similarity(o"
				+ i1 + ", o" + i2 + "): " + ontoSpace.getSim(o1, o2);
		System.out.println(out);
		fw.append(out + "\n");
		fw.flush();

		// SingleLinkage
		measure = new EntityLexicalMeasure(stringMeasuress);
		setMeasure = new SingleLinkage(measure);
		ontoSpace = new OntologySpaceMeasure(setMeasure);
		out = "EntityLexicalMeasure/StringMeasureSS/SingleLinkage - Similarity(o"
				+ i1 + ", o" + i2 + "): " + ontoSpace.getSim(o1, o2);
		System.out.println(out);
		fw.append(out + "\n");
		fw.flush();

		measure = new EntityLexicalMeasure(commonWords);
		setMeasure = new SingleLinkage(measure);
		ontoSpace = new OntologySpaceMeasure(setMeasure);
		out = "EntityLexicalMeasure/CommonWords/SingleLinkage - Similarity(o"
				+ i1 + ", o" + i2 + "): " + ontoSpace.getSim(o1, o2);
		System.out.println(out);
		fw.append(out + "\n");
		fw.flush();

		// AverageLinkage
		measure = new EntityLexicalMeasure(stringMeasuress);
		setMeasure = new AverageLinkage(measure);
		ontoSpace = new OntologySpaceMeasure(setMeasure);
		out = "EntityLexicalMeasure/StringMeasureSS/AverageLinkage - Similarity(o"
				+ i1 + ", o" + i2 + "): " + ontoSpace.getSim(o1, o2);
		System.out.println(out);
		fw.append(out + "\n");
		fw.flush();

		measure = new EntityLexicalMeasure(commonWords);
		setMeasure = new AverageLinkage(measure);
		ontoSpace = new OntologySpaceMeasure(setMeasure);
		out = "EntityLexicalMeasure/CommonWords/AverageLinkage - Similarity(o"
				+ i1 + ", o" + i2 + "): " + ontoSpace.getSim(o1, o2);
		System.out.println(out);
		fw.append(out + "\n");
		fw.flush();

		// FullLinkage
		measure = new EntityLexicalMeasure(commonWords);
		setMeasure = new FullLinkage<Entity<?>>(measure);
		ontoSpace = new OntologySpaceMeasure(setMeasure);
		out = "EntityLexicalMeasure/CommonWords/FullLinkage - Similarity(o"
				+ i1 + ", o" + i2 + "): " + ontoSpace.getSim(o1, o2);
		System.out.println(out);
		fw.append(out + "\n");
		fw.flush();

		measure = new EntityLexicalMeasure(stringMeasuress);
		setMeasure = new FullLinkage<Entity<?>>(measure);
		ontoSpace = new OntologySpaceMeasure(setMeasure);
		out = "EntityLexicalMeasure/StringMeasureSS/FullLinkage - Similarity(o"
				+ i1 + ", o" + i2 + "): " + ontoSpace.getSim(o1, o2);
		System.out.println(out);
		fw.append(out + "\n");
		fw.flush();

		// MaximumSet
		measure = new EntityLexicalMeasure(commonWords);
		setMeasure = new MaximumSet(measure);
		ontoSpace = new OntologySpaceMeasure(setMeasure);
		out = "EntityLexicalMeasure/CommonWords/MaximumSet - Similarity(o" + i1
				+ ", o" + i2 + "): " + ontoSpace.getSim(o1, o2);
		System.out.println(out);
		fw.append(out + "\n");
		fw.flush();

		measure = new EntityLexicalMeasure(stringMeasuress);
		setMeasure = new MaximumSet(measure);
		ontoSpace = new OntologySpaceMeasure(setMeasure);
		out = "EntityLexicalMeasure/StringMeasureSS/MaximumSet - Similarity(o"
				+ i1 + ", o" + i2 + "): " + ontoSpace.getSim(o1, o2);
		System.out.println(out);
		fw.append(out + "\n");
		fw.flush();

		// MaxSet
		measure = new EntityLexicalMeasure(commonWords);
		setMeasure = new MaxSet(measure);
		ontoSpace = new OntologySpaceMeasure(setMeasure);
		out = "EntityLexicalMeasure/CommonWords/MaxSet - Similarity(o" + i1
				+ ", o" + i2 + "): " + ontoSpace.getSim(o1, o2);
		System.out.println(out);
		fw.append(out + "\n");
		fw.flush();

		measure = new EntityLexicalMeasure(stringMeasuress);
		setMeasure = new MaxSet(measure);
		ontoSpace = new OntologySpaceMeasure(setMeasure);
		out = "EntityLexicalMeasure/StringMeasureSS/MaxSet - Similarity(o" + i1
				+ ", o" + i2 + "): " + ontoSpace.getSim(o1, o2);
		System.out.println(out);
		fw.append(out + "\n");
		fw.flush();

		// WeightedMaxSum
		measure = new EntityLexicalMeasure(commonWords);
		setMeasure = new WeightedMaxSum(measure);
		ontoSpace = new OntologySpaceMeasure(setMeasure);
		out = "EntityLexicalMeasure/CommonWords/WeightedMaxSum - Similarity(o"
				+ i1 + ", o" + i2 + "): " + ontoSpace.getSim(o1, o2);
		System.out.println(out);
		fw.append(out + "\n");
		fw.flush();

		measure = new EntityLexicalMeasure(stringMeasuress);
		setMeasure = new WeightedMaxSum(measure);
		ontoSpace = new OntologySpaceMeasure(setMeasure);
		out = "EntityLexicalMeasure/StringMeasureSS/WeightedMaxSum - Similarity(o"
				+ i1 + ", o" + i2 + "): " + ontoSpace.getSim(o1, o2);
		System.out.println(out);
		fw.append(out + "\n");
		fw.flush();
		fw.close();
	}

	public void getVectorSpace(int i1, int i2,
			List<LoadedOntology<?>> loadedOntos, LoadedOntology<?> o1,
			LoadedOntology<?> o2) throws IOException {

		String out;
		FileWriter fw = new FileWriter(new File("reports/VectorSpace-" + i1
				+ "" + i2 + ".txt"));

		System.out.println("\n == Vector Space ==");
		VectorSpaceMeasure vectorSpace;

		vectorSpace = new VectorSpaceMeasure(loadedOntos, new CosineVM(),
				DocumentCollection.WEIGHT.TFIDF);

		out = "VectorSpaceMeasure/CosineVM - Similarity(o" + i1 + ", o" + i2
				+ "): " + vectorSpace.getSim(o1, o2);
		fw.append(out + "\n");
		fw.flush();
		System.out.println(out);

		vectorSpace = new VectorSpaceMeasure(loadedOntos, new KendallTau(),
				DocumentCollection.WEIGHT.TFIDF);
		out = "VectorSpaceMeasure/KendallTau - Similarity(o" + i1 + ", o" + i2
				+ "): " + vectorSpace.getSim(o1, o2);
		fw.append(out + "\n");
		fw.flush();
		System.out.println(out);

		vectorSpace = new VectorSpaceMeasure(loadedOntos, new JaccardVM(),
				DocumentCollection.WEIGHT.TFIDF);
		out = "VectorSpaceMeasure/JaccardVM - Similarity(o" + i1 + ", o" + i2
				+ "): " + vectorSpace.getSim(o1, o2);
		fw.append(out + "\n");
		fw.flush();
		System.out.println(out);
		fw.close();

	}

	public void getAlignmentMeasures(String filename, int i1, int i2,
			List<String> aligns, List<URI> ontos, LoadedOntology<?> o1,
			LoadedOntology<?> o2) throws AlignmentException, IOException {

		String out;
		FileWriter fw = new FileWriter(new File(filename));

		Measure<LoadedOntology<?>> measure;
		OntologyNetwork noo = new BasicOntologyNetwork();

		Iterator<URI> ion = ontos.iterator();
		while (ion.hasNext()) {
			URI uri = ion.next();
			if (uri != null)
				noo.addOntology(uri);
		}

		Iterator<String> ia = aligns.iterator();
		while (ia.hasNext()) {
			AlignmentParser aparser = new AlignmentParser(0);
			Alignment al = aparser.parse(ia.next());
			noo.addAlignment(al);
		}

		measure = new ASUnionPathCoverageMeasure(noo);
		out = "";
		double d = measure.getSim(o1, o2);
		out += String.valueOf(d);
		fw.append(out + "\n");
		fw.flush();
		fw.close();

	}

	/**
	 * Lista conceitos e setenças da ontologia.
	 * 
	 * @param uriOntoBase
	 * @throws OntowrapException
	 * @throws URISyntaxException
	 */
	public static void listConceptSentences(String uriOntoBase)
			throws OntowrapException, URISyntaxException {
		OntologyFactory of = OntologyFactory.getFactory();
		LoadedOntology<?> ontology = of.loadOntology(new URI(uriOntoBase));

		for (Object e : ontology.getEntities()) {
			try {
				for (String annot : ontology.getEntityAnnotations(e)) {
					System.out.println(annot);
				}
				URI entUri = ontology.getEntityURI(e);
				if (entUri != null && entUri.getFragment() != null) {
					System.out.println("-- " + entUri.getFragment()
							+ "-----------------");
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

	}

	/**
	 * Obtém uma lista de conceitos/termos.
	 * 
	 * @param toAnalyse
	 * @param words
	 */
	public static void analyseString(String toAnalyse, Collection<String> words) {
		TokenStream tokenS = snowballAnalyzer.tokenStream("", new StringReader(
				toAnalyse));
		TermAttribute termAtt = tokenS.addAttribute(TermAttribute.class);
		try {
			while (tokenS.incrementToken()) {
				words.add(termAtt.term());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
