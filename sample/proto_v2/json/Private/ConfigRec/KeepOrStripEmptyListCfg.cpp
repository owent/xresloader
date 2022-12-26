/**
 * This file is generated by xresloader 2.13.0, please don't edit it.
 * You can find more information about this xresloader on https://xresloader.atframe.work/ .
 * If there is any problem, please find or report issues on https://github.com/xresloader/xresloader/issues .
 */
#include "ConfigRec/KeepOrStripEmptyListCfg.h"



UKeepOrStripEmptyListCfgHelper::UKeepOrStripEmptyListCfgHelper() : Super()
{
    UKeepOrStripEmptyListCfgHelper::ClearRow(this->Empty);
    this->DataTable = nullptr;
    this->EnableDefaultLoader = true;
}

void UKeepOrStripEmptyListCfgHelper::OnReload()
{
    // TODO Rebuild Index
}

void UKeepOrStripEmptyListCfgHelper::SetLoader(TSharedPtr<ConstructorHelpers::FObjectFinder<UDataTable> > NewLoader)
{
    this->Loader = NewLoader;
    if (this->Loader && this->Loader->Succeeded())
    {
        this->DataTable = this->Loader->Object;
        this->DataTable->OnDataTableChanged().AddUObject(this, &UKeepOrStripEmptyListCfgHelper::OnReload);
        OnReload();
    }
    else
    {
        this->DataTable = nullptr;
    }
}

void UKeepOrStripEmptyListCfgHelper::InitializeDefaultLoader() const
{
    if (!this->EnableDefaultLoader) {
        return;
    }
    const_cast<UKeepOrStripEmptyListCfgHelper*>(this)->EnableDefaultLoader = false;
    const_cast<UKeepOrStripEmptyListCfgHelper*>(this)->SetLoader(MakeShareable(new ConstructorHelpers::FObjectFinder<UDataTable>(TEXT("DataTable'/Game/ConfigRec/KeepEmptyListCfg'"))));
}

void UKeepOrStripEmptyListCfgHelper::DisableDefaultLoader()
{
    this->EnableDefaultLoader = false;
}

FName UKeepOrStripEmptyListCfgHelper::GetRowName(int32 Id)
{
    return *FString::Printf(TEXT("%lld"), static_cast<long long>(Id));
}

FName UKeepOrStripEmptyListCfgHelper::GetDataRowName(int32 Id) const
{
    return UKeepOrStripEmptyListCfgHelper::GetRowName(Id);
}

FName UKeepOrStripEmptyListCfgHelper::GetTableRowName(const FKeepOrStripEmptyListCfg& TableRow) const
{
    return GetDataRowName(TableRow.Id);
}

const FKeepOrStripEmptyListCfg& UKeepOrStripEmptyListCfgHelper::GetDataRowByName(const FName& Name, bool& IsValid) const
{
    IsValid = false;
    if (!this->DataTable && this->EnableDefaultLoader && !this->Loader.IsValid()) {
        this->InitializeDefaultLoader();
    }
    if (!this->DataTable) {
        return this->Empty;
    }

    FString Context;
    FKeepOrStripEmptyListCfg* LookupRow = DataTable->FindRow<FKeepOrStripEmptyListCfg>(Name, Context, false);
    if (!LookupRow) {
        return this->Empty;
    };

    IsValid = true;
    return *LookupRow;
}

const FKeepOrStripEmptyListCfg& UKeepOrStripEmptyListCfgHelper::GetDataRowByKey(int32 Id, bool& IsValid) const
{
    return GetDataRowByName(GetDataRowName(Id), IsValid);
}

bool UKeepOrStripEmptyListCfgHelper::ForeachRow(TFunctionRef<void (const FName& Key, const FKeepOrStripEmptyListCfg& Value)> Predicate) const
{
    if (!this->DataTable && this->EnableDefaultLoader && !this->Loader.IsValid()) {
        this->InitializeDefaultLoader();
    }
    if (!this->DataTable) {
        return false;
    }

    FString Context;
    this->DataTable->ForeachRow(Context, Predicate);
    return true;
}

UDataTable* UKeepOrStripEmptyListCfgHelper::GetRawDataTable(bool& IsValid) const
{
    IsValid = false;
    if (!this->DataTable && this->EnableDefaultLoader && !this->Loader.IsValid()) {
        this->InitializeDefaultLoader();
    }
    if (!this->DataTable) {
        return NULL;
    }

    IsValid = true;
    return this->DataTable;
}

void UKeepOrStripEmptyListCfgHelper::ClearRow(FKeepOrStripEmptyListCfg& TableRow)
{
    TableRow.Name = TEXT("");
    TableRow.Id = 0;
    TableRow.ArrayMsg.Reset(0);
    TableRow.ArrayPlainMsg.Reset(0);
    TableRow.ArrayInt32.Reset(0);
    TableRow.ArrayInt64.Reset(0);
}

void UKeepOrStripEmptyListCfgHelper::ClearDataRow(FKeepOrStripEmptyListCfg& TableRow) const
{
    UKeepOrStripEmptyListCfgHelper::ClearRow(TableRow);
}

