/**
 * This file is generated by xresloader 2.12.0, please don't edit it.
 * You can find more information about this xresloader on https://xresloader.atframe.work/ .
 * If there is any problem, please find or report issues on https://github.com/xresloader/xresloader/issues .
 */
// Test role_upgrade_cfg with multi keys

#include "ConfigRec/RoleUpgradeCfg.h"



URoleUpgradeCfgHelper::URoleUpgradeCfgHelper() : Super()
{
    URoleUpgradeCfgHelper::ClearRow(this->Empty);
    this->DataTable = nullptr;
    this->EnableDefaultLoader = true;
}

void URoleUpgradeCfgHelper::OnReload()
{
    // TODO Rebuild Index
}

void URoleUpgradeCfgHelper::SetLoader(TSharedPtr<ConstructorHelpers::FObjectFinder<UDataTable> > NewLoader)
{
    this->Loader = NewLoader;
    if (this->Loader && this->Loader->Succeeded())
    {
        this->DataTable = this->Loader->Object;
        this->DataTable->OnDataTableChanged().AddUObject(this, &URoleUpgradeCfgHelper::OnReload);
        OnReload();
    }
    else
    {
        this->DataTable = nullptr;
    }
}

void URoleUpgradeCfgHelper::InitializeDefaultLoader() const
{
    if (!this->EnableDefaultLoader) {
        return;
    }
    const_cast<URoleUpgradeCfgHelper*>(this)->EnableDefaultLoader = false;
    const_cast<URoleUpgradeCfgHelper*>(this)->SetLoader(MakeShareable(new ConstructorHelpers::FObjectFinder<UDataTable>(TEXT("DataTable'/Game/ConfigRec/RoleUpgradeCfg'"))));
}

void URoleUpgradeCfgHelper::DisableDefaultLoader()
{
    this->EnableDefaultLoader = false;
}

FName URoleUpgradeCfgHelper::GetRowName(int32 Id, int32 Level)
{
    return *FString::Printf(TEXT("%lld"), static_cast<long long>(Id) * 1000 + static_cast<long long>(Level));
}

FName URoleUpgradeCfgHelper::GetDataRowName(int32 Id, int32 Level) const
{
    return URoleUpgradeCfgHelper::GetRowName(Id, Level);
}

FName URoleUpgradeCfgHelper::GetTableRowName(const FRoleUpgradeCfg& TableRow) const
{
    return GetDataRowName(TableRow.Id, TableRow.Level);
}

const FRoleUpgradeCfg& URoleUpgradeCfgHelper::GetDataRowByName(const FName& Name, bool& IsValid) const
{
    IsValid = false;
    if (!this->DataTable && this->EnableDefaultLoader && !this->Loader.IsValid()) {
        this->InitializeDefaultLoader();
    }
    if (!this->DataTable) {
        return this->Empty;
    }

    FString Context;
    FRoleUpgradeCfg* LookupRow = DataTable->FindRow<FRoleUpgradeCfg>(Name, Context, false);
    if (!LookupRow) {
        return this->Empty;
    };

    IsValid = true;
    return *LookupRow;
}

const FRoleUpgradeCfg& URoleUpgradeCfgHelper::GetDataRowByKey(int32 Id, int32 Level, bool& IsValid) const
{
    return GetDataRowByName(GetDataRowName(Id, Level), IsValid);
}

bool URoleUpgradeCfgHelper::ForeachRow(TFunctionRef<void (const FName& Key, const FRoleUpgradeCfg& Value)> Predicate) const
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

UDataTable* URoleUpgradeCfgHelper::GetRawDataTable(bool& IsValid) const
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

void URoleUpgradeCfgHelper::ClearRow(FRoleUpgradeCfg& TableRow)
{
    TableRow.Name = TEXT("");
    TableRow.Id = 0;
    TableRow.Level = 0;
    TableRow.CostType = 0;
    TableRow.CostValue = 0;
    TableRow.ScoreAdd = 0;
}

void URoleUpgradeCfgHelper::ClearDataRow(FRoleUpgradeCfg& TableRow) const
{
    URoleUpgradeCfgHelper::ClearRow(TableRow);
}

