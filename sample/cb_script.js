var DataSrcImpl = Java.type("org.xresloader.core.data.src.DataSrcImpl");
var dataSrcImplInstance = DataSrcImpl.getOurInstance();
var SchemeConf = Java.type("org.xresloader.core.scheme.SchemeConf");
var schemeConfInstance = SchemeConf.getInstance();

var SqlTimestamp = Java.type("java.sql.Timestamp");

var ProgramOptions = Java.type("org.xresloader.core.ProgramOptions");

var PBType = Java.type("com.google.protobuf.Descriptors.FieldDescriptor.Type");

var ArrayList = Java.type("java.util.ArrayList");
var Integer = Java.type("java.lang.Integer");
parseInt = Integer.valueOf;
var G = {};

var mapSource2ID = {
  "./资源转换示例.xlsx|process_by_script1": 1,
  "./资源转换示例.xlsx|process_by_script2": 2,

  "./资源转换示例-大文件.xlsx|process_by_script1": 1,
  "./资源转换示例-大文件.xlsx|process_by_script2": 2,
  "./资源转换示例-大文件.xlsx|process_by_script3": 3,
  "./资源转换示例-大文件.xlsx|process_by_script4": 4,
  "./资源转换示例-大文件.xlsx|process_by_script5": 5,
  "./资源转换示例-大文件.xlsx|process_by_script6": 6,
  "./资源转换示例-大文件.xlsx|process_by_script7": 7,
  "./资源转换示例-大文件.xlsx|process_by_script8": 8,
};

function initDataSource() {
  G.currentID =
    mapSource2ID[
      dataSrcImplInstance.getCurrentFileName() +
        "|" +
        dataSrcImplInstance.getCurrentTableName()
    ];

  var curFileName = gOurInstance.getCurrentFileName();
  var curSheetName = gOurInstance.getCurrentTableName();
  print("> Check source : " + curFileName + ", " + curSheetName);
}

function extractRealKey(keyWithPrefix) {
  var transIDRegex = /id_([A-Za-z0-9]+)/;
  var arrRegRes = transIDRegex.exec(keyWithPrefix);
  var actualKey = null;
  if (arrRegRes != null && arrRegRes.length > 1) {
    actualKey = arrRegRes[1];
  }
  return actualKey;
}

function toCombineID(prefix, id) {
  var cid;
  if (prefix == null) {
    prefix = G.currentID;
  }
  if (prefix != null && id != null) {
    if (prefix < 100000 && id < 100000) {
      cid = parseInt(prefix * 100000 + (id % 100000));
    }
  }
  return cid;
}

var generateCombineID = function (curMsg, msgDesc) {
  var newKV = {};
  for (var key in curMsg) {
    var fd = msgDesc.findFieldByName(key);
    if (fd == null || fd.getType() != PBType.MESSAGE) {
      continue;
    }
    var fieldMessageType = fd.getMessageType();
    var value = curMsg[key];
    var actualKey = extractRealKey(key);

    if (fd.isMapField()) {
      var mapKVDesc = fd.getMessageType();
      var mapValueDesc = mapKVDesc.findFieldByName("value");
      if (mapValueDesc.getType() == PBType.MESSAGE) {
        if (fieldMessageType.getName() == "combine_id") {
          newKV[actualKey] = {};
          for (var subKey in value) {
            newKV[actualKey][subKey] = toCombineID(
              value[subKey].prefix,
              value[subKey].id
            );
          }
        } else {
          generateCombineID(curMsg[key], fieldMessageType);
        }
      }
    } else if (fd.isRepeated()) {
      if (fieldMessageType.getName() == "combine_id") {
        newKV[actualKey] = new ArrayList();
        for (var subKey in value) {
          newKV[actualKey].add(
            subKey,
            toCombineID(value[subKey].prefix, value[subKey].id)
          );
        }
      } else {
        for (var subKey in value) {
          generateCombineID(curMsg[key], fieldMessageType);
        }
      }
    } else {
      if (fieldMessageType.getName() == "combine_id") {
        var id_CombineID = curMsg[key];
        newKV[actualKey] = toCombineID(id_CombineID.prefix, id_CombineID.id);
      }
    }
  }
  for (var key in newKV) {
    if (newKV[key] != null) {
      curMsg[key] = newKV[key];
    }
  }
};

/**
 *
 * @param {HashMap<String, Object>} curMsg
 * @param {org.xresloader.core.data.dst.DataDstWriterNode.DataDstTypeDescriptor} typeDesc
 * @returns boolean
 */
function currentMessageCallback(curMsg, typeDesc) {
  generateCombineID(curMsg, typeDesc.getRawDescriptor());
  if (curMsg.human_readable_date != null) {
    var stamp = SqlTimestamp.valueOf(curMsg.human_readable_date);
    curMsg.date = {
      seconds: parseInt(stamp.getTime() / 1000),
      nanos: stamp.getNanos(),
    };
  }
  return true;
}
