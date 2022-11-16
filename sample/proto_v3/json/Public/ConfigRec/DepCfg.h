/**
 * This file is generated by xresloader 2.12.1-pracing, please don't edit it.
 * You can find more information about this xresloader on https://xresloader.atframe.work/ .
 * If there is any problem, please find or report issues on https://github.com/xresloader/xresloader/issues .
 */
#pragma once

#include "CoreMinimal.h"
#include "UObject/ConstructorHelpers.h"
#include "Engine/DataTable.h"
#include "ConfigRec/Dep2Cfg.h"
#include "DepCfg.generated.h"


USTRUCT(BlueprintType)
struct FDepCfg : public FTableRowBase
{
    GENERATED_USTRUCT_BODY()

    // Start of fields
    /** Field Type: STRING, Name: Name, Index: 2. This field is generated for UE Editor compatible. **/
    UPROPERTY(EditAnywhere, BlueprintReadOnly, Category = "XResConfig")
    FName Name;

    /** Field Type: INT, Name: Id, Index: 1. This field is generated for UE Editor compatible. **/
    UPROPERTY(EditAnywhere, BlueprintReadOnly, Category = "XResConfig")
    int32 Id;

    /** Field Type: MESSAGE, Name: Dep2, Index: 3. This field is generated for UE Editor compatible. **/
    UPROPERTY(EditAnywhere, BlueprintReadOnly, Category = "XResConfig")
    FDep2Cfg Dep2;

};