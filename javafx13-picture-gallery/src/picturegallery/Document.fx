package picturegallery;

/**
 * @author jiyarza
 */

public class Document {
    public var uid : String;
    public var path : String;
    public var type : String;
    public var state : String;
    public var title : String;
    public var lastModified : String;

    override function toString() : String {
        return "{uid}\n{path}\n{type}\n{state}\n{title}\n{lastModified}\n"
    }
}
