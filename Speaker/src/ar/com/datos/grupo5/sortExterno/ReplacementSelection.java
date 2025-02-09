
package ar.com.datos.grupo5.sortExterno;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ar.com.datos.grupo5.Constantes;


/**
 * @author Led Zeppelin
 * @version 1.0
 * @since 04-May-2009 04:16:43 p.m.
 */
public class ReplacementSelection {

	/**
	 * Nombre del archivo con los registros a ordenar.
	 */
	private String arch;
	
	/**
	 * Lista con los pares idTermino - Documento.
	 */
	private List<NodoRS> listaNodoDisponibles;
	
	/**
	 * Lista de nodos Congelados 
	 */
	private List<NodoRS> listaNodoCongelados;
	
	/**
	 * Lista de particiones generadas.
	 */
	private ArrayList<String> listaParticiones;
	
	/**
	 * Memoria que voy a usar como ventana de datos 
	 * para las particiones.
	 */
	private int memoria;
	
	private RandomAccessFile archivoTrabajo;
	/**
	 * Cantidad de nodos que entran en la memoria asignada.
	 */
	private int cantidadNodos;
	
	/**
	 * Contructor de copia por defecto.
	 */
	public ReplacementSelection() {
		this.listaNodoDisponibles = new ArrayList<NodoRS>();
		this.listaNodoCongelados = new ArrayList<NodoRS>();
		this.listaParticiones = new ArrayList<String>();
	}
	
	public void finalize() throws Throwable {
	}

	/**
	 * Define el nombre del archivo y lee la cantidad de memoria 
	 * en bytes a usar.
	 * @param archExt Nombre del archivo que tiene 
	 * los elementos a ordena.
	 */
	public ReplacementSelection(final String archExt) {
		this.arch = archExt;
		this.memoria = Constantes.TAMANIO_BUFFER_REPLACEMENT_SELECTION;
		this.listaNodoDisponibles = new ArrayList<NodoRS>();
		this.listaNodoCongelados = new ArrayList<NodoRS>();
		this.listaParticiones = new ArrayList<String>();
	}
	
	/**
	 * Devuelve la cantidad de elementos no congelados.
	 * @return Cantidad de elementos no congelados.
	 */
	private int cuantosDisponibles() {
		
		/*
		Iterator<NodoRS> it;
		NodoRS nodo;
		
		it = this.listaNodo.iterator();
		int cont = 0;
		
		while (it.hasNext()) {
			nodo = it.next();
			if (nodo.getFlag() == 0) {
				cont++;
			}
		}
		*/
		return this.listaNodoDisponibles.size();
	}

	/**
	 * Convierte los nodos congelados en nodos diponibles.
	 * @return 
	 */
	private void hacerDisponible() {
		
		this.listaNodoDisponibles.addAll(this.listaNodoCongelados);
		this.listaNodoCongelados.clear();
	}
	
	/**
	 * 
	 * @return
	 * 		devuelve true si todos los nodos est�n congelados.
	 * False si no lo est�n.
	 */
	private boolean nodoRSCongelado() {
		
		/*
		int cantCongelada = 0;
		Iterator<NodoRS> it = this.listaNodo.iterator();
		NodoRS nodo;
		while (it.hasNext()) {
			nodo = it.next();
			if (nodo.getFlag() == 1) {
				cantCongelada++;	
			}
		}
		*/
		return (this.listaNodoDisponibles.isEmpty());
	}

	/**
	 * Verifica si la lista tiene m�s elementos.
	 * @return True si no tiene m�s elementos. False si a�n tiene.
	 */
	private boolean nodoRSVacio() {
		return this.listaNodoDisponibles.isEmpty();
	}

	/**
	 * Busca el menor de los nodos.
	 * @return el menor de los nodos.
	 */
	private NodoRS obtenerMenorNodo() {
		
//		Comparator<NodoRS> comp = Collections.reverseOrder();
//		
//		Collections.sort(this.listaNodo, comp);
		
		return Collections.min(this.listaNodoDisponibles);
	}

	/**
	 * Realiza la carga inicial de la lista de elementos disponibles.
	 */
	public final int cargarInicialListaDisponibles(final long nRegistros) {
		
		NodoRS nodo = new NodoRS();
		byte[] dataNodo = new byte[nodo.getTamanio()];
		int cantidadNodosCargados = 0;
		
		
		for (int i = 0; i < this.cantidadNodos && i < nRegistros; i++) {
			try {
				archivoTrabajo.read(dataNodo, 0, dataNodo.length);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				return cantidadNodosCargados;
			}
			nodo.setBytes(dataNodo);
			this.listaNodoDisponibles.add(nodo);
			nodo = new NodoRS();
			cantidadNodosCargados++;
		}
		return cantidadNodosCargados;
	}
	
	
	
	public final void agregarNuevoNodo(final NodoRS menor) {
		int comp;
		NodoRS nodo = new NodoRS();
		byte[] dataNodo = new byte[nodo.getTamanio()];
		try {
			archivoTrabajo.read(dataNodo, 0, dataNodo.length);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		nodo.setBytes(dataNodo); //idT,idD,fdt

		//4.4.1-si reg(f) <= grabado -> congelado
		comp = nodo.compareTo(menor);
		if (comp == -1 || comp == 0) {
			this.listaNodoCongelados.add(nodo);
		}
		//4.4.2-si reg(f) > grabado -> disponible
		if (comp == 1) {
			this.listaNodoDisponibles.add(nodo);
		}
	}
	
	public final void guardarDisponibles(final RandomAccessFile particionActual) {
		NodoRS menor = new NodoRS();
		int desc = cuantosDisponibles();
		byte[] dataNodo = new byte[menor.getTamanio()];
		
		//guardo los disponibles que quedaron en la particion 
		for (int j = 0; j < desc; j++) {
			menor = obtenerMenorNodo();
			//4.3-lo grabo en Pi y lo elimino de listaNodo
			dataNodo = menor.getBytes();
			try {
				particionActual.write(dataNodo, 0, dataNodo.length);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return;
			}
			this.listaNodoDisponibles.remove(menor);
		}

	}
	
	/**
	 * Metodo que se encarga de ordenar lo elementos leidos y
	 * generar las particiones.
	 * @return cantidad de particiones.
	 */
	public final int ordenar() {
	//	int flag, desc, comp;
		long nRegistros, cont = 0;
		NodoRS nodo = new NodoRS();
	//	NodoRS nodo1 = new NodoRS();
		NodoRS menor;
		byte[] dataNodo = new byte[nodo.getTamanio()];
		Integer particionNumero = new Integer(1);
		RandomAccessFile p;
		
		//1-calculo tama�o de lista de objetos a ordenar
		this.cantidadNodos = this.memoria / nodo.getTamanio();
		
		//2-abro el archivo
		try {
			archivoTrabajo = new RandomAccessFile(arch,Constantes.ABRIR_PARA_LECTURA_ESCRITURA);
			if (archivoTrabajo != null) {
				//Cantidad de regsitros en el archivo
				nRegistros = archivoTrabajo.length() / nodo.getTamanio();
				archivoTrabajo.seek(0);
				
				cont = cargarInicialListaDisponibles(nRegistros);
				
				while (!nodoRSCongelado() && !nodoRSVacio()) {
					//4-arranca el bucle (mientras el buffer no este vacio ni congelado)
					//4.1-creo Pi
					String nombreParticion = new String(this.arch + "part" + particionNumero.toString()); 
					p = new RandomAccessFile(nombreParticion,Constantes.ABRIR_PARA_LECTURA_ESCRITURA);
					
					while (!nodoRSCongelado() && !nodoRSVacio()) {
						
						//4.2-tomo el menor del buffer con flag en disponible
						menor = obtenerMenorNodo();
						
						//4.3-lo grabo en Pi y lo elimino de listaNodo
						dataNodo = menor.getBytes();
						p.write(dataNodo, 0, dataNodo.length);
						this.listaNodoDisponibles.remove(menor);
						
						//4.4-leo el siguiente del archivo f
						if (cont < nRegistros) {
							//Levanto un nuevo nodo para la particion actual
							agregarNuevoNodo(menor);
							cont++;
							
						} else {
							//Guardo los nodos que aun siguen disponibles.
							guardarDisponibles(p);
							
						}
					}
					//5-cierro Pi y registro que la guarde
					p.close();
					listaParticiones.add(nombreParticion);
					//6-pongo los flag en disponible y vuelvo a empezar
					hacerDisponible();
					particionNumero++;
				}
				archivoTrabajo.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
			return 0;
		}
		return this.listaParticiones.size();
	}

	/**
	 * @return La lista
	 */
	public final ArrayList<String> getListaParticiones() {
		return this.listaParticiones;
	}
	
	/**
	 * @return La lista
	 */
	public final String getArch() {
		return this.arch;
	}
}
