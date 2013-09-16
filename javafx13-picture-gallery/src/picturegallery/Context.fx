/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package picturegallery;

/**
 * @author jiyarza
 */
def DEFAULT_NUXEO_URL = "http://localhost:8080/nuxeo/";
def DOWNLOAD_FILE = "/downloadFile?blobFile=content";
def DEFAULT_USERNAME = "jiyarza2";
def DEFAULT_PASSWORD = "jiyarza2";
def DEFAULT_WIDTH = 800;
def DEFAULT_HEIGHT = 600;

public var nuxeoUrl : String = DEFAULT_NUXEO_URL on replace {
    println("url={nuxeoUrl}");
    if (nuxeoUrl == null) {
        nuxeoUrl = DEFAULT_NUXEO_URL
    }
}
public var automationUrl : String = bind "{nuxeoUrl}site/automation";
public var nxpicsUrl : String = bind "{nuxeoUrl}nxpicsfile/default/";
public var restApiUrl : String = bind "{nuxeoUrl}restAPI/default/";

public var username : String = DEFAULT_USERNAME;
public var password : String = DEFAULT_PASSWORD;

public var width : Number = 800 on replace {
    if (width <= 0) {
        width = DEFAULT_WIDTH
    }
}
public var height : Number = 600 on replace {
    if (height <= 0) {
        height = DEFAULT_HEIGHT
    }
}
