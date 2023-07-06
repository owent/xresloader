/**
 * This file is generated by xresloader 2.14.0-rc3, please don't edit it.
 * You can find more information about this xresloader on https://xresloader.atframe.work/ .
 * If there is any problem, please find or report issues on https://github.com/xresloader/xresloader/issues .
 */
#pragma once

#include "CoreMinimal.h"
#include "UObject/ConstructorHelpers.h"
#include "Engine/DataTable.h"
#include "ConfigRec/GoogleProtobufDuration.h"
#include "ConfigRec/GoogleProtobufTimestamp.h"
#include "ConfigRec/DepCfg.h"

#include "RoleCfg.generated.h"


USTRUCT(BlueprintType)
struct FRoleCfg : public FTableRowBase
{
    GENERATED_USTRUCT_BODY()

    // Start of fields
    /** Field Type: STRING, Name: Name, Index: 5. This field is generated for UE Editor compatible. **/
    UPROPERTY(EditAnywhere, BlueprintReadOnly, Category = "XResConfig")
    FName Name;

    /** Field Type: INT, Name: Id, Index: 1. This field is generated for UE Editor compatible. **/
    UPROPERTY(EditAnywhere, BlueprintReadOnly, Category = "XResConfig")
    int32 Id;

    /** Field Type: INT, Name: UnlockLevel, Index: 2. This field is generated for UE Editor compatible. **/
    UPROPERTY(EditAnywhere, BlueprintReadOnly, Category = "XResConfig")
    int32 UnlockLevel;

    /** Field Type: INT, Name: CostType, Index: 3. This field is generated for UE Editor compatible. **/
    UPROPERTY(EditAnywhere, BlueprintReadOnly, Category = "XResConfig")
    int32 CostType;

    /** Field Type: INT, Name: CostValue, Index: 4. This field is generated for UE Editor compatible. **/
    UPROPERTY(EditAnywhere, BlueprintReadOnly, Category = "XResConfig")
    int32 CostValue;

    /** Field Type: MESSAGE, Name: DepTest, Index: 10. This field is generated for UE Editor compatible. **/
    UPROPERTY(EditAnywhere, BlueprintReadOnly, Category = "XResConfig")
    FDepCfg DepTest;

    /** Field Type: STRING, Name: TestArray, Index: 11. This field is generated for UE Editor compatible. **/
    UPROPERTY(EditAnywhere, BlueprintReadOnly, Category = "XResConfig")
    TArray< FString > TestArray;

    /** Field Type: STRING, Name: IntAsString, Index: 12. This field is generated for UE Editor compatible. **/
    UPROPERTY(EditAnywhere, BlueprintReadOnly, Category = "XResConfig")
    FString IntAsString;

    /** Field Type: INT, Name: TestPlainEnumArray, Index: 13. This field is generated for UE Editor compatible. **/
    UPROPERTY(EditAnywhere, BlueprintReadOnly, Category = "XResConfig")
    TArray< int32 > TestPlainEnumArray;

    /** Field Type: MESSAGE, Name: ConvertTimepointOne, Index: 21. This field is generated for UE Editor compatible. **/
    UPROPERTY(EditAnywhere, BlueprintReadOnly, Category = "XResConfig")
    FGoogleProtobufTimestamp ConvertTimepointOne;

    /** Field Type: STRING, Name: OriginTimepointOne, Index: 22. This field is generated for UE Editor compatible. **/
    UPROPERTY(EditAnywhere, BlueprintReadOnly, Category = "XResConfig")
    FString OriginTimepointOne;

    /** Field Type: MESSAGE, Name: ConvertDurationOne, Index: 23. This field is generated for UE Editor compatible. **/
    UPROPERTY(EditAnywhere, BlueprintReadOnly, Category = "XResConfig")
    FGoogleProtobufDuration ConvertDurationOne;

    /** Field Type: STRING, Name: OriginDurationOne, Index: 24. This field is generated for UE Editor compatible. **/
    UPROPERTY(EditAnywhere, BlueprintReadOnly, Category = "XResConfig")
    FString OriginDurationOne;

    /** Field Type: MESSAGE, Name: ConvertTimepointArr, Index: 25. This field is generated for UE Editor compatible. **/
    UPROPERTY(EditAnywhere, BlueprintReadOnly, Category = "XResConfig")
    TArray< FGoogleProtobufTimestamp > ConvertTimepointArr;

    /** Field Type: STRING, Name: OriginTimepointArr, Index: 26. This field is generated for UE Editor compatible. **/
    UPROPERTY(EditAnywhere, BlueprintReadOnly, Category = "XResConfig")
    TArray< FString > OriginTimepointArr;

    /** Field Type: MESSAGE, Name: ConvertDurationArr, Index: 27. This field is generated for UE Editor compatible. **/
    UPROPERTY(EditAnywhere, BlueprintReadOnly, Category = "XResConfig")
    TArray< FGoogleProtobufDuration > ConvertDurationArr;

    /** Field Type: STRING, Name: OriginDurationArr, Index: 28. This field is generated for UE Editor compatible. **/
    UPROPERTY(EditAnywhere, BlueprintReadOnly, Category = "XResConfig")
    TArray< FString > OriginDurationArr;

};