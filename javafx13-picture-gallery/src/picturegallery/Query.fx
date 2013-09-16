/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package picturegallery;
import javafx.io.http.HttpHeader;
import javafx.io.http.HttpRequest;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Implements a /Document.Query automation client
 * @author jiyarza
 */
public def GET_ALL_USER_IMAGES = "SELECT * FROM Document WHERE ecm:primaryType=\"OrangeImage\" OR ecm:primaryType=\"Picture\" AND ecm:currentLifeCycleState<>\"deleted\"";

public class Query {

    public var url : String;
    public var username : String;
    public var password : String;
    public var nxql : String;
    // Completion callback that also delivers parsed photo metadata
    public var onDone: function(data : Document[]) = null;

    public function start() : Void {
        if (username == null or password == null or url == null or nxql == null) {
            return;
        }

        var request : HttpRequest = HttpRequest {
            location: "{url}/Document.Query"
            method: HttpRequest.POST
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
            onOutput: function(os: java.io.OutputStream) {
                try {
                    os.write("\{params:\{query:'{nxql}'\}\}".getBytes());
                } catch (ex: IOException) {
                    ex.printStackTrace();
                } finally {
                    os.close();
                }
            }

            onInput: function(is: InputStream) {
                try {
                    var parser = DocumentPullParser {
                        onDone: onDone;
                    };
                    parser.parse(is);

                    if(parser.errorMessage.length() > 0) {
                        println("Error - {parser.errorMessage}");                        
                    }

                 } finally {
                    is.close();
                 }
            }
        }
        request.start();
    }



}
