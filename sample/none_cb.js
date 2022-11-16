
var DataSrcImpl = Java.type("org.xresloader.core.data.src.DataSrcImpl")
var gOurInstance = DataSrcImpl.getOurInstance()
var SchemeConf = Java.type("org.xresloader.core.scheme.SchemeConf")
var gSchemeConf = SchemeConf.getInstance()

var initDataSource = function () {
    var methods = gOurInstance.getClass().getMethods()
    for(var key in methods) {
        print("> initDataSource", methods[key].toString())
    }
}

/**
 * 
 * @param {HashMap<String, Object>} curMsg
 * @param {com.google.protobuf.Descriptors.Descriptor} msgDesc
 * @returns boolean
 */
var currentMessageCallback = function (curMsg, msgDesc) {
    print("> ", msgDesc.getFullName(), curMsg)
    return true
}
