/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package picturegallery;

import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.Group;
import javafx.stage.StageStyle;

/**
 * @author jiyarza
 */
public var width : Number = bind scene.width;
public var height : Number = bind scene.height;

def query = Query {
    url: bind Context.automationUrl
    username: bind Context.username
    password: bind Context.password
    nxql: Query.GET_ALL_USER_IMAGES
    onDone: updateImages
}

var thumbGroup = Group { };

function updateImages(docs : Document[]) {
    println("updateImages");
    delete thumbGroup.content;
    if(docs == null) {
        return;
    }
    for(doc in docs) {
        insert ThumbImage {
            document: doc;
        } into thumbGroup.content;
    }
}

/*
var params = Text {
    translateY: 50
    translateX: 400
    content: bind "{FX.getArgument("url") as String}\n{Context.username}\n{Context.width}\n{Context.height}"
}
*/

var scene = Scene {
        width: Context.width
        height: Context.height
        //fill: Color.BLACK
        content: [ thumbGroup ]
}

var stage = Stage {
    title: "Picture Gallery"
    style: StageStyle.TRANSPARENT
    scene: scene
}

function run() {
    Context.nuxeoUrl = FX.getArgument("url") as String;
    Context.width = Number.valueOf(FX.getArgument("width") as String);
    Context.height = Number.valueOf(FX.getArgument("height") as String);
    query.start();
}
