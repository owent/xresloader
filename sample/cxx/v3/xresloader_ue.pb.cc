// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: xresloader_ue.proto

#include "xresloader_ue.pb.h"

#include <algorithm>

#include <google/protobuf/io/coded_stream.h>
#include <google/protobuf/extension_set.h>
#include <google/protobuf/wire_format_lite.h>
#include <google/protobuf/descriptor.h>
#include <google/protobuf/generated_message_reflection.h>
#include <google/protobuf/reflection_ops.h>
#include <google/protobuf/wire_format.h>
// @@protoc_insertion_point(includes)
#include <google/protobuf/port_def.inc>
namespace org {
namespace xresloader {
namespace ue {
}  // namespace ue
}  // namespace xresloader
}  // namespace org
static constexpr ::PROTOBUF_NAMESPACE_ID::Metadata* file_level_metadata_xresloader_5fue_2eproto = nullptr;
static constexpr ::PROTOBUF_NAMESPACE_ID::EnumDescriptor const** file_level_enum_descriptors_xresloader_5fue_2eproto = nullptr;
static constexpr ::PROTOBUF_NAMESPACE_ID::ServiceDescriptor const** file_level_service_descriptors_xresloader_5fue_2eproto = nullptr;
const ::PROTOBUF_NAMESPACE_ID::uint32 TableStruct_xresloader_5fue_2eproto::offsets[1] = {};
static constexpr ::PROTOBUF_NAMESPACE_ID::internal::MigrationSchema* schemas = nullptr;
static constexpr ::PROTOBUF_NAMESPACE_ID::Message* const* file_default_instances = nullptr;

const char descriptor_table_protodef_xresloader_5fue_2eproto[] PROTOBUF_SECTION_VARIABLE(protodesc_cold) =
  "\n\023xresloader_ue.proto\022\021org.xresloader.ue"
  "\032 google/protobuf/descriptor.proto:/\n\007ke"
  "y_tag\022\035.google.protobuf.FieldOptions\030\315\010 "
  "\001(\003:4\n\014ue_type_name\022\035.google.protobuf.Fi"
  "eldOptions\030\316\010 \001(\t:8\n\020ue_type_is_class\022\035."
  "google.protobuf.FieldOptions\030\317\010 \001(\010:0\n\006h"
  "elper\022\037.google.protobuf.MessageOptions\030\315"
  "\010 \001(\t:8\n\016not_data_table\022\037.google.protobu"
  "f.MessageOptions\030\316\010 \001(\010b\006proto3"
  ;
static const ::PROTOBUF_NAMESPACE_ID::internal::DescriptorTable*const descriptor_table_xresloader_5fue_2eproto_deps[1] = {
  &::descriptor_table_google_2fprotobuf_2fdescriptor_2eproto,
};
static ::PROTOBUF_NAMESPACE_ID::internal::SCCInfoBase*const descriptor_table_xresloader_5fue_2eproto_sccs[1] = {
};
static ::PROTOBUF_NAMESPACE_ID::internal::once_flag descriptor_table_xresloader_5fue_2eproto_once;
const ::PROTOBUF_NAMESPACE_ID::internal::DescriptorTable descriptor_table_xresloader_5fue_2eproto = {
  false, false, descriptor_table_protodef_xresloader_5fue_2eproto, "xresloader_ue.proto", 351,
  &descriptor_table_xresloader_5fue_2eproto_once, descriptor_table_xresloader_5fue_2eproto_sccs, descriptor_table_xresloader_5fue_2eproto_deps, 0, 1,
  schemas, file_default_instances, TableStruct_xresloader_5fue_2eproto::offsets,
  file_level_metadata_xresloader_5fue_2eproto, 0, file_level_enum_descriptors_xresloader_5fue_2eproto, file_level_service_descriptors_xresloader_5fue_2eproto,
};

// Force running AddDescriptors() at dynamic initialization time.
static bool dynamic_init_dummy_xresloader_5fue_2eproto = (static_cast<void>(::PROTOBUF_NAMESPACE_ID::internal::AddDescriptors(&descriptor_table_xresloader_5fue_2eproto)), true);
namespace org {
namespace xresloader {
namespace ue {
::PROTOBUF_NAMESPACE_ID::internal::ExtensionIdentifier< ::google::protobuf::FieldOptions,
    ::PROTOBUF_NAMESPACE_ID::internal::PrimitiveTypeTraits< ::PROTOBUF_NAMESPACE_ID::int64 >, 3, false >
  key_tag(kKeyTagFieldNumber, PROTOBUF_LONGLONG(0));
const std::string ue_type_name_default("");
::PROTOBUF_NAMESPACE_ID::internal::ExtensionIdentifier< ::google::protobuf::FieldOptions,
    ::PROTOBUF_NAMESPACE_ID::internal::StringTypeTraits, 9, false >
  ue_type_name(kUeTypeNameFieldNumber, ue_type_name_default);
::PROTOBUF_NAMESPACE_ID::internal::ExtensionIdentifier< ::google::protobuf::FieldOptions,
    ::PROTOBUF_NAMESPACE_ID::internal::PrimitiveTypeTraits< bool >, 8, false >
  ue_type_is_class(kUeTypeIsClassFieldNumber, false);
const std::string helper_default("");
::PROTOBUF_NAMESPACE_ID::internal::ExtensionIdentifier< ::google::protobuf::MessageOptions,
    ::PROTOBUF_NAMESPACE_ID::internal::StringTypeTraits, 9, false >
  helper(kHelperFieldNumber, helper_default);
::PROTOBUF_NAMESPACE_ID::internal::ExtensionIdentifier< ::google::protobuf::MessageOptions,
    ::PROTOBUF_NAMESPACE_ID::internal::PrimitiveTypeTraits< bool >, 8, false >
  not_data_table(kNotDataTableFieldNumber, false);

// @@protoc_insertion_point(namespace_scope)
}  // namespace ue
}  // namespace xresloader
}  // namespace org
PROTOBUF_NAMESPACE_OPEN
PROTOBUF_NAMESPACE_CLOSE

// @@protoc_insertion_point(global_scope)
#include <google/protobuf/port_undef.inc>
