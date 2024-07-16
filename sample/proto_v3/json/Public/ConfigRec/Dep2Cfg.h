/**
 * This file is generated by xresloader 2.17.1, please don't edit it.
 * You can find more information about this xresloader on https://xresloader.atframe.work/ .
 * If there is any problem, please find or report issues on https://github.com/xresloader/xresloader/issues .
 */
#pragma once

#include "CoreMinimal.h"
#include "UObject/ConstructorHelpers.h"
#include "Engine/DataTable.h"

#include "Dep2Cfg.generated.h"


USTRUCT(BlueprintType)
struct FDep2Cfg : public FTableRowBase
{
    GENERATED_USTRUCT_BODY()

    // Start of fields
    /** Field Type: INT, Name: Id, Index: 1. This field is generated for UE Editor compatible. **/
    UPROPERTY(EditAnywhere, BlueprintReadOnly, Category = "XResConfig")
    int32 Id;

    /** Field Type: STRING, Name: Level, Index: 2. This field is generated for UE Editor compatible. **/
    UPROPERTY(EditAnywhere, BlueprintReadOnly, Category = "XResConfig")
    FString Level;

};