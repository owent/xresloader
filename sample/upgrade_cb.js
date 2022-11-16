var Integer = Java.type("java.lang.Integer")

var G = {
    curIDSeq: 0
}
var tbSrcIDMap = {
    "资源转换示例.xlsx|upgrade_10001": 10000,
    "资源转换示例.xlsx|upgrade_10002": 20000
}

var initDataSource = function() {
    var curFileName = gOurInstance.getCurrentFileName()
    var curSheetName = gOurInstance.getCurrentTableName()
    var pureFileRex = /.*\/(.*\.[xlsx]*)/
    var rexResult = pureFileRex.exec(curFileName)
    if (rexResult != null && rexResult.length > 1) {
        curFileName = rexResult[1]
    }

    var szKey = curFileName+"|"+curSheetName
    print("> Check source id: ", szKey, tbSrcIDMap[szKey])
    G.curSrcID = tbSrcIDMap[szKey]
    G.curIDSeq = 0
}

/**
 * 
 * @param {HashMap<String, Object>} curMsg
 * @param {com.google.protobuf.Descriptors.Descriptor} msgDesc
 * @returns boolean
 */
var currentMessageCallback = function(curMsg, msgDesc) {
    G.curIDSeq = G.curIDSeq + 1
    if (G.curSrcID != null) {
        curMsg.SrcID = Integer.valueOf(G.curSrcID + G.curIDSeq)
    }
    print("> ", msgDesc.getFullName(), curMsg)
    return true
}