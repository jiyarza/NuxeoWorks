/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package picturegallery;
import javafx.data.pull.PullParser;
import java.io.InputStream;

/**
 * @author jiyarza
 */

public class DocumentPullParser {
    // Error Message (if any)
    public var errorMessage = "";

    // Information about all interesting photos
    var documents: Document[];

    // Completion callback that also delivers parsed photo metadata
    public var onDone: function(data : Document[]) = null;

    public function parse(is: InputStream) {
        try {
           println("onInput - bytes of content available: {is.available()}");

           var parser = PullParser {
               input: is
               documentType: PullParser.JSON
           }

           while (parser.event.type != PullParser.END_DOCUMENT) {
               parser.seek("entity-type");
               if (parser.event.type == PullParser.START_VALUE) {
                   parser.forward();
                   var doc = Document{}
                   parseDocument(doc, parser);
                   //doc.blob = "{url}/files/{doc.uid}?path=%2Fcontent";
                   println(doc.toString());
                   insert doc into documents;
               }
           }
           if (onDone != null) { onDone(documents); }
        } finally {
            is.close();
        }
    }


    function parseDocument(doc:Document, parser:PullParser) : Void {
        while (parser.event.type != PullParser.END_DOCUMENT) {
            parser.forward();
            if (parser.event.type == PullParser.END_ARRAY_ELEMENT) {
                break;
            } else if (parser.event.type == PullParser.TEXT) {
                setDocumentData(doc, parser.event.name, parser.event.text);
            }
        }
    }

    function setDocumentData(doc: Document, name: String, value: String) {
        if (name.equals("uid")) {
            doc.uid = value;
        } else if (name.equals("path")) {
            doc.path = value;
        } else if (name.equals("type")) {
            doc.type = value;
        } else if (name.equals("state")) {
            doc.state = value;
        } else if (name.equals("title")) {
            doc.title = value;
        } else if (name.equals("lastModified")) {
            doc.lastModified = value;
        }
    }
}
