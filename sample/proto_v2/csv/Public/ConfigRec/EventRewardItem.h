/**
 * This file is generated by xresloader 2.19.1, please don't edit it.
 * You can find more information about this xresloader on https://xresloader.atframe.work/ .
 * If there is any problem, please find or report issues on https://github.com/xresloader/xresloader/issues .
 */
#pragma once

#include "CoreMinimal.h"
#include "UObject/ConstructorHelpers.h"
#include "Engine/DataTable.h"
// Include headers set by UeCfg-IncludeHeader
#include "Engine/CompositeDataTable.h"


#include "EventRewardItem.generated.h"


USTRUCT(BlueprintType)
struct FEventRewardItem : public FTableRowBase
{
    GENERATED_USTRUCT_BODY()

    // Start of fields
    /** Field Type: oneof/union -> FString, Name: Nested, Index: 0. This field is generated for UE Editor compatible. **/
    UPROPERTY(EditAnywhere, BlueprintReadOnly, Category = "XResConfig")
    FString Nested;

    /** Field Type: INT, Name: ItemId, Index: 1. This field is generated for UE Editor compatible. **/
    UPROPERTY(EditAnywhere, BlueprintReadOnly, Category = "XResConfig")
    int32 ItemId = 0;

    /** Field Type: INT, Name: ItemCount, Index: 2. This field is generated for UE Editor compatible. **/
    UPROPERTY(EditAnywhere, BlueprintReadOnly, Category = "XResConfig")
    int32 ItemCount = 0;

    /** Field Type: STRING, Name: NestedNote, Index: 11. This field is generated for UE Editor compatible. **/
    UPROPERTY(EditAnywhere, BlueprintReadOnly, Category = "XResConfig")
    FString NestedNote;

    /** Field Type: INT, Name: NestedEnumType, Index: 12. This field is generated for UE Editor compatible. **/
    UPROPERTY(EditAnywhere, BlueprintReadOnly, Category = "XResConfig")
    int32 NestedEnumType = 0;

};