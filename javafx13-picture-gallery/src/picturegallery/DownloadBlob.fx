/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package picturegallery;

import javafx.io.http.HttpRequest;
import javafx.io.http.HttpHeader;
import java.io.InputStream;
import javafx.scene.image.Image;

/**
 * @author jiyarza
 */

public class DownloadBlob {

    public var url : String;
    public var username : String;
    public var password : String;
    public var document : Document;
    public var image : Image;

    // Completion callback that also delivers parsed photo metadata
    public var onDone: function(data : Image) = null;

    public function start() : Void {        
        if (username == null or password == null or url == null) {
            return;
        }

        var request : HttpRequest = HttpRequest {
            location: composeLocation();
            method: HttpRequest.GET
            headers: [
                        HttpHeader {
                            name: "Content-Type"
                            value: "application/json+nxrequest";
                        },
                        HttpHeader.basicAuth(username,password),
                        HttpHeader {
                            name: "Accept"
                            value: "application/json+nxentity, */*";
                        }
                     ]
            onResponseHeaders:
                    function(headerNames: String[]) {
                        println("onResponseHeaders - there are {headerNames.size()} response headers:");
                        for (name in headerNames) {
                            println("    {name}: {request.getResponseHeaderValue(name)}");
                        }
                    }            

            onInput: function(is: InputStream) {
                try {
                    image = Image {                        
                        backgroundLoading: true
                        preserveRatio: true
                        smooth: true
                        impl_source: is
                        placeholder: Image {
                            url: "{__DIR__}images/loading.gif"
                        }
                    }
                 } finally {
                    is.close();
                 }
            }
        }
        request.start();
    }

    function composeLocation() : String {
        return "{url}/files/{document.uid}?path=%2Fcontent";
    }
}
