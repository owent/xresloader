/**
 * This file is generated by xresloader 2.7.2, please don't edit it.
 * You can find more information about this xresloader on https://xresloader.atframe.work/ .
 * If there is any problem, please find or report issues on https://github.com/xresloader/xresloader/issues .
 */
#include "KindConst.h"



UKindConstHelper::UKindConstHelper() : Super()
{
    UKindConstHelper::ClearRow(this->Empty);
    this->Loader = MakeShareable(new ConstructorHelpers::FObjectFinder<UDataTable>(TEXT("DataTable'/Game/DataTable/KindConst'")));
    if (this->Loader && this->Loader->Succeeded())
    {
        this->DataTable = this->Loader->Object;
        this->DataTable->OnDataTableChanged().AddUObject(this, &UKindConstHelper::OnReload);
        OnReload();
    }
    else
    {
        this->DataTable = nullptr;
    }
}

void UKindConstHelper::OnReload()
{
    // TODO Rebuild Index
}

FName UKindConstHelper::GetRowName(FName Name)
{
    return Name;
}

FName UKindConstHelper::GetDataRowName(FName Name) const
{
    return UKindConstHelper::GetRowName(Name);
}

FName UKindConstHelper::GetTableRowName(const FKindConst& TableRow) const
{
    return GetDataRowName(TableRow.Name);
}

const FKindConst& UKindConstHelper::GetDataRowByName(const FName& Name, bool& IsValid) const
{
    IsValid = false;
    if (!this->DataTable) {
        return this->Empty;
    }

    FString Context;
    FKindConst* LookupRow = DataTable->FindRow<FKindConst>(Name, Context, false);
    if (!LookupRow) {
        return this->Empty;
    };

    IsValid = true;
    return *LookupRow;
}

const FKindConst& UKindConstHelper::GetDataRowByKey(FName Name, bool& IsValid) const
{
    return GetDataRowByName(GetDataRowName(Name), IsValid);
}

bool UKindConstHelper::ForeachRow(TFunctionRef<void (const FName& Key, const FKindConst& Value)> Predicate) const
{
    if (!this->DataTable) {
        return false;
    }

    FString Context;
    this->DataTable->ForeachRow(Context, Predicate);
    return true;
}

UDataTable* UKindConstHelper::GetRawDataTable(bool& IsValid) const
{
    IsValid = false;
    if (!this->DataTable) {
        return NULL;
    }

    IsValid = true;
    return this->DataTable;
}

void UKindConstHelper::ClearRow(FKindConst& TableRow)
{
    TableRow.Name = TEXT("");
    TableRow.Value = 0;
}

void UKindConstHelper::ClearDataRow(FKindConst& TableRow) const
{
    UKindConstHelper::ClearRow(TableRow);
}

