/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package picturegallery;

import javafx.scene.CustomNode;
import javafx.scene.image.ImageView;
import java.util.Random;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.Group;
import javafx.animation.transition.FadeTransition;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseEvent;
import javafx.animation.transition.RotateTransition;
import javafx.animation.Timeline;
import javafx.animation.transition.Transition;
import javafx.animation.transition.TranslateTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;

/**
 * This class is an Image container.
 * Provides the photo frame decoration and effects.
 *
 * @author jiyarza
 */
var zoomActive:Integer = 0;
function isZoomActive():Boolean {
    return zoomActive>0;
}


public class ThumbImage extends CustomNode {

    var margin:Integer = 8;
    var width:Integer = 200 + margin;
    var height:Integer = 200 + margin;
    var random = new Random();
    var downloadBlob : DownloadImage;
    var x:Number;
    var y:Number;
    var toX:Number;
    var toY:Number;
    var mouseDragged:Boolean = false;
    var finalRot:Number;
    var hoverRotateTransition : RotateTransition;
    var zoomTimeline : Timeline;
    var shadowEffect : DropShadow;
    var zoomedIn : Boolean;
    
    public var document:Document on replace {
        DownloadBlob {
            document: document
        }.start();
    };

    public var imageView : ImageView = ImageView {
        fitWidth: bind width - margin*2;
        fitHeight: bind height - margin*2;
        preserveRatio: true
        x: margin
        y: margin
        smooth: true
        image: bind downloadBlob.image
    };

    var progress = bind imageView.image.progress on replace {
        if(progress >= 90.0) {                        
            visible = true;
            opacity = 0;
            if (progress == 100) {
                FadeTransition {
                    node: this
                    duration: 1s
                    fromValue: 0.1
                    toValue: 1.0
                }.playFromStart();
            }
        }
    }

    override function create() : Node {
        cursor = Cursor.HAND;
        blocksMouse = true;
        visible = false;
        zoomedIn = false;
        x = translateX = random.nextInt(Main.width - this.width);
        if (x < 1) x = translateX = 1;
        y = translateY = random.nextInt(Main.height - this.height);
        if (y < 1) y = translateY = 1;
        finalRot = rotate = random.nextInt(360)-180 as Float;
//        glowEffect = Glow {
//            level: .5;
//        }
        shadowEffect = DropShadow {
        };
        effect = shadowEffect;
        
        hoverRotateTransition = RotateTransition {
            duration: .6s
            node: this
        };
/*
        downloadBlob = DownloadBlob {
            url: bind Context.url
            username: bind Context.username
            password: bind Context.password
            document: document            
        }
        downloadBlob.start();
*/
        downloadBlob = DownloadImage {
            url: bind Context.nxpicsUrl
            username: bind Context.username
            password: bind Context.password
            document: document
        }
        downloadBlob.start();


        zoomTimeline = Timeline {
        };

        var borderRect = Rectangle {
            width: bind imageView.boundsInParent.width + margin * 2
            height: bind imageView.boundsInParent.height + margin * 2

            strokeWidth: margin
            stroke: Color.WHITE
            smooth: true
        }

//         rotate();

        Group {
            content: [ borderRect, imageView ]
        }
    }

    override var onMouseEntered = function (me:MouseEvent):Void {
        if (isZoomActive()) return;
        if (mouseDragged or zoomTimeline.running or zoomedIn) {
            return;
        }

        toFront();

        hoverRotateTransition.stop();
        hoverRotateTransition.fromAngle = finalRot;
        hoverRotateTransition.toAngle = 0;
        hoverRotateTransition.playFromStart();
    }

    override var onMouseExited = function (me:MouseEvent):Void {
        if (zoomedIn) {
            zoomOut();
            return;
        }
        if(mouseDragged or zoomTimeline.running or
                hoverRotateTransition.running) {
            return;
        }
        hoverRotateTransition.stop();
        hoverRotateTransition.fromAngle = 0;
        hoverRotateTransition.toAngle = finalRot;
        hoverRotateTransition.playFromStart();
    }

    override var onMousePressed = function (me:MouseEvent):Void {        
        mouseDragged = true;
    }

    override var onMouseReleased = function (me:MouseEvent):Void {        
        mouseDragged = false;
        x = translateX;
        y = translateY;
    }

    override var onMouseDragged = function (me : MouseEvent):Void {
        translateX = x + me.dragX;
        translateY = y + me.dragY;
    }

    override var onMouseClicked = function (me : MouseEvent):Void {
        if (zoomedIn) {
            zoomOut();
        } else if (not isZoomActive()) {
            zoomIn();
        }
    }



    public function zoomIn() : Void {
        toX = Main.width / 2 - this.width / 2;
        toY = Main.height / 2 - this.height / 2;

        zoomTimeline.keyFrames = [
            KeyFrame {
                time: 500ms
                values: [ scaleX => 2.5 tween Interpolator.EASEOUT ]
            },
            KeyFrame {
                time: 500ms
                values: [ scaleY => 2.5 tween Interpolator.EASEOUT ]
            },
            KeyFrame {
                time: 1s
                values: [ translateX => toX tween Interpolator.EASEOUT ]
            },
            KeyFrame {
                time: 1s
                values: [ translateY => toY tween Interpolator.EASEOUT ]
            }
        ];
        zoomTimeline.playFromStart();
        zoomedIn = true;
        zoomActive++;
    }

    public function zoomOut() : Void {
        toX = Main.width / 2 - this.width / 2;
        toY = Main.height / 2 - this.height / 2;

        zoomTimeline.keyFrames = [
            KeyFrame {
                time: 500ms
                values: [ scaleX => 1 tween Interpolator.EASEOUT ]
            },
            KeyFrame {
                time: 500ms
                values: [ scaleY => 1 tween Interpolator.EASEOUT ]
            },
            KeyFrame {
                time: 1s
                values: [ translateX => x tween Interpolator.EASEOUT ]
            },
            KeyFrame {
                time: 1s
                values: [ translateY => y tween Interpolator.EASEOUT ]
            }
        ];
        zoomTimeline.playFromStart();
        zoomedIn = false;
        zoomActive--;

        hoverRotateTransition.stop();
        hoverRotateTransition.fromAngle = 0;
        hoverRotateTransition.toAngle = finalRot;
        hoverRotateTransition.playFromStart();
    }



    public function cancel() : Void {
        if(imageView.image != null) {
            imageView.image.cancel();
        }
    }

    override public function toString() : String {
        return "{document}\n"
    }
}
