package com.opensistemas.nxdroid.logic.ws;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.SoapFault;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.AndroidHttpTransport;

import com.opensistemas.nxdroid.logic.DoMa;
import com.opensistemas.nxdroid.logic.DoMaService;

import android.util.Log;


public class SoapManager {
	
	public static final String WS_CONNECT_WITH_MAC = "connectWithMac";
	public static final String WS_DISCONNECT = "disconnect";	
	/**
	 * to get a document from the server TODO this method should not fetch
	 * deleted documents
	 */
	public static final String WS_GET_DOCUMENT = "getDocument";
	/** to search synchronizable documents from the server */
	public static final String WS_SEARCH = "search";
	/** to get synchronizable documents from the server */
	public static final String WS_GET_SYNCHRONIZED_DOCUMENTS = "getSynchronizedDocuments";
	/** to create a document in the server */
	public static final String WS_CREATE_DOCUMENT = "createDocument";
	/** to get a document from its path */
	public static final String WS_GET_DOCUMENT_FROM_PATH = "getDocumentFromPath";
	/** to delete a document from the server */
	public static final String WS_DELETE = "delete";
	/** to synchronize a document with the server */
	public static final String WS_SYNC = "sync";
	/** to publish documents on facebook or on twitter */
	public static final String WS_PUBLISH_DOCUMENT = "publishDocument";
	/** to get the document properties object */
	public static final String WS_GET_DOCUMENT_PROPERTIES = "getDocumentProperties";
	/** Web service NameSpace */
	public static final String NAMESPACE = "http://ws.multimedia.doma.orange.org/";	
	
	/** singleton instance */
	private static SoapManager instance;

	private String sessionId = null;
	
	/**
	 * singleton's private constructor
	 */
	private SoapManager() {
	}

	/**
	 * creates and returns the unique instance
	 * 
	 * @return
	 */
	public static SoapManager getInstance() {
		if (instance == null)
			instance = new SoapManager();
		return instance;
	}
	
	public synchronized SoapObject execute(SoapObject request, String soapAction) {
		//String sessionId = null;
		SoapObject result = null;
		try {
			if (sessionId == null) login();
			result = execute(sessionId, request, soapAction);
		} finally {
			//logout(sessionId);
		}
		return result;
	}
	
	/**
	 * This method encapsulates all the SOAP calls to the DoMa web service
	 * 
	 * @param request
	 * @param soapAction
	 * @return
	 * TODO LoginManager.connect is the same method. Could as well login/logout inside the execute 
	 */
	private synchronized SoapObject execute(String sessionId, SoapObject request, String soapAction) {
		try {
			String URL = DoMaService.getDoMaInstance().getPreference(DoMa.PREF_KEY_URL).concat("/nuxeo/webservices/domasync?wsdl");
			request.addProperty("sessionId", sessionId);			
			SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
			envelope.setOutputSoapObject(request);
			AndroidHttpTransport androidHttpTransport = new AndroidHttpTransport(URL);
			Log.d("SoapManager.execute", request.toString());
			androidHttpTransport.call("\"".concat(soapAction).concat("\""), envelope);			
			if (envelope.bodyIn instanceof SoapFault) {
				SoapFault fault = (SoapFault) envelope.bodyIn;
				Log.w("SoapManager.execute", fault.faultstring);
			} else {
				SoapObject result = (SoapObject) envelope.bodyIn;
				if (result != null) {
					Log.d("SoapManager.execute", result.toString());
				} else {
					Log.w("SoapManager.execute", "SoapObject result: NULL.");
				}
				return result;
			}
		} catch (Exception e) {
			Log.e("SoapManager.execute", "ERROR: ", e);
		}
		return null;
	}

	
	
	public synchronized String login() {
		if (sessionId != null) {
			Log.w("SoapManager.login","WARNING: A session is active: " + sessionId + " (performing logout first).");
			logout();
		}
		sessionId = null;
		
		try {			
			String username = DoMaService.getDoMaInstance().getPreference(DoMa.PREF_KEY_USERNAME);
			String password = DoMaService.getDoMaInstance().getPreference(DoMa.PREF_KEY_PASSWORD);
			String mac = DoMaService.getDoMaInstance().getPreference(DoMa.PREF_KEY_MAC);		
			String URL = DoMaService.getDoMaInstance().getPreference(DoMa.PREF_KEY_URL).concat("/nuxeo/webservices/domasync?wsdl");				
			
			mac = validateMAC(mac);
			Log.d("SoapManager.login()", username + ", " + password + ", " + mac + "URL: " + URL);
			SoapObject request = new SoapObject(NAMESPACE, WS_CONNECT_WITH_MAC);
			request.addProperty("username", username);
			request.addProperty("password", password);
			request.addProperty("mac", mac);
	
			SoapObject result = null;			
			SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
			envelope.setOutputSoapObject(request);
			Log.d("SoapManager.execute", envelope.bodyOut.toString());
			
			AndroidHttpTransport androidHttpTransport = new AndroidHttpTransport (URL);
			androidHttpTransport.call("\"".concat(NAMESPACE).concat(WS_CONNECT_WITH_MAC).concat("\""), envelope);
			
			if (envelope.bodyIn instanceof SoapObject) {
				result = (SoapObject) envelope.bodyIn;
				Log.d("SoapManager.connect", result.toString());
			} else {
				SoapFault fault = (SoapFault) envelope.bodyIn;
				Log.e("SoapManager.connect", "SoapFault", fault);
			}
			
			if(result != null){
				Object resultReturn = result.getProperty("return");
				if(resultReturn != null){
					sessionId = resultReturn.toString();
				}
			}
		} catch(Exception e) {
			Log.e("SoapManager.login", "ERROR: ", e);
		}			

		return sessionId;		
	}
	
	public synchronized void logout() {
		if (sessionId != null) {
			SoapObject request = new SoapObject(NAMESPACE, WS_DISCONNECT);		
			execute(sessionId, request, NAMESPACE.concat(WS_DISCONNECT));
			sessionId = null;
		}
	}
	
	
	
	private String validateMAC(String mac) {
		//get only digits from MAC Address
		StringBuffer strBuff = new StringBuffer();
		char c;
		for (int i = 0; i < mac.length() ; i++) {
			c = mac.charAt(i);
			if (Character.isDigit(c) || Character.isLetter(c)) {
				strBuff.append(c);
			}
		}
		return strBuff.toString();
	}	
	
	
}
