package br.uern.aridus.roo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.xml.serialize.Printer;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;

public class AServClient {
	Client client;
	ClientConfig cconfig;
	Hashtable<Integer, String> methodClasses;
	Hashtable<Integer, String> methodNames;
	Properties ontologies = new Properties();
	List<Integer> requests = new ArrayList<Integer>();

	private void loadMethods() throws FileNotFoundException, IOException{
		methodClasses = new Hashtable<Integer, String>();
		methodNames = new Hashtable<Integer, String>();
		
		Properties prop = new Properties();
		prop.load(new FileReader("methods.properties"));
		prop.list(System.out);
		Enumeration<Object> keys = prop.keys();
		int id = 0;
		while(keys.hasMoreElements()){
			String key = (String) keys.nextElement();
			methodNames.put(id, key);
			methodClasses.put(id, (String) prop.get(key));
			id++;
		}
	}
	
	private List<Integer> generateRequests() throws FileNotFoundException, IOException{
		
		List<Integer> requests = new ArrayList<Integer>();
		Properties req = new Properties();
		req.load(new FileReader(new File("requests.properties")));
		req.list(System.out);
		Enumeration<Object> keys = req.keys();
		boolean coringa = false;
		while(keys.hasMoreElements()){
			String key = (String) keys.nextElement();
			String value = (String) req.get(key);
			
			if(!coringa && !keys.hasMoreElements()){
				if(key.equalsIgnoreCase("*")){
					coringa = true;
					if(value.equalsIgnoreCase("*")){
						// *=*
						Enumeration<Object> ontosK = ontologies.keys();
						while(ontosK.hasMoreElements()){
							String k = (String) ontosK.nextElement();
							Enumeration<Object> ontosV = ontologies.keys();
							while(ontosV.hasMoreElements()){
								String v = (String) ontosV.nextElement();
								if(!k.equalsIgnoreCase(v)){
							         requests.add(Integer.valueOf(k+""+v));
									 System.out.println(k+""+v);
								}
							}
						}
					}else{
						// *=value
						Enumeration<Object> ontos = ontologies.keys();
						while(ontos.hasMoreElements()){
							String o = (String) ontos.nextElement();
							if(!o.equalsIgnoreCase(value)){
							    requests.add(Integer.valueOf(o+""+value));
							    System.out.println(o+""+value);
							}
						}
					}
				}else{
					if(value.equalsIgnoreCase("*")){
						coringa = true;
						// 1=*
						Enumeration<Object> ontos = ontologies.keys();
						while(ontos.hasMoreElements()){
							String o = (String) ontos.nextElement();
							if(!o.equalsIgnoreCase(key)){
							    requests.add(Integer.valueOf(key+""+o));
							    System.out.println(key+""+o);
							}
						}
					}
				}
			}
			
			if(!coringa){
				if(!key.equalsIgnoreCase("*") && !value.equalsIgnoreCase("*")){
					requests.add(Integer.valueOf(key+""+value));
					System.out.println(key+""+value);
				}
			}
		}
		return requests;
	}
	
	public AServClient() throws FileNotFoundException, IOException {
		cconfig = new DefaultClientConfig();
		client = Client.create(cconfig);
		loadMethods();
		ontologies.load(new FileReader(new File("ontologies.properties")));
		
		Enumeration enumos = ontologies.keys();
		while(enumos.hasMoreElements()){
			Object k = enumos.nextElement();
			Object v = ontologies.get(k);
			File f = new File(v.toString());
			ontologies.put(k, (URI.create(f.getAbsolutePath()).toString()));
		}
		ontologies.list(System.out);
		requests = generateRequests();
	}

	public static void main(String[] args) throws IOException {

		System.out.println("Configurações:");
		AServClient aserv = new AServClient();
		System.out.println("Executando alinhamentos:");
		aserv.executeOnDesk(aserv);
		System.out.println("\nExecução concluída!");
	}
	
	public void executeOnDesk(AServClient aserv) throws IOException{
        
		File output = new File("aserv.output");
		FileWriter fwo = new FileWriter(output);
		int o1, o2, req;
		
		for (int m = 0; m < aserv.methodNames.size(); m++) {
			String methodClass = aserv.methodClasses.get(m);
			String methodName = aserv.methodNames.get(m);
			
			Iterator<Integer> it = aserv.requests.iterator();
			while(it.hasNext()){
				req = it.next();
				o1 = req / 10;
				o2 = req % 10;
					
				String alignapi = "java -cp alignapi/: -jar alignapi/procalign.jar ";
				alignapi = alignapi.concat(" -i "+methodClass);
				alignapi = alignapi.concat(" -t 0.0");
				alignapi = alignapi.concat(" -r fr.inrialpes.exmo.align.impl.renderer.RDFRendererVisitor");
				alignapi = alignapi.concat(" -o ./alignments/alignment-" + o1 +""+ o2 + "-"+methodName+".rdf ");
				
				
				// Criando os URI das Ontologias
				String uriO1 = (String) ontologies.get(String.valueOf(o1));
				String uriO2 = (String) ontologies.get(String.valueOf(o2));
				File fo1 = new File(uriO1);
				File fo2 = new File(uriO2);
				
				URI uri1 = null, uri2 = null;
				try {
					uri1 = new URI(fo1.getAbsolutePath());
					uri2 = new URI(fo2.getAbsolutePath());
				} catch (URISyntaxException e1) {
					e1.printStackTrace();
				}
				
				alignapi = alignapi.concat("file://"+uri1.toString() +
						" file://"+ uri2.toString());
				
				System.out.print("\nRequest alignment [ "+ methodName +" ]: " + o1 + "-" + o2 + " ... ");
				Process p = Runtime.getRuntime().exec(alignapi.toString());
				int signal = 0;
				try {
					signal = p.waitFor();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if(signal==0){
					System.out.print("OK!");
				}
				fwo.append(alignapi.toString()+"\n");
				fwo.flush();
			}
		}
		fwo.close();
	}
	
	public void executeOnService(AServClient aserv) throws Exception{
		throw new Exception("Não implementado!!!"); 
		/**
		File output = new File("c:\\roo-aligns\\aserv.output");
		FileWriter fwo = new FileWriter(output);
		int o1, o2, req;
		String onto1, onto2;
		for (int m = 0; m < aserv.methodNames.size(); m++) {
			String methodClass = aserv.methodClasses.get(m);
			String methodName = aserv.methodNames.get(m);
			
			Iterator<Integer> it = aserv.requests.iterator();
			while(it.hasNext()){
				req = it.next();
				o1 = req / 10;
				o2 = req % 10;
				onto1 = (String) aserv.ontologies.get(o1); // filePrefixo + o1 + fileSufixo; // ontologies.get(o1)
				onto2 = (String) aserv.ontologies.get(o2); // filePrefixo + o2 + fileSufixo; // ...
				String alid = aserv.match(onto1, onto2, methodClass);
				aserv.store(alid);
				aserv.saveRDF(alid, "c:\\roo-aligns\\alignment-" + req + "-"+methodName+".rdf");
				// System.out.println("Save alignment(" + o1 + " , " + o2 + ")");
				fwo.write("----------------------------------------\n");
				fwo.write(onto1 + "\n");
				fwo.write(onto2 + "\n");
				fwo.write(alid + "\n");
				fwo.write("http://aserv.inrialpes.fr/html/retrieve?method=fr.inrialpes.exmo.align.impl.renderer.HTMLRendererVisitor&id="
						+ alid + "\n");
				fwo.write("http://aserv.inrialpes.fr/html/retrieve?method=fr.inrialpes.exmo.align.impl.renderer.RDFRendererVisitor&id="
						+ alid + "\n");
				fwo.flush();
			}
		}
		fwo.close();
	}
	

	public String match(String onto1, String onto2, String method) {
		String alid = "";

		int status = 0;
		while (status != 200) {
			WebResource service = client
					.resource("http://aserv.inrialpes.fr/rest/match")
					.queryParam("onto1", onto1).queryParam("onto2", onto2)
					.queryParam("method", method).queryParam("action", "Match");
			
			System.out.println(service.getURI());
			
			ClientResponse resp = service.get(ClientResponse.class);
			status = resp.getStatus();
			if (resp != null && status == 200) {
				String s = resp.getEntity(String.class);
				System.out.println(s);
				int b = s.indexOf("<alid>");
				int e = s.indexOf("</alid>");
				// System.out.println("------------------------------------------------------------------");
				// System.out.println(onto1);
				// System.out.println(onto2);
				// System.out.println("http://aserv.inrialpes.fr/html/retrieve?method=fr.inrialpes.exmo.align.impl.renderer.HTMLRendererVisitor&id="+s.substring(b+6,
				// e));
				if(b>0 || e>0){
					alid = s.substring(b + 6, e);
					System.out
						.println("http://aserv.inrialpes.fr/html/retrieve?method=fr.inrialpes.exmo.align.impl.renderer.HTMLRendererVisitor&id="
								+ alid);
					if (status == 200)
						System.out.println("OK: Match " + alid);
					else
						System.out.println("FAULT: Macth " + alid);
				}
			}
		}
		return alid;
		**/
	}

	public void store(String alid) throws Exception {
		throw new Exception("Não implementado!!!");
		/*int status = 0;
		do {
			WebResource service = client
					.resource("http://aserv.inrialpes.fr/rest/store")
					.queryParam("id", alid).queryParam("action", "Store");

			ClientResponse resp = service.get(ClientResponse.class);
			status = resp.getStatus();
			if (status == 200)
				System.out.println("OK: Store " + alid);
			else
				System.out.println("FAULT: Store " + alid);
		} while (status != 200);*/
	}

	public void saveRDF(String alid, String filename) throws Exception {
		throw new Exception("Não implementado!!!");
		/*File f = new File(filename);
		FileWriter fw = new FileWriter(f);

		int status = 0;
		do {
			WebResource service = client
					.resource("http://aserv.inrialpes.fr/html/retrieve")
					.queryParam("action", "retrieve")
					.queryParam("method",
							"fr.inrialpes.exmo.align.impl.renderer.RDFRendererVisitor")
					.queryParam("id", alid);

			ClientResponse resp = service.get(ClientResponse.class);
			status = resp.getStatus();
			if (status == 200) {
				System.out.println("OK: Retrieve " + alid);
				BufferedReader bfr = new BufferedReader(new InputStreamReader(
						resp.getEntityInputStream()));
				while (bfr.ready()) {
					String str = bfr.readLine();
					fw.write(str + "\n");
				}
				bfr.close();
				fw.flush();
				fw.close();
			} else
				System.out.println("FAULT: Retrieve " + alid);
		} while (status != 200);
		// URL url = new URL(
		// "http://aserv.inrialpes.fr/html/retrieve?method=fr.inrialpes.exmo.align.impl.renderer.RDFRendererVisitor&id="
		// + alid);
		// URLConnection con = url.openConnection();
		// BufferedReader bfr = new BufferedReader(new InputStreamReader(
		// con.getInputStream()));
		// while (bfr.ready()) {
		// String str = bfr.readLine();
		// fw.write(str+"\n");
		// }
		// bfr.close();
		// fw.flush();
		// fw.close();
*/	}
}
