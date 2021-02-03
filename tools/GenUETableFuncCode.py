#!/usr/bin/env python3
# -*- encoding: utf-8 -*-

import os
import sys
import json

# 代码模板，包括头文件、CPP、声明、定义等
BlueprintFunctionLibraryName = 'TableBPFuncLib'
HeaderFileTemp = '''
#pragma once

#include "CoreMinimal.h"
#include "Kismet/BlueprintFunctionLibrary.h"
/*INCLUDE_BEGIN*/
{IncludeCode}
/*INCLUDE_END*/
#include "{BpFuncLibName}.generated.h"

/**
 * 
 */
UCLASS()
class HXNEXT_API U{BpFuncLibName} : public UBlueprintFunctionLibrary
{{
	GENERATED_BODY()
	
public:
    /*DECLARATION_BEGIN*/
    {DeclarationCode}
    /*DECLARATION_END*/
}};
'''

CppFileTemp = '''
#include "{BpFuncLibName}.h"

/*DIFINITION_BEGIN*/
{DifinitionCode}
/*DIFINITION_END*/
'''

HeaderTemp = '#include "TableData/{StructName}.h"'
DeclarationTemp = '''
	UFUNCTION(BlueprintCallable, Category = "XResConfig")
	static {HelperName}* Get{TableName}Table();
'''
DifinitionTemp = '''
{HelperName}* UTableBPFuncLib::Get{TableName}Table()
{{
	UClass* clazz = {HelperName}::StaticClass();
	if (nullptr == clazz)
	{{
		return nullptr;
	}}

	return clazz->GetDefaultObject<{HelperName}>();
}}
'''

# 各种路径
UnreaImportSettingsPath = 'output/UnreaImportSettings.json'
OutputPath = 'output/TableHelperCode/'
HeaderFileName = '{BpFuncLibName}.h'
CppFileName = '{BpFuncLibName}.cpp'

# 从UnreaImportSettings.json解析出生成的UE结构名
def ParseStructFromJson(jsonPath):
    with open(jsonPath, 'r') as f:
        data = json.load(f)
    
    structLst = []
    if 'ImportGroups' in data:
        cfgLst = data['ImportGroups']
        for cfg in cfgLst:
            if 'ImportSettings' in cfg:
                settings = cfg['ImportSettings']
                if 'ImportRowStruct' in settings:
                    structLst.append(settings['ImportRowStruct'])
    
    return structLst

# 生成对应头文件和CPP文件
def GenerateCode(structLst):
    includeCode = ''
    declarationCode = ''
    difinitionCode = ''

    for structName in structLst:
        helperName = f'U{structName}Helper'
        tableName = structName[len('Hx2proto'):]

        if len(includeCode) != 0:
            includeCode += '\n'
        includeCode += HeaderTemp.format(StructName = structName)

        if len(declarationCode) != 0:
            declarationCode += '\n'
        declarationCode += DeclarationTemp.format(HelperName = helperName, TableName = tableName)

        if len(difinitionCode) != 0:
            difinitionCode += '\n'
        difinitionCode += DifinitionTemp.format(HelperName = helperName, TableName = tableName)

    bpFuncLibName = BlueprintFunctionLibraryName
    headerCode = HeaderFileTemp.format(BpFuncLibName = bpFuncLibName, IncludeCode = includeCode, DeclarationCode = declarationCode)
    cppCode = CppFileTemp.format(BpFuncLibName = bpFuncLibName, DifinitionCode = difinitionCode)

    with open(OutputPath + HeaderFileName.format(BpFuncLibName = bpFuncLibName), 'w') as headerFile:
        headerFile.write(headerCode)
    
    with open(OutputPath + CppFileName.format(BpFuncLibName = bpFuncLibName), 'w') as cppFile:
        cppFile.write(cppCode)

if __name__=='__main__':  
    if not os.path.exists(UnreaImportSettingsPath):
        print("{} not exists".format(UnreaImportSettingsPath))
    else:
        structLst = ParseStructFromJson(UnreaImportSettingsPath)
        GenerateCode(structLst)


