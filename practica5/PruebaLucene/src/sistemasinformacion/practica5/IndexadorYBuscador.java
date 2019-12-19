package sistemasinformacion.practica5;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.document.TextField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.Field;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.es.SpanishAnalyzer;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;


import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.io.BufferedReader;
import java.io.File;


/**
 * Clase de un indexador y buscador usando Lucene
 * @author Andrés Gavín Murillo
 * @author Eduardo Gimeno Soriano
 * @author Sergio Álvarez Peiro
 *
 */
public class IndexadorYBuscador {
	
	private IndexWriter indice = null;
	
	private Directory directorioDelIndiceCreado = null;

	/**
	 * Analizador utilizado por el indexador / buscador 
	 */
	private Analyzer analizador;
	
	

	/**
	 * Constructor parametrizado
	 * @param ficherosAIndexar Directorio a indexar
	 * @throws IOException 
	 */
	public IndexadorYBuscador() throws IOException {
		this.analizador = new SpanishAnalyzer();
		this.directorioDelIndiceCreado = crearIndiceEnUnDirectorio();
	}
	
	/**
	 * Elimina el índice
	 * @throws IOException
	 */
	public void eliminarIndice() throws IOException {
		indice.deleteAll();
		indice.close();
		
		for (String s : directorioDelIndiceCreado.listAll())
			directorioDelIndiceCreado.deleteFile(s);
		directorioDelIndiceCreado.close();
	}
	
	
	
	/**
	 * Añade un fichero al índice
	 * @param path ruta del fichero a indexar
	 * @throws IOException
	 */
	private void anhadirFichero(String path) 
	throws IOException {
		File file = new File(path);
	    if (!file.exists() || !file.canRead()) {
	      System.out.println("El fichero '" + path + "' no existe o no se puede leer");
	      return;
	    }
	    
		InputStream inputStream = new FileInputStream(file);
		BufferedReader inputStreamReader = new BufferedReader(
				new InputStreamReader(inputStream,"UTF-8"));
		
		Document doc = new Document();   
		doc.add(new TextField("contenido", inputStreamReader));
		doc.add(new StringField("path", path, Field.Store.YES));
		indice.addDocument(doc);
		indice.commit();
	}
	
	
	
	/**
	 * Añade un directorio al índice
	 * @param path ruta del directorio a indexar
	 * @throws IOException
	 */
	private void anhadirDirectorio(String path) 
	throws IOException {
		if (!path.endsWith("/"))
			path += "/";
		
		File docDir = new File(path);
	    if (!docDir.exists() || !docDir.canRead()) {
	      System.out.println("El directorio '" + path + "' no existe o no se puede leer");
	      return;
	    }
	    
	    String[] documentos = docDir.list();
        if (documentos != null)
        	for (String doc : documentos) {
        		if (new File(path + doc).isDirectory())
        			anhadirDirectorio(path + doc);
        		else
            		anhadirFichero(path + doc);
        	}
	}
	
	
	
	/**
	 * Crea el índice vacío en "directorioAIndexar"
	 * @return un índice (Directory) en memoria
	 * @throws IOException
	 */
	private Directory crearIndiceEnUnDirectorio() throws IOException {
		String indexDir = "indice";
		
		Directory directorioAlmacenarIndice = new MMapDirectory(Paths.get(indexDir));

		IndexWriterConfig configuracionIndice = new IndexWriterConfig(analizador);

		indice = new IndexWriter(directorioAlmacenarIndice, configuracionIndice);
		
		return directorioAlmacenarIndice;
	}
	
	
	
	/**
	 * Busca la palabra indicada en queryAsString en el directorioDelIndice.
	 * @param hitsPorPagina
	 * @param queryAsString
	 * @throws IOException
	 */
	private void buscarQueryEnIndice(int hitsPorPagina, String queryAsString)
	throws IOException{

		DirectoryReader directoryReader = DirectoryReader.open(directorioDelIndiceCreado);
		IndexSearcher buscador = new IndexSearcher(directoryReader);
		
		QueryParser queryParser = new QueryParser("contenido", analizador); 
		Query query = null;
		try{
			query = queryParser.parse(queryAsString);
			TopDocs resultado = buscador.search(query, indice.numDocs() * hitsPorPagina);
			ScoreDoc[] hits = resultado.scoreDocs;
		      
			System.out.println("\nBuscando " + queryAsString + ": Encontrados " + hits.length + " hits.");
			int i = 0;
			for (ScoreDoc hit: hits) {
				int docId = hit.doc;
				
				Document doc = buscador.doc(docId);
				System.out.println((++i) + ". " + doc.get("path") + "\t" + hit.score);
			}

		}catch (ParseException e){
			throw new IOException(e);
		}	
	}
	
	
	
	/**
	 * Programa principal
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[]args) throws IOException{
		final String ayuda = "1.- Indexar un directorio\n"
				+ "2.- Añadir un documento al índice\n"
				+ "3.- Buscar término\n"
				+ "4.- Salir\n";
		boolean fin = false;
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in, "UTF-8"));
		IndexadorYBuscador indexador = new IndexadorYBuscador();
		
		while (!fin) {
			System.out.print(ayuda);
			String entrada = in.readLine();
			if (entrada == null || entrada.length() == -1) {
				break;
			}
			entrada = entrada.trim();
			if (entrada.length() == 0) {
				break;
			}
			
			if (entrada.charAt(0) == '1') {
				System.out.println("Introduzca la ruta de un directorio a indexar: ");
				String directorio = in.readLine();
				indexador.anhadirDirectorio(directorio);
			}
			else if (entrada.charAt(0) == '2') {
				System.out.println("Introduzca el nombre de un documento a indexar: ");
				String doc = in.readLine();
				indexador.anhadirFichero(doc);
			}
			else if (entrada.charAt(0) == '3') {
				System.out.println("Introduzca el término a buscar: ");
				String term = in.readLine();
				indexador.buscarQueryEnIndice(1, term);
				System.out.println();
			}
			else if (entrada.charAt(0) == '4') {
				indexador.eliminarIndice();
				fin = true;
			}
		}
	}
}


