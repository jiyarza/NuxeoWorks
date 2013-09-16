package com.opensistemas.nxdroid.logic;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.SoapFault;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.AndroidHttpTransport;
import android.util.Log;


public class LoginManager {

	private static final String WS_CONNECT_WITH_MAC = "connectWithMac";
	private static final String WS_DISCONNECT = "disconnect";
	private static final String NAMESPACE = "http://ws.multimedia.doma.orange.org/";
	//private static final String URL = "http://doma.ilabspain.com/nuxeo/webservices/domasync?wsdl";
	//private static final String URL = "http://192.168.1.128:8080/nuxeo/webservices/domasync?wsdl";
	
	private String sessionID;

	private static LoginManager instance;
	
	/**
	 * constructor privado
	 */
	private LoginManager(){
	}

	/**
	 * miembro publico para crear una instancia unica
	 * @return
	 */
	public static LoginManager getInstance(){
		if(instance == null)
			instance = new LoginManager();
		return instance;
	}

	/**
	 * Method to login with the server
	 * @return true if the login is correct
	 */
	public boolean login() {
		String username = DoMaService.getDoMaInstance().getPreference(DoMa.PREF_KEY_USERNAME);
		String password = DoMaService.getDoMaInstance().getPreference(DoMa.PREF_KEY_PASSWORD);
		String mac = DoMaService.getDoMaInstance().getPreference(DoMa.PREF_KEY_MAC);		
		String URL = DoMaService.getDoMaInstance().getPreference(DoMa.PREF_KEY_URL).concat("/nuxeo/webservices/domasync?wsdl");		
		boolean login = false;
		//validate MAC Address
		if(!mac.equals("0")){
			mac = validateMAC(mac);
			Log.d("LoginManager.login()", username + ", " + password + ", " + mac + "URL: " + URL);
			//CALL the web service method
			SoapObject request = new SoapObject(NAMESPACE, WS_CONNECT_WITH_MAC);
			request.addProperty("username", username);
			request.addProperty("password", password);
			request.addProperty("mac", mac);
			SoapObject resultSoap = connect(request, NAMESPACE.concat(WS_CONNECT_WITH_MAC));
			if(resultSoap != null){
				Object result = resultSoap.getProperty("return");
				if(result != null){
					sessionID = result.toString();
					login = true;
				}
			}
		}
		Log.d("LoginManager.login()", login ? "SUCCESS - SessionId: " + sessionID : "FAILED");
		return login;
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
		mac = strBuff.toString();
		return mac;
	}

	/**
	 * Method to logout with the server
	 */
	public void logout(){
		//CALL the web service method
		SoapObject request = new SoapObject(NAMESPACE, WS_DISCONNECT);
		request.addProperty("sessionId", sessionID);
		connect(request, NAMESPACE.concat(WS_DISCONNECT));		
		sessionID = null;
	}

	private SoapObject connect(SoapObject request, String soapAction){
		try {
			String URL = DoMaService.getDoMaInstance().getPreference(DoMa.PREF_KEY_URL).concat("/nuxeo/webservices/domasync?wsdl");
			SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
			envelope.setOutputSoapObject(request);			
			AndroidHttpTransport androidHttpTransport = new AndroidHttpTransport (URL); 
			androidHttpTransport.call(soapAction, envelope);
			
			if (envelope.bodyIn instanceof SoapObject) {
				SoapObject result = (SoapObject) envelope.bodyIn;
				Log.d("LoginManager.connect", result.toString());
				return result;
			} else {
				SoapFault fault = (SoapFault) envelope.bodyIn;
				Log.e("LoginManager.connect", "SoapFault", fault);
			}

		} catch(Exception e) {
			Log.e("LoginManager.Exception", "ERROR: ", e);
		}
		return null;
	}

	public String getSessionID() {
		return sessionID;
	}
	
}
