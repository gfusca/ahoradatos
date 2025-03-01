
package ar.com.datos.grupo5.registros;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import ar.com.datos.grupo5.Constantes;
import ar.com.datos.grupo5.interfaces.Registro;
import ar.com.datos.grupo5.utils.Conversiones;

/**
 * Esta clase implementa el registro para el diccionario.
 * 
 * @see ar.com.datos.grupo5.interfaces.Registro
 * @author LedZeppeling
 */
public class RegistroDiccionario implements Registro {
	
	/**
	 * Cuantos bytes puedo pasar.
	 */
	private Long moreBytes;
	
	/**
	 * Offset.
	 */
	private Long offset;
	
	/**
	 * El dato que se guarda.
	 */
	private String dato;
	
	/**
	 * En este caso se devuelve de una vez todos los bytes. Devuelvo true la
	 * primera vez y pongo en false, despues cuando se pregunta nuevamente
	 * devuelvo false, pero pongo en true para que el registro pueda ser usado
	 * denuevo.
	 * 
	 * @return true si hay mas bytes para pedir con getBytes.
	 */
	public final boolean hasMoreBytes() {
		
		if (moreBytes > 0) {
			return true;
		}
		
		return false;
	}
	
	/**
	 * @see ar.com.datos.grupo5.interfaces.Registro#toBytes()
	 * @return los bytes que representan al registro.
	 */
	public final byte[] getBytes() {

		ByteArrayOutputStream bos = new ByteArrayOutputStream();  
		DataOutputStream dos = new DataOutputStream(bos);
		try {
			int longDatosAdic = Constantes.SIZE_OF_INT
					+ Constantes.SIZE_OF_LONG;
			byte[] datosByte = dato.getBytes();
			
			if (moreBytes == (datosByte.length + longDatosAdic)) {
				byte[] longDatoBytes = Conversiones
						.intToArrayByte(datosByte.length);
				byte[] offsetBytes = Conversiones.longToArrayByte(offset);

				dos.write(offsetBytes, 0, offsetBytes.length);
				dos.write(longDatoBytes, 0, longDatoBytes.length);
				moreBytes -= offsetBytes.length;
				moreBytes -= longDatoBytes.length;
			}
			dos.write(datosByte, 0, datosByte.length);
			moreBytes -= datosByte.length;
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return bos.toByteArray();
	}
	
	/**
	 * M�todo que llena el registro con la informaci�n del buffer.
	 */
	public void llenar() {
		// TODO Llenar este m�todo
	}
	
	/**
	 * M�todo que devuelve el offset.
	 * 
	 * @return El offset en el archivo de audio.
	 */
	public final Long getOffset() {
		return offset;
	}

	/**
	 * M�todo para cargar el offset.
	 * 
	 * @param offset
	 *            El offset a cargar.
	 */
	public final void setOffset(final Long offset) {
		this.offset = offset;
	}

	/**
	 * M�todo para devolver el dato.
	 * 
	 * @return El dato.
	 */
	public final String getDato() {
		return dato;
	}

	/**
	 * M�todo para cargar el dato.
	 * 
	 * @param dato
	 *            El dato a setear.
	 */
	public final void setDato(final String dato) {
		this.dato = dato;
		// Ac� considero el tama�o (int) y el offset (long).
		this.moreBytes = (long) dato.getBytes().length + Constantes.SIZE_OF_INT
				+ Constantes.SIZE_OF_LONG;
	}
	 
	/**
	 * @see ar.com.datos.grupo5.interfaces.Registro#setBytes(byte[], Long)
	 * @param buffer
	 *            la tira de bytes.
	 * @param offset
	 *            El offset en el que se encuentra el dato de audio asociado.
	 */
	public final void setBytes(final byte[] buffer, final Long offset) {
		this.setOffset(offset);
		this.setDato(new String(buffer));
	}
}
