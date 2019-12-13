package sistemasinformacion.practica5;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.document.TextField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.Field;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.es.SpanishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;


import java.util.ArrayList;
import java.util.Collection;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.io.BufferedReader;


/**
 * Clase de ejemplo de un indexador y buscador usando Lucene
 * @author sisinf
 *
 */
public class IndexadorYBuscador{

	/**
	 * Relación de ficheros a indexar / buscar
	 */
	private Collection <String> ficherosAIndexar = new ArrayList<String>();
	/**
	 * Relación de palabras clave a buscar
	 */
	private Collection <String> queries = new ArrayList <String>();
	/**
	 * Analizar utilizado por el indexador / buscador 
	 */
	private Analyzer analizador;
	
	private final static String INDEXDIR = "./ficheros/indice";
	
	

	/**
	 * Constructor parametrizado
	 * @param ficherosAIndexar Colección de ficheros a indexar
	 * @param queries Colección de palabras a buscar
	 */
	public IndexadorYBuscador(Collection<String> ficherosAIndexar, Collection<String> queries){
		this.ficherosAIndexar = ficherosAIndexar;
		this.queries = queries;
		
		analizador = new SimpleAnalyzer();

/*		try {
			FileReader reader = new FileReader("./ficheros/stopwords.txt");
			analizador = new StandardAnalyzer(reader);
		} catch (Exception e) {
			System.out.println("Error leyendo fichero de Stop Words. Usando valor por defecto");
			analizador = new StandardAnalyzer();
		} 
*/
		//analizador = new SpanishAnalyzer();

	
	}
	
	
	
	/**
	 * Añade un fichero al índice
	 * @param indice Indice que estamos construyendo
	 * @param path ruta del fichero a indexar
	 * @throws IOException
	 */
	private void anhadirFichero(IndexWriter indice, String path) 
	throws IOException {
		InputStream inputStream = new FileInputStream(path);
		BufferedReader inputStreamReader = new BufferedReader(
				new InputStreamReader(inputStream,"UTF-8"));
		
		Document doc = new Document();   
		doc.add(new TextField("contenido", inputStreamReader));
		doc.add(new StringField("path", path, Field.Store.YES));
		indice.addDocument(doc);
	}
	
	
	
	/**
	 * Indexa los ficheros incluidos en "ficherosAIndexar"
	 * @return un índice (Directory) en memoria, con los índices de los ficheros
	 * @throws IOException
	 */
	private Directory crearIndiceEnUnDirectorio() throws IOException{
		IndexWriter indice = null;
		Directory directorioAlmacenarIndice = new MMapDirectory(Paths.get(INDEXDIR));

		IndexWriterConfig configuracionIndice = new IndexWriterConfig(analizador);

		indice = new IndexWriter(directorioAlmacenarIndice, configuracionIndice);
		
		for (String fichero : ficherosAIndexar) {
			anhadirFichero(indice, fichero);
		}
		
		indice.close();
		return directorioAlmacenarIndice;
	}
	
	
	
	/**
	 * Busca la palabra indicada en queryAsString en el directorioDelIndice.
	 * @param directorioDelIndice
	 * @param paginas
	 * @param hitsPorPagina
	 * @param queryAsString
	 * @throws IOException
	 */
	private void buscarQueryEnIndice(Directory directorioDelIndice, 
										int paginas, 
										int hitsPorPagina, 
										String queryAsString)
	throws IOException{

		DirectoryReader directoryReader = DirectoryReader.open(directorioDelIndice);
		IndexSearcher buscador = new IndexSearcher(directoryReader);
		
		QueryParser queryParser = new QueryParser("contenido", analizador); 
		Query query = null;
		try{
			query = queryParser.parse(queryAsString);
			TopDocs resultado = buscador.search(query, paginas * hitsPorPagina);
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
	 * Ejecuta en el índice una búsqueda por cada una de las palabras clave solicitadas. <p>
	 * Las palabras clave solicitadas están en la propiedad global "queries". 
	 * @param directorioDelIndice índice
	 * @param paginas 
	 * @param hitsPorPagina
	 * @throws IOException
	 */
	private void buscarQueries(Directory directorioDelIndice, int paginas, int hitsPorPagina)
	throws IOException{
		for (String palabra : queries) {
			buscarQueryEnIndice(directorioDelIndice, 
								paginas, 
								hitsPorPagina, 
								palabra);			
		}
	}
	
	
	
	/**
	 * Programa principal de prueba. Rellena las colecciones "ficheros" y "queries"
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[]args) throws IOException{
		// Establecemos la lista de ficheros a indexar
		Collection <String> ficheros = new ArrayList <String>();
		ficheros.add("./ficheros/uno.txt");
		ficheros.add("./ficheros/dos.txt");
		ficheros.add("./ficheros/tres.txt");
		ficheros.add("./ficheros/cuatro.txt");

		// Establecemos las palabras clave a utilizar en la búsqueda
		Collection <String> queries = new ArrayList <String>();
		queries.add("Contaminación");
		queries.add("cambio climatico");
		queries.add("cambio climático");
		queries.add("cambio");
		queries.add("climatico");
		queries.add("por");
		queries.add("aeropuerto");

		// Creamos el idexador / buscador
		IndexadorYBuscador ejemplo = new IndexadorYBuscador(ficheros, queries);

		// Indexamos los ficheros
		Directory directorioDelIndiceCreado = ejemplo.crearIndiceEnUnDirectorio();
		
		// Abrimos un ficher indexado previamente
		//Directory directorioDelIndiceCreado = MMapDirectory.open(Paths.get(INDEXDIR));
		
		// Ejecutamos la búsqueda de las palabras clave
		ejemplo.buscarQueries(directorioDelIndiceCreado, ficheros.size(), 1);
	}
	
}


