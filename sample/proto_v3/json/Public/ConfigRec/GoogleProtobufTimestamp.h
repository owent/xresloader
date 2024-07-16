/**
 * This file is generated by xresloader 2.17.1, please don't edit it.
 * You can find more information about this xresloader on https://xresloader.atframe.work/ .
 * If there is any problem, please find or report issues on https://github.com/xresloader/xresloader/issues .
 */
#pragma once

#include "CoreMinimal.h"
#include "UObject/ConstructorHelpers.h"
#include "Engine/DataTable.h"

#include "GoogleProtobufTimestamp.generated.h"


USTRUCT(BlueprintType)
struct FGoogleProtobufTimestamp : public FTableRowBase
{
    GENERATED_USTRUCT_BODY()

    // Start of fields
    /** Field Type: LONG, Name: Seconds, Index: 1. This field is generated for UE Editor compatible. **/
    UPROPERTY(EditAnywhere, BlueprintReadOnly, Category = "XResConfig")
    int64 Seconds;

    /** Field Type: INT, Name: Nanos, Index: 2. This field is generated for UE Editor compatible. **/
    UPROPERTY(EditAnywhere, BlueprintReadOnly, Category = "XResConfig")
    int32 Nanos;

};