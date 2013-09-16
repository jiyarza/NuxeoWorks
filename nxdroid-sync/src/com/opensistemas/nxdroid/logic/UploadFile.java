package com.opensistemas.nxdroid.logic;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import com.opensistemas.nxdroid.logic.ws.Base64;

import android.util.Log;

public class UploadFile {
	
	private static final int BUFF_SIZE = 8 * 1024;

	// A local test
	public static void main(String args[]) {
		final String REMOTE_ID = "e3d4738a-5fa1-4232-99b8-5286204bad67";
		final String USERPASS = "res:res";
		final String URL = "http://doma.ilabspain.com/";
		final String LOCAL_DOC = "doc/doc.pdf";
		final String REMOTE_FILENAME = "video.flv";

		File file = new File(LOCAL_DOC);
		if (file.exists()) {
			uploadFile(URL, file, REMOTE_ID, REMOTE_FILENAME, USERPASS);
		} else {
			System.out.println("File does not exist.");
		}
	}

	/**
	 * Uploads a file attached to an existing document in the DoMa server.
	 * 
	 * @param server
	 *            Server URL
	 * @param file
	 *            A local, existing, file
	 * @param remoteId
	 *            The remote document ID (Nuxeo ID String)
	 * @param remoteFilename
	 *            The remote filename. If the remote document has a file already
	 *            attached to it, this name must be the same (except when the
	 *            document has the "Files" schema, in which case it is not
	 *            necessary).
	 * @param userpass
	 *            A string with the format user:pass for http basic
	 *            authentication.
	 * @return
	 */
	public static String uploadFile(String server, File file, String remoteId,
			String remoteFilename, String userpass) {
		try {
			String uploadRestlet = server + "nuxeo/restAPI/default/" + remoteId
					+ "/" + encode(remoteFilename) + "/uploadBlob";
			//Log.d("UploadFile.uploadFile", "URL: " + uploadRestlet);

			if (restletCall(uploadRestlet, file, userpass))
				return "OK";
			else
				return "ERROR";

		} catch (Exception e) {
			Log.e("UploadFile.uploadFile()", "Error uploading file: " + e);
			return "ERROR";
		}
	}

	/**
	 * Performs a RESTLET request.
	 * 
	 * 
	 * @param restlet
	 *            Complete restlet URL
	 * @param file
	 * 
	 * @param userpass
	 * 
	 * @return true if the communication has been successful, false otherwise.
	 * 
	 */
	private static boolean restletCall(String restlet, File file,
			String userpass) {
		boolean result = false;
		DataOutputStream out = null;
		InputStream in = null;
		try {
			Log.d("UploadFile.restletCall", restlet);
			URL urlRestlet = new URL(restlet);
			HttpURLConnection connection = (HttpURLConnection) urlRestlet
					.openConnection();
			connection.setDoInput(true);
			connection.setDoOutput(true);
			connection.setRequestMethod("POST");
			String encoding = Base64.encodeBytes(userpass.getBytes());
			connection.setRequestProperty("Authorization", "Basic: " + encoding);

			String lineEnd = "\r\n";
			String twoHyphens = "--";
			String boundary = "*****";
			connection.setRequestProperty("Connection", "Keep-Alive");
			connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
			connection.setRequestProperty("Content-Transfer-Encoding", "binary");
			connection.setChunkedStreamingMode(BUFF_SIZE);			
			out = new DataOutputStream(new BufferedOutputStream(connection
					.getOutputStream(), BUFF_SIZE));

			if(file.getName().endsWith(".mp3")){
				out.writeBytes(twoHyphens + boundary + lineEnd);
				out.writeBytes("Content-Disposition: form-data; name=\"file\";filename=\""
							+ Base64.encodeBytes(file.getName().getBytes()) + "\"" + lineEnd);
				out.writeBytes(lineEnd);
			}

			Log.d("UploadFile.restletCall", "Uploading: " + file.getAbsolutePath());

			FileInputStream fileInput = new FileInputStream(file);
			copy(new FlushedInputStream(fileInput), out);
			
			if(file.getName().endsWith(".mp3")){
				out.writeBytes(lineEnd);
				out.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
			}

			Log.d("UploadFile.restletCall", "connection.getResponseMessage: " + connection.getResponseMessage());

			try {
				out.flush();
				fileInput.close();
			} catch (IOException e) {
				Log.w("UploadFile.restletCall", e.getMessage());
			}
			in = connection.getInputStream();
			result = (in != null);
			System.out.print("UploadFile.restletCall->");
			copy(new FlushedInputStream(in), System.out);
		} catch (Exception e) {
			Log.e("UploadFile.restletCall()", null, e);
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (Exception e) {
				}
			}			
			if (in != null) {
				try {
					in.close();
				} catch (Exception e) {
				}				
			}
		}
		return result;
	}

	private static String encode(String string) {
		String result = "";
		char[] stringArray;
		stringArray = string.toCharArray();
		for (int i = 0; i < stringArray.length; i++) {
			if (stringArray[i] == ' ')
				result += "%20";
			else if (stringArray[i] == '(')
				result += "%28";
			else if (stringArray[i] == ')')
				result += "%29";			
			else if (stringArray[i] == '\r')
				result += "%0D";
			else if (stringArray[i] == '\n')
				result += "%0A";
			else if (stringArray[i] == '\t')
				result += "%09";
			else if (stringArray[i] == '¡')
				result += "%C2%A1";
			else if (stringArray[i] == '¢')
				result += "%C2%A2";
			else if (stringArray[i] == '£')
				result += "%C2%A3";
			else if (stringArray[i] == '¤')
				result += "%C2%A4";
			else if (stringArray[i] == '¥')
				result += "%C2%A5";
			else if (stringArray[i] == '¦')
				result += "%C2%A6";
			else if (stringArray[i] == '§')
				result += "%C2%A7";
			else if (stringArray[i] == '¨')
				result += "%C2%A8";
			else if (stringArray[i] == '©')
				result += "%C2%A9";
			else if (stringArray[i] == 'ª')
				result += "%C2%AA";
			else if (stringArray[i] == '«')
				result += "%C2%AB";
			else if (stringArray[i] == '¬')
				result += "%C2%AC";
			else if (stringArray[i] == '®')
				result += "%C2%AE";
			else if (stringArray[i] == '¯')
				result += "%C2%AF";
			else if (stringArray[i] == '°')
				result += "%C2%B0";
			else if (stringArray[i] == '±')
				result += "%C2%B1";
			else if (stringArray[i] == '²')
				result += "%C2%B2";
			else if (stringArray[i] == '³')
				result += "%C2%B3";
			else if (stringArray[i] == '´')
				result += "%C2%B4";
			else if (stringArray[i] == 'µ')
				result += "%C2%B5";
			else if (stringArray[i] == '¶')
				result += "%C2%B6";
			else if (stringArray[i] == '·')
				result += "%C2%B7";
			else if (stringArray[i] == '¸')
				result += "%C2%B8";
			else if (stringArray[i] == '¹')
				result += "%C2%B9";
			else if (stringArray[i] == 'º')
				result += "%C2%BA";
			else if (stringArray[i] == '»')
				result += "%C2%BB";
			else if (stringArray[i] == '¼')
				result += "%C2%BC";
			else if (stringArray[i] == '½')
				result += "%C2%BD";
			else if (stringArray[i] == '¾')
				result += "%C2%BE";
			else if (stringArray[i] == '¿')
				result += "%C2%BF";
			else if (stringArray[i] == 'À')
				result += "%C3%80";
			else if (stringArray[i] == 'Á')
				result += "%C3%81";
			else if (stringArray[i] == 'Â')
				result += "%C3%82";
			else if (stringArray[i] == 'Ã')
				result += "%C3%83";
			else if (stringArray[i] == 'Ä')
				result += "%C3%84";
			else if (stringArray[i] == 'Å')
				result += "%C3%85";
			else if (stringArray[i] == 'Æ')
				result += "%C3%86";
			else if (stringArray[i] == 'Ç')
				result += "%C3%87";
			else if (stringArray[i] == 'È')
				result += "%C3%88";
			else if (stringArray[i] == 'É')
				result += "%C3%89";
			else if (stringArray[i] == 'Ê')
				result += "%C3%8A";
			else if (stringArray[i] == 'Ë')
				result += "%C3%8B";
			else if (stringArray[i] == 'Ì')
				result += "%C3%8C";
			else if (stringArray[i] == 'Í')
				result += "%C3%8D";
			else if (stringArray[i] == 'Î')
				result += "%C3%8E";
			else if (stringArray[i] == 'Ï')
				result += "%C3%8F";
			else if (stringArray[i] == 'Ð')
				result += "%C3%90";
			else if (stringArray[i] == 'Ñ')
				result += "%C3%91";
			else if (stringArray[i] == 'Ò')
				result += "%C3%92";
			else if (stringArray[i] == 'Ó')
				result += "%C3%93";
			else if (stringArray[i] == 'Ô')
				result += "%C3%94";
			else if (stringArray[i] == 'Õ')
				result += "%C3%95";
			else if (stringArray[i] == 'Ö')
				result += "%C3%96";
			else if (stringArray[i] == '×')
				result += "%C3%97";
			else if (stringArray[i] == 'Ø')
				result += "%C3%98";
			else if (stringArray[i] == 'Ù')
				result += "%C3%99";
			else if (stringArray[i] == 'Ú')
				result += "%C3%9A";
			else if (stringArray[i] == 'Û')
				result += "%C3%9B";
			else if (stringArray[i] == 'Ü')
				result += "%C3%9C";
			else if (stringArray[i] == 'Ý')
				result += "%C3%9D";
			else if (stringArray[i] == 'Þ')
				result += "%C3%9E";
			else if (stringArray[i] == 'ß')
				result += "%C3%9F";
			else if (stringArray[i] == 'à')
				result += "%C3%A0";
			else if (stringArray[i] == 'á')
				result += "%C3%A1";
			else if (stringArray[i] == 'â')
				result += "%C3%A2";
			else if (stringArray[i] == 'ã')
				result += "%C3%A3";
			else if (stringArray[i] == 'ä')
				result += "%C3%A4";
			else if (stringArray[i] == 'å')
				result += "%C3%A5";
			else if (stringArray[i] == 'æ')
				result += "%C3%A6";
			else if (stringArray[i] == 'ç')
				result += "%C3%A7";
			else if (stringArray[i] == 'è')
				result += "%C3%A8";
			else if (stringArray[i] == 'é')
				result += "%C3%A9";
			else if (stringArray[i] == 'ê')
				result += "%C3%AA";
			else if (stringArray[i] == 'ë')
				result += "%C3%AB";
			else if (stringArray[i] == 'ì')
				result += "%C3%AC";
			else if (stringArray[i] == 'í')
				result += "%C3%AD";
			else if (stringArray[i] == 'î')
				result += "%C3%AE";
			else if (stringArray[i] == 'ï')
				result += "%C3%AF";
			else if (stringArray[i] == 'ð')
				result += "%C3%B0";
			else if (stringArray[i] == 'ñ')
				result += "%C3%B1";
			else if (stringArray[i] == 'ò')
				result += "%C3%B2";
			else if (stringArray[i] == 'ó')
				result += "%C3%B3";
			else if (stringArray[i] == 'ô')
				result += "%C3%B4";
			else if (stringArray[i] == 'õ')
				result += "%C3%B5";
			else if (stringArray[i] == 'ö')
				result += "%C3%B6";
			else if (stringArray[i] == '÷')
				result += "%C3%B7";
			else if (stringArray[i] == 'ø')
				result += "%C3%B8";
			else if (stringArray[i] == 'ù')
				result += "%C3%B9";
			else if (stringArray[i] == 'ú')
				result += "%C3%BA";
			else if (stringArray[i] == 'û')
				result += "%C3%BB";
			else if (stringArray[i] == 'ü')
				result += "%C3%BC";
			else if (stringArray[i] == 'ý')
				result += "%C3%BD";
			else if (stringArray[i] == 'þ')
				result += "%C3%BE";
			else if (stringArray[i] == 'ÿ')
				result += "%C3%BF";
			else
				result += stringArray[i];
		}
		return result;
	}

	private static void copy(InputStream in, OutputStream out)
			throws IOException {
		byte[] buffer = new byte[BUFF_SIZE];
		int read;
		while ((read = in.read(buffer)) != -1) {
			out.write(buffer, 0, read);
			out.flush();
		}
	}
}
