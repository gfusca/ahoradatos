package ar.com.datos.tests;

import java.io.IOException;

import org.apache.log4j.Logger;

import ar.com.datos.grupo5.compresion.lzp.Lzp;
import ar.com.datos.grupo5.excepciones.SessionException;
import ar.com.datos.grupo5.interfaces.Compresor;

public class TestLZP {

	private static Logger LOG = Logger.getLogger(TestBinario.class);
	
	public static void main(String[] args) throws IOException, SessionException {
		
		Compresor comp = new Lzp();
		String cadena = "ABCCBABC,CBCCBCCABA";
		//String cadena = "AAAAAA,AAAAAA";
		String[] cadena2 = cadena.split(",");
		String result = "";
		
		comp.iniciarSesion();
		for (int i = 0; i < cadena2.length; i++) {
			result += comp.comprimir(cadena2[i]);
			comp.imprimirHashMap();
		}
		
		LOG.info(cadena.replace(",", ""));
		LOG.info("Longitud en bytes antes de comprimir: " + cadena.replace(",", "").length() * 2);
		result += comp.finalizarSession();
		LOG.info(result);
		LOG.info("Longitud despues de comprimi: " + (result.length() / 8));

	}
}
