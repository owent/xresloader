/**
 * This file is generated by xresloader 2.15.1, please don't edit it.
 * You can find more information about this xresloader on https://xresloader.atframe.work/ .
 * If there is any problem, please find or report issues on https://github.com/xresloader/xresloader/issues .
 */
#pragma once

#include "CoreMinimal.h"
#include "UObject/ConstructorHelpers.h"
#include "Engine/DataTable.h"
#include "ConfigRec/Dep2Cfg.h"

#include "KeepOrStripEmptyListCfg.generated.h"


USTRUCT(BlueprintType)
struct FKeepOrStripEmptyListCfg : public FTableRowBase
{
    GENERATED_USTRUCT_BODY()

    // Start of fields
    /** Field Type: STRING, Name: Name, Index: 0. This field is generated for UE Editor compatible. **/
    UPROPERTY(EditAnywhere, BlueprintReadOnly, Category = "XResConfig")
    FName Name;

    // This is a Key
    /** Field Type: INT, Name: Id, Index: 1. This field is generated for UE Editor compatible. **/
    UPROPERTY(EditAnywhere, BlueprintReadOnly, Category = "XResConfig")
    int32 Id;

    /** Field Type: MESSAGE, Name: ArrayMsg, Index: 2. This field is generated for UE Editor compatible. **/
    UPROPERTY(EditAnywhere, BlueprintReadOnly, Category = "XResConfig")
    TArray< FDep2Cfg > ArrayMsg;

    /** Field Type: MESSAGE, Name: ArrayPlainMsg, Index: 3. This field is generated for UE Editor compatible. **/
    UPROPERTY(EditAnywhere, BlueprintReadOnly, Category = "XResConfig")
    TArray< FDep2Cfg > ArrayPlainMsg;

    /** Field Type: INT, Name: ArrayInt32, Index: 4. This field is generated for UE Editor compatible. **/
    UPROPERTY(EditAnywhere, BlueprintReadOnly, Category = "XResConfig")
    TArray< int32 > ArrayInt32;

    /** Field Type: LONG, Name: ArrayInt64, Index: 5. This field is generated for UE Editor compatible. **/
    UPROPERTY(EditAnywhere, BlueprintReadOnly, Category = "XResConfig")
    TArray< int64 > ArrayInt64;

};


UCLASS(Blueprintable, BlueprintType)
class UKeepOrStripEmptyListCfgHelper : public UObject
{
    GENERATED_BODY()

public:
    UKeepOrStripEmptyListCfgHelper();

    void OnReload();

    void SetLoader(TSharedPtr<ConstructorHelpers::FObjectFinder<UDataTable> > NewLoader);

    void InitializeDefaultLoader() const;

    void DisableDefaultLoader();

    const TCHAR* GetObjectPath() const;

    static FName GetRowName(int32 Id);

    UFUNCTION(BlueprintCallable, Category = "XResConfig")
    FName GetDataRowName(int32 Id) const;

    UFUNCTION(BlueprintCallable, Category = "XResConfig")
    FName GetTableRowName(const FKeepOrStripEmptyListCfg& TableRow) const;

    UFUNCTION(BlueprintCallable, Category = "XResConfig")
    const FKeepOrStripEmptyListCfg& GetDataRowByName(const FName& Name, bool& IsValid) const;

    UFUNCTION(BlueprintCallable, Category = "XResConfig")
    const FKeepOrStripEmptyListCfg& GetDataRowByKey(int32 Id, bool& IsValid) const;

    bool ForeachRow(TFunctionRef<void (const FName& Key, const FKeepOrStripEmptyListCfg& Value)> Predicate) const;

    UFUNCTION(BlueprintCallable, Category = "XResConfig")
    UDataTable* GetRawDataTable(bool& IsValid) const;

    static void ClearRow(FKeepOrStripEmptyListCfg& TableRow);

    UFUNCTION(BlueprintCallable, Category = "XResConfig")
    void ClearDataRow(FKeepOrStripEmptyListCfg& TableRow) const;

private:
    TSharedPtr<ConstructorHelpers::FObjectFinder<UDataTable> > Loader;
    UDataTable* DataTable;
    bool EnableDefaultLoader;
    FKeepOrStripEmptyListCfg Empty;
};

