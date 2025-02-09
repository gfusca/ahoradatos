package ar.com.datos.grupo5.btree;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import ar.com.datos.grupo5.Constantes;
import ar.com.datos.grupo5.registros.RegistroNodo;
import ar.com.datos.grupo5.utils.Conversiones;

/**
 * Representa el nodo de arbol b.
 * @author LedZeppeling
 *
 */
public class Nodo {

	/**
	 * Logger.
	 */
	private static final Logger LOG = Logger.getLogger(Nodo.class);
	
	/**
	 * Flag de desborde.
	 */
	private boolean overflow;	
	/**
	 * 
	 * Espacio en el nodo.
	 */
	private int minIndiceCarga;
	
	/**
	 * Espacio en el nodo.
	 */
	private int espacioTotal;

	/**
	 * Espacio ocupado.
	 */
	private int espacioOcupado;
	
	/**
	 * Nro de bloque.
	 */
	private Integer nroBloque;
		
	/**
	 * Si el nodo es hoja o no.
	 */
	private boolean esHoja = false;
	
	/**
	 * Lista de registros.
	 */
	public ArrayList< RegistroNodo > registros;
	
	/**
	 * El nodo padre.
	 */
	private Integer nroBloquePadre;
	
	/**
	 * El Numero de Bloque del Secuencial Set.
	 */
	private Integer punteroBloque = 0;
	
	/**
	 * Constructor.
	 */
	public Nodo() {
		
		nroBloquePadre = null;
		registros = new ArrayList<RegistroNodo>();
		overflow = false;
		minIndiceCarga = -1;
		espacioTotal = Constantes.SIZE_OF_INDEX_BLOCK
				- Constantes.SIZE_OF_ADMIN_NODE_DATA - Constantes.SIZE_OF_INT;
		espacioOcupado = 0;
	}
	
	/**********************
	 * Metodos
	 **********************/

	/**
	 * Verifica la existencia de una clave.
	 * @param clave .
	 * @return true si la clave existe.
	 */
	public final boolean existeClave(final Clave clave) {

		return this.registros.contains(clave);
	}
	
	/**
	 * Busca una clave en el nodo.
	 * 
	 * @param clave
	 *            .
	 * @return El indice en el array en donde esta el registro que contiene la
	 *         clave o del primer elemento que es mayor a la clave. 
	 */
	public final int buscarRegistro(final Clave clave) {

		int pos = -1;
		int resultado = 0;
		
		if (registros.size() == 0) {
			return 0;
		}
		
		// Si la clave es menor a la primera, no está.
		if (clave.compareTo(registros.get(0).getClave()) < 0) {
			return Constantes.MENOR;
		}

		// Si la clave es mayor a la ultima, no está.
		if (clave.compareTo(registros.get(registros.size() - 1)
				.getClave()) > 0) {
			return Constantes.MAYOR;
		}
		
		//Recorro los nodos en busca de la clave.
		for (RegistroNodo reg : this.registros) {
			pos++;
			
			resultado = reg.getClave().compareTo(clave);
			if (resultado >= 0) {
				return pos;
			}
		}
		
		return pos;
	}
	
	/**
	 * Agrega un nodo.
	 * @param registro El reistro para insertar.
	 * @return El resultado de la insercion.
	 * @throws IOException .
	 */
	public final boolean insertarRegistro(final RegistroNodo registro)
			throws IOException {
		
		//Obtengo la posicion en donde debo insertarlo.
		ocupar(registro.getBytes().length);
		int pos = this.buscarRegistro(registro.getClave());
		switch (pos) {
		case Constantes.MENOR:
				this.registros.add(0, registro);
			break;
		case Constantes.MAYOR:
				this.registros.add(registros.size(), registro);
			break;
		default:
				// Me fijo si ya lo contiene.
			// if (registros.get(pos).getClave().equals(registro.getClave())) {
			// return true;
			// }
				this.registros.add(pos, registro);
			break;
		}
		tieneCargaMinima();
		if (overflow) {
			return false;
		}
		return true;
	}
	
	/**
	 * Remueve el registro en la posicion pos.
	 * 
	 * @param pos el indice del registro que se quiere eliminar.
	 * @return el regstro que se elimina.
	 *            .
	 * @throws IOException .
	 */
	public final RegistroNodo removerRegistro(final int pos) 
		throws IOException {

		RegistroNodo reg = registros.remove(pos);
		ocupar(-reg.getBytes().length);
		return reg;
	}
	
	/**
	 * @param registro the registro to set
	 * @return .
	 */
	public final boolean setRegistro(final RegistroNodo registro) {
		int pos = this.buscarRegistro(registro.getClave());
		if (pos >= 0) {
			this.registros.set(pos, registro);
			return true;
		}
		return false;
	}
	
	/**
	 * Para testear.
	 */
	public final void listar() {
		
		System.out.println();
		System.out.println("Contenido del nodo: [" + nroBloque + "] Padre: ["
				+ nroBloquePadre + "] FactMinCarga: " + minIndiceCarga 
				+ " Esp. Total: " + espacioTotal + " Esp. Ocupado: "
				+ espacioOcupado + " Tiene Overflow: " + isOverflow());
		System.out.println("Apunta al bloque numero: " + punteroBloque);
		for (RegistroNodo reg : registros) {
			System.out.println("==== " + reg.getClave().getClave()
					+ " Puntero Izquierdo: [" + reg.getNroBloqueIzquierdo()
					+ "] Puntero derecho: [" + reg.getNroBloqueDerecho()
					+ "] ");
		}
	}
	
	/**
	 * La raiz cuando hace split, genera 2 nodos nuevos. Uno de estos nodos va a
	 * pasar a ser la nueva raiz, y los otros dos sus hijos. El nodo actual, que
	 * es la raiz, por comodidad va a pasar a ser un hijo ya que la raiz solo va
	 * a contener un registro y es mas facil crear un nodo nuevo.
	 * 
	 * @param ultimoNroBloque el numero del ultimo bloque asignado.
	 * @return the nodos
	 * @throws IOException .
	 */
	public final ArrayList<Nodo> splitRaiz(final int ultimoNroBloque)
			throws IOException {
		
		ArrayList<Nodo> nodos = new ArrayList<Nodo>();
		Nodo nodo = new Nodo();
		Nodo nuevaRaiz = new Nodo();
		
		this.setOverflow(false);
		this.setNroBloquePadre(Constantes.NRO_BLOQUE_RAIZ);
		this.setNroBloque(ultimoNroBloque + 1);
		nodo.setNroBloquePadre(Constantes.NRO_BLOQUE_RAIZ);
		nodo.setNroBloque(ultimoNroBloque + 2);
		nodo.setEsHoja(this.isHoja());
		nuevaRaiz.setNroBloquePadre(-1);
		nuevaRaiz.setNroBloque(Constantes.NRO_BLOQUE_RAIZ);
		nuevaRaiz.setEsHoja(false);
		
		// Llenar nodo hno (nodo)
		while (minIndiceCarga < registros.size()) {
			RegistroNodo reg = removerRegistro(minIndiceCarga);
			nodo.insertarRegistro(reg);
		}
		if (nodo.getCantidadRegistros() == 0) {
			RegistroNodo reg = removerRegistro(getCantidadRegistros() - 1);
			nodo.insertarRegistro(reg);
		}
		// Cargar Nueva raiz (nodoAux)
		RegistroNodo reg = new RegistroNodo();
		if (!this.isHoja()) {
			// Liberar la clave de la nueva raiz de la raiz anterior
			reg.setClave(this.getUltimoRegistro().getClave());
			removerRegistro(getCantidadRegistros() - 1);
		} else {
			reg.setClave(nodo.getPrimerRegistro().getClave());
		}
		reg.setNroBloqueIzquierdo(this.getNroBloque());
		reg.setNroBloqueDerecho(nodo.getNroBloque());
		nuevaRaiz.insertarRegistro(reg);
		nodos.add(nuevaRaiz);
		nodos.add(nodo);
		return nodos;
	}
	
	/**
	 * @param hayNodoDerecho es el nodo con el cual lo tengo que tratar para dividir.
	 * @param nodoHermano es el hermano del nodo que tengo que splitear
	 * @param nodoPadre El padre del nodo y su hermano
	 * @param ultimoNroBloque .
	 * @return the nodos
	 * @throws IOException ,
	 */
	public final Nodo split(final Nodo nodoHermano, final Nodo nodoPadre,
			final boolean hayNodoDerecho, final int ultimoNroBloque)
			throws IOException {
		
		Nodo nuevoHermano = new Nodo();
		nuevoHermano.setNroBloque(ultimoNroBloque + 1);
		nuevoHermano.setNroBloquePadre(getNroBloquePadre());
		nuevoHermano.setEsHoja(this.isHoja());
		ArrayList<RegistroNodo> regs = new ArrayList<RegistroNodo>();
		int pos = nodoPadre.buscarRegistro(nodoHermano
				.getPrimerRegistro().getClave());

		if (hayNodoDerecho) {
			// -> El nuevo nodo es MAYOR que el nodo actual.
			// Paso todos los regs hasta el que garantiza el 66% de ocupacion
			// Primero paso los del nodo actual
			RegistroNodo regAux = new RegistroNodo();
			regAux.setNroBloqueDerecho(nodoHermano
					.getPrimerRegistro().getNroBloqueIzquierdo());
			regAux.setNroBloqueIzquierdo(getUltimoRegistro()
					.getNroBloqueDerecho());
			while (minIndiceCarga < registros.size()) {
				RegistroNodo reg = removerRegistro(minIndiceCarga);
				regs.add(reg);
			}

			if (!this.isHoja()) {
				// Liberar la clave de la nueva raiz de la raiz anterior
				// Recordar que el nodoDerecho esta en pos->izq o en pos-1->der
				pos = nodoPadre.buscarRegistro(getPrimerRegistro().getClave());
				if (pos == Constantes.MENOR) {
					pos = 0;
				}
				
				regAux.setClave(nodoPadre.registros.get(pos).getClave());
				regs.add(regAux);
				
				RegistroNodo regAux2 = new RegistroNodo();
				regAux2 = regs.remove(0);
				if (!regAux.getClave().equals(regAux2.getClave())) {
					int espacio = nodoPadre.getEspacioOcupado();
					espacio += diferenciaClaves(nodoPadre.getRegistros().get(
						pos), regAux2);
					nodoPadre.setEspacioOcupado(espacio);
					//El que tiene que subir al padre no es el que estaba
					nodoPadre.registros.get(pos).setClave(regAux2.getClave());
					//Setearle a los hijos los padres
				}
				// ahora, tengo que vaciar el hno y llenarlo otra vez
				regs.addAll(nodoHermano.getRegistros());
				nodoHermano.getRegistros().clear();
				nodoHermano.setEspacioOcupado(0);
				
				//ahora lo cargo hasta la minima ocupacion
				nodoHermano.setMinIndiceCarga(Constantes.MENOR);
				while ((regs.size() > 0) && (!nodoHermano.tieneCargaMinima())) {
					RegistroNodo reg = regs.remove(0);
					nodoHermano.insertarRegistro(reg);
				}
				// Luego lleno el nodo con lo del hermano siguiente
				regAux = new RegistroNodo();
				regAux2 = new RegistroNodo();
				regAux2 = regs.remove(0);

				if (regs.size() == 0) {
					nuevoHermano.insertarRegistro(regAux2);
					regAux2 = new RegistroNodo();
					regAux2.setClave(nodoHermano.getUltimoRegistro()
							.getClave());
					regAux2.setNroBloqueDerecho(nuevoHermano.getNroBloque());
					regAux2.setNroBloqueIzquierdo(nodoHermano.getNroBloque());
					nodoPadre.insertarRegistro(regAux2);
					nodoHermano.removerRegistro(nodoHermano.registros.size() - 1);
				} else {
					regAux = new RegistroNodo();
					regAux.setClave(regAux2.getClave());
					regAux.setNroBloqueDerecho(nuevoHermano.getNroBloque());
					regAux.setNroBloqueIzquierdo(nodoHermano.getNroBloque());
					nodoPadre.insertarRegistro(regAux);
				}
				while (regs.size() > 0) {
					RegistroNodo reg = regs.remove(0);
					nuevoHermano.insertarRegistro(reg);
				}
				
			} else {
				if(pos == Constantes.MAYOR) {
					pos = nodoPadre.getCantidadRegistros() - 1; 
				}
				// ahora, tengo que vaciar el hno y llenarlo otra vez
				regs.addAll(nodoHermano.getRegistros());
				nodoHermano.getRegistros().clear();
				nodoHermano.setEspacioOcupado(0);
				//ahora lo cargo el hermano hasta la minima ocupacion
				nodoHermano.setMinIndiceCarga(Constantes.MENOR);
				while ((regs.size() > 0) && (!nodoHermano.tieneCargaMinima())) {
					RegistroNodo reg = regs.remove(0);
					nodoHermano.insertarRegistro(reg);
				}
				if (regs.size() == 0) {
					RegistroNodo reg = nodoHermano.removerRegistro(nodoHermano.getCantidadRegistros() - 1);
					regs.add(reg);
				}
				// Luego lleno el nodo con lo del hermano siguiente
				while (regs.size() > 0) {
					RegistroNodo reg = regs.remove(0);
					nuevoHermano.insertarRegistro(reg);
				}
				int espacio = nodoPadre.getEspacioOcupado();
				espacio += diferenciaClaves(nodoPadre.getRegistros().get(
					pos), nodoHermano.getPrimerRegistro());
				nodoPadre.setEspacioOcupado(espacio);
				// Ahora tengo que cargar las claves en el padre, y listo!
				nodoPadre.getRegistros().get(pos).setClave(
						nodoHermano.getPrimerRegistro().getClave());			
				RegistroNodo reg = new RegistroNodo();
				reg.setClave(nuevoHermano.getPrimerRegistro().getClave());
				reg.setNroBloqueIzquierdo(nodoHermano.getNroBloque());
				reg.setNroBloqueDerecho(nuevoHermano.getNroBloque());
				nodoPadre.insertarRegistro(reg);
				pos = nodoPadre.buscarRegistro(reg.getClave());
				if(pos == Constantes.MAYOR) {
					pos = nodoPadre.getCantidadRegistros() - 1; 
				}
				if (pos < nodoPadre.registros.size() - 1) {
					nodoPadre.getRegistros().get(pos + 1)
					.setNroBloqueIzquierdo(reg.getNroBloqueDerecho());				
				}
			}
			
		} else {
			RegistroNodo regAux = new RegistroNodo();
			
			regAux.setNroBloqueDerecho(getPrimerRegistro()
					.getNroBloqueIzquierdo());
			regAux.setNroBloqueIzquierdo(nodoHermano
					.getUltimoRegistro().getNroBloqueDerecho());
			
			while (nodoHermano.minIndiceCarga < nodoHermano.registros.size()) {
				RegistroNodo reg = nodoHermano.removerRegistro(nodoHermano.
						minIndiceCarga);
				regs.add(reg);
			}
			if (!this.isHoja()) {
				// Liberar la clave de la nueva raiz de la raiz anterior
				if (pos == Constantes.MENOR) {
					pos = 0;
				}
				regAux.setClave(nodoPadre.registros.get(pos).getClave());
				
				regs.add(regAux);
				
				RegistroNodo regAux2 = new RegistroNodo();
				regAux2 = regs.remove(0);
				if (!regAux.getClave().equals(regAux2.getClave())) {
					int espacio = nodoPadre.getEspacioOcupado();
					espacio += diferenciaClaves(nodoPadre.getRegistros().get(
						pos), regAux2);
					nodoPadre.setEspacioOcupado(espacio);
					nodoPadre.registros.get(pos).setClave(regAux2.getClave());
				}
				// ahora, tengo que vaciar el hno y llenarlo otra vez
				regs.addAll(getRegistros());
				getRegistros().clear();
				setEspacioOcupado(0);		
				//ahora lo cargo hasta la minima ocupacion
				setMinIndiceCarga(Constantes.MENOR);
				while ((regs.size() > 0) && (!tieneCargaMinima())) {
					RegistroNodo reg = regs.remove(0);
					insertarRegistro(reg);
				}
				// Luego lleno el nodo con lo del hermano siguiente
				regAux = new RegistroNodo();
				regAux2 = new RegistroNodo();
				regAux2 = regs.remove(0);
				if (regs.size() == 0) {
					nuevoHermano.insertarRegistro(regAux2);
					regAux2 = new RegistroNodo();
					regAux2.setClave(getUltimoRegistro().getClave());
					regAux2.setNroBloqueDerecho(nuevoHermano.getNroBloque());
					regAux2.setNroBloqueIzquierdo(getNroBloque());
					nodoPadre.insertarRegistro(regAux2);
					removerRegistro(registros.size() - 1);
				} else {
					regAux = new RegistroNodo();
					regAux.setClave(regAux2.getClave());
					regAux.setNroBloqueDerecho(nuevoHermano.getNroBloque());
					regAux.setNroBloqueIzquierdo(getNroBloque());
					nodoPadre.insertarRegistro(regAux);
				}
				while (regs.size() > 0) {
					RegistroNodo reg = regs.remove(0);
					nuevoHermano.insertarRegistro(reg);
				}
			} else {
				// ahora, tengo que vaciar el hno y llenarlo otra vez
				regs.addAll(getRegistros());
				getRegistros().clear();
				setEspacioOcupado(0);
				
				//ahora lo cargo hasta la minima ocupacion
				setMinIndiceCarga(Constantes.MENOR);
				while ((regs.size() > 0) && (!tieneCargaMinima())) {
					RegistroNodo reg = regs.remove(0);
					insertarRegistro(reg);
				}
				if (regs.size() == 0) {
					RegistroNodo reg = nodoHermano.removerRegistro(nodoHermano.getCantidadRegistros() - 1);
					regs.add(reg);
				}
				// Luego lleno el nodo con lo del hermano siguiente
				while (regs.size() > 0) {
					RegistroNodo reg = regs.remove(0);
					nuevoHermano.insertarRegistro(reg);
				}
				// Ahora tengo que cargar las claves en el padre, y listo!
				pos = nodoPadre.buscarRegistro(getPrimerRegistro().getClave());
				if (pos == Constantes.MENOR) {
					pos = 0;
				}
				//if(pos == Constantes.MAYOR) {
				//	pos = nodoPadre.getCantidadRegistros() - 1; 
				//}
				int espacio = nodoPadre.getEspacioOcupado();
				espacio += diferenciaClaves(nodoPadre.getRegistros().get(
					pos), getPrimerRegistro());
				nodoPadre.setEspacioOcupado(espacio);
				nodoPadre.getRegistros().get(pos).setClave(
						getPrimerRegistro().getClave());
				
				RegistroNodo reg = new RegistroNodo();
	
				reg.setClave(nuevoHermano.getPrimerRegistro().getClave());
				reg.setNroBloqueIzquierdo(getNroBloque());
				reg.setNroBloqueDerecho(nuevoHermano.getNroBloque());
				nodoPadre.insertarRegistro(reg);
				pos = nodoPadre.buscarRegistro(reg.getClave());
				if(pos == Constantes.MAYOR) {
					pos = nodoPadre.getCantidadRegistros() - 1; 
				}
				if (pos < nodoPadre.registros.size() - 1) {
					nodoPadre.getRegistros().get(pos + 1)
					.setNroBloqueIzquierdo(reg.getNroBloqueDerecho());
				}
			}
		}
		this.setOverflow(false);
		return nuevoHermano;
	}
	
	public int diferenciaClaves(RegistroNodo regOld, RegistroNodo regNew) {
		return regNew.getClave().getClave().getBytes().length
				- regOld.getClave().getClave().getBytes().length;
	}
	
	/**
	 * @return si pudo ocupar el nodo, o no le alcanzo el espacio libre
	 */
	public final boolean tieneCargaMinima() {
		Float ocup = new Float(espacioOcupado);
		ocup = ocup / espacioTotal;
		if (minIndiceCarga < 0) {
			if (ocup >= Constantes.FACTOR_CARGA_NODOS) {
				minIndiceCarga = registros.size();			
				return true;
			} else {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * @param espacio
	 *            the espacioOcupado to set
	 */
	public final void ocupar(final int espacio) {
		
		// Si es el primer registro, entonces sumo el puntero a la izquierda.
		if (espacioOcupado == 0){
			espacioOcupado += Constantes.SIZE_OF_INT;
		} else {
			overflow = false;
		}
		
		espacioOcupado += espacio;
		
		if (espacio < 0) {
			espacioOcupado += Constantes.SIZE_OF_INT;
		} else {
			espacioOcupado -= Constantes.SIZE_OF_INT;
		}
		
		if (espacioOcupado > this.espacioTotal) {
			overflow = true;
		} else {
			overflow = false;
		}
	}
	
	/**
	 * Para serializar.
	 * @return bytes[]
	 */
	public final byte[] getBytes() {
		
		ByteArrayOutputStream bos = new ByteArrayOutputStream();  
		DataOutputStream dos = new DataOutputStream(bos);
		
		try {			
			
			//Nro de bloque
			byte[] longDatos = Conversiones.intToArrayByte(nroBloque);
			dos.write(longDatos, 0, longDatos.length);
			//Nro de bloque del padre
			longDatos = Conversiones.intToArrayByte(nroBloquePadre);
			dos.write(longDatos, 0, longDatos.length);
			//Nro de bloque en el secuencialSet
			longDatos = Conversiones.intToArrayByte(punteroBloque);
			dos.write(longDatos, 0, longDatos.length);
			//Espacio ocupado del nodo.
			longDatos = Conversiones.intToArrayByte(espacioOcupado);
			dos.write(longDatos, 0, longDatos.length);
			
			if (espacioOcupado > espacioTotal) {
				LOG.debug("Error !!!!!!!!!!!!!!!!!!!!!!!!!!! Nodo en overflow");
			}
			byte[] regBytes = null;
			int offset = 0;
			//Serializo todos los registros.
			for (RegistroNodo registro : registros) {
				regBytes = registro.getBytes();
				// El primer registro lo grabo completo, despues no grabo el
				// puntero al bloque de la izquierda para no repetir.
				dos.write(regBytes, offset, regBytes.length - offset);
				if (offset == 0) {
					offset += Constantes.SIZE_OF_INT;
				}
			}

		} catch (IOException e) {
			LOG.error("Error: " + e.getMessage(), e);
			e.printStackTrace();
		}
		
		return bos.toByteArray();
	}

	/**
	 * @return .
	 */
	public final boolean hasMoreBytes() {
		// TODO Auto-generated method stub
		return false;
	}

	/**
	 * 
	 * @param buffer el nodo, pero expresado como cadena de bytes.
	 * @throws Exception 
	 * @throws IOException .
	 */
	public final void setBytes(final byte[] buffer) throws IOException {
		
		ByteArrayInputStream bis = new ByteArrayInputStream(buffer);
		DataInputStream dos = new DataInputStream(bis);
		int bloqueAnt = 0, leido = 4, aLeer = 0;
		int cantidad = 0;
		byte[] datos = new byte[Constantes.SIZE_OF_INT];
		RegistroNodo reg = null;
		getRegistros().clear();
		setEspacioOcupado(0);
		setMinIndiceCarga(Constantes.MENOR);
		registros = new ArrayList<RegistroNodo>();
		overflow = false;
		
		try {
			//Leo el numero de bloque.
			nroBloque = dos.readInt();
			nroBloquePadre = dos.readInt();
			punteroBloque = dos.readInt();
			//espacioOcupado = dos.readInt();
			//aLeer = espacioOcupado;
			aLeer = dos.readInt();
			
			// Leo el primer dato del primer registro,que es el numero de bloque
			// izquierdo al que apunta.
			bloqueAnt = dos.readInt();
			while (aLeer > leido) {
				
				//Lea la cantidad de bytes que ocupa el registro.
				cantidad = dos.readInt();
				leido += Constantes.SIZE_OF_INT;
				
				datos = new byte[cantidad];
				leido += dos.read(datos, 0, cantidad);
				reg = new RegistroNodo();
				reg.setBytes(datos, bloqueAnt);
				bloqueAnt = reg.getNroBloqueDerecho();
				if (bloqueAnt < 0) {
					esHoja = true;
				}
				//Los agrego a la lista.
				//registros.add(reg);
				this.insertarRegistro(reg);
			}
			
		} catch (IOException e) {
			LOG.error(e);
			e.printStackTrace();
			throw e;
		}
	}
	
	/**************************
	 * Getters and Setters
	 **************************/
	
	/**
	 * @return the espacioTotal
	 */
	public final int getEspacioTotal() {
		return espacioTotal;
	}

	/**
	 * @return the espacioOcupado
	 */
	public final int getEspacioOcupado() {
		return espacioOcupado;
	}
	
	/**
	 * @return the esHoja
	 */
	public final boolean isHoja() {
		return esHoja;
	}

	/**
	 * @return the registros
	 */
	public final ArrayList<RegistroNodo> getRegistros() {
		return registros;
	}

	/**
	 * @param espacio the espacioTotal to set
	 */
	public final void setEspacioTotal(final int espacio) {
		this.espacioTotal = espacio;
	}

	/**
	 * @param espacio the espacioOcupado to set
	 */
	public final void setEspacioOcupado(final int espacio) {
		this.espacioOcupado = espacio;
	}
	
	/**
	 * @param hoja the esHoja to set
	 */
	public final void setEsHoja(final boolean hoja) {
		this.esHoja = hoja;
	}
	
	/**
	 * @return El primer registro del nodo.
	 */
	public final RegistroNodo getPrimerRegistro() {
		return this.registros.get(0);
	}
	
	/**
	 * @return El ultimo registro del nodo.
	 */
	public final RegistroNodo getUltimoRegistro() {
		return this.registros.get(registros.size() - 1);
	}
	
	/**
	 * @return the nroBloquePadre
	 */
	public final Integer getNroBloquePadre() {
		return nroBloquePadre;
	}

	/**
	 * @param bloque the nroBloquePadre to set
	 */
	public final void setNroBloquePadre(final Integer bloque) {
		this.nroBloquePadre = bloque;
	}

	/**
	 * @return the nroBloque
	 */
	public final Integer getNroBloque() {
		return nroBloque;
	}

	/**
	 * @param bloque the nroBloque to set
	 */
	public final void setNroBloque(final Integer bloque) {
		this.nroBloque = bloque;
	}

	/**
	 * @param hayOverflow the overflow to set
	 */
	public final void setOverflow(final boolean hayOverflow) {
		this.overflow = hayOverflow;
	}

	/**
	 * @return the overflow
	 */
	public final boolean isOverflow() {
		if (espacioOcupado > espacioTotal) {
			return true;
		}
		return false;
	}
	
	/**
	 * @return the minIndiceCarga
	 */
	public final int getMinIndiceCarga() {
		return minIndiceCarga;
	}

	/**
	 * @param minIndice the minIndiceCarga to set
	 */
	public final void setMinIndiceCarga(final int minIndice) {
		this.minIndiceCarga = minIndice;
	}
	
	/**
	 * @param espacioNuevoReg El espacio en bytes del bloque a insertar
	 * @return si entra, true, sino false
	 */
	public final boolean hayEspacio(final int espacioNuevoReg) {
		if ((espacioOcupado + espacioNuevoReg - Constantes.SIZE_OF_INT) <= espacioTotal) {
			return true;
		}
		return false;
	}
	
	/**
	 * Forma rapida para conocer la cantidad de registros.
	 * @return La cantidad de registros.
	 */
	public final int getCantidadRegistros() {
		return registros.size();
	}
	
	/**
	 * Redifinicion del toString.
	 * @return toStringde los registros.
	 */
	public final String toString() {
		return registros.toString();
	}

	/**
	 * @param punteroBloque the punteroBloque to set
	 */
	public void setPunteroBloque(Integer punteroBloque) {
		this.punteroBloque = punteroBloque;
	}

	/**
	 * @return the punteroBloque
	 */
	public Integer getPunteroBloque() {
		return punteroBloque;
	}
}
