// Generated by the protocol buffer compiler.  DO NOT EDIT!
// NO CHECKED-IN PROTOBUF GENCODE
// source: dep_level2.proto
// Protobuf C++ Version: 5.28.0

#include "dep_level2.pb.h"

#include <algorithm>
#include <type_traits>
#include "google/protobuf/io/coded_stream.h"
#include "google/protobuf/generated_message_tctable_impl.h"
#include "google/protobuf/extension_set.h"
#include "google/protobuf/wire_format_lite.h"
#include "google/protobuf/descriptor.h"
#include "google/protobuf/generated_message_reflection.h"
#include "google/protobuf/reflection_ops.h"
#include "google/protobuf/wire_format.h"
// @@protoc_insertion_point(includes)

// Must be included last.
#include "google/protobuf/port_def.inc"
PROTOBUF_PRAGMA_INIT_SEG
namespace _pb = ::google::protobuf;
namespace _pbi = ::google::protobuf::internal;
namespace _fl = ::google::protobuf::internal::field_layout;

inline constexpr dep2_cfg::Impl_::Impl_(
    ::_pbi::ConstantInitialized) noexcept
      : level_(
            &::google::protobuf::internal::fixed_address_empty_string,
            ::_pbi::ConstantInitialized()),
        id_{0u},
        _cached_size_{0} {}

template <typename>
PROTOBUF_CONSTEXPR dep2_cfg::dep2_cfg(::_pbi::ConstantInitialized)
#if defined(PROTOBUF_CUSTOM_VTABLE)
    : ::google::protobuf::Message(_class_data_.base()),
#else   // PROTOBUF_CUSTOM_VTABLE
    : ::google::protobuf::Message(),
#endif  // PROTOBUF_CUSTOM_VTABLE
      _impl_(::_pbi::ConstantInitialized()) {
}
struct dep2_cfgDefaultTypeInternal {
  PROTOBUF_CONSTEXPR dep2_cfgDefaultTypeInternal() : _instance(::_pbi::ConstantInitialized{}) {}
  ~dep2_cfgDefaultTypeInternal() {}
  union {
    dep2_cfg _instance;
  };
};

PROTOBUF_ATTRIBUTE_NO_DESTROY PROTOBUF_CONSTINIT
    PROTOBUF_ATTRIBUTE_INIT_PRIORITY1 dep2_cfgDefaultTypeInternal _dep2_cfg_default_instance_;
static constexpr const ::_pb::EnumDescriptor**
    file_level_enum_descriptors_dep_5flevel2_2eproto = nullptr;
static constexpr const ::_pb::ServiceDescriptor**
    file_level_service_descriptors_dep_5flevel2_2eproto = nullptr;
const ::uint32_t
    TableStruct_dep_5flevel2_2eproto::offsets[] ABSL_ATTRIBUTE_SECTION_VARIABLE(
        protodesc_cold) = {
        ~0u,  // no _has_bits_
        PROTOBUF_FIELD_OFFSET(::dep2_cfg, _internal_metadata_),
        ~0u,  // no _extensions_
        ~0u,  // no _oneof_case_
        ~0u,  // no _weak_field_map_
        ~0u,  // no _inlined_string_donated_
        ~0u,  // no _split_
        ~0u,  // no sizeof(Split)
        PROTOBUF_FIELD_OFFSET(::dep2_cfg, _impl_.id_),
        PROTOBUF_FIELD_OFFSET(::dep2_cfg, _impl_.level_),
};

static const ::_pbi::MigrationSchema
    schemas[] ABSL_ATTRIBUTE_SECTION_VARIABLE(protodesc_cold) = {
        {0, -1, -1, sizeof(::dep2_cfg)},
};
static const ::_pb::Message* const file_default_instances[] = {
    &::_dep2_cfg_default_instance_._instance,
};
const char descriptor_table_protodef_dep_5flevel2_2eproto[] ABSL_ATTRIBUTE_SECTION_VARIABLE(
    protodesc_cold) = {
    "\n\020dep_level2.proto\"%\n\010dep2_cfg\022\n\n\002id\030\001 \001"
    "(\r\022\r\n\005level\030\002 \001(\tb\006proto3"
};
static ::absl::once_flag descriptor_table_dep_5flevel2_2eproto_once;
PROTOBUF_CONSTINIT const ::_pbi::DescriptorTable descriptor_table_dep_5flevel2_2eproto = {
    false,
    false,
    65,
    descriptor_table_protodef_dep_5flevel2_2eproto,
    "dep_level2.proto",
    &descriptor_table_dep_5flevel2_2eproto_once,
    nullptr,
    0,
    1,
    schemas,
    file_default_instances,
    TableStruct_dep_5flevel2_2eproto::offsets,
    file_level_enum_descriptors_dep_5flevel2_2eproto,
    file_level_service_descriptors_dep_5flevel2_2eproto,
};
// ===================================================================

class dep2_cfg::_Internal {
 public:
};

dep2_cfg::dep2_cfg(::google::protobuf::Arena* arena)
#if defined(PROTOBUF_CUSTOM_VTABLE)
    : ::google::protobuf::Message(arena, _class_data_.base()) {
#else   // PROTOBUF_CUSTOM_VTABLE
    : ::google::protobuf::Message(arena) {
#endif  // PROTOBUF_CUSTOM_VTABLE
  SharedCtor(arena);
  // @@protoc_insertion_point(arena_constructor:dep2_cfg)
}
inline PROTOBUF_NDEBUG_INLINE dep2_cfg::Impl_::Impl_(
    ::google::protobuf::internal::InternalVisibility visibility, ::google::protobuf::Arena* arena,
    const Impl_& from, const ::dep2_cfg& from_msg)
      : level_(arena, from.level_),
        _cached_size_{0} {}

dep2_cfg::dep2_cfg(
    ::google::protobuf::Arena* arena,
    const dep2_cfg& from)
#if defined(PROTOBUF_CUSTOM_VTABLE)
    : ::google::protobuf::Message(arena, _class_data_.base()) {
#else   // PROTOBUF_CUSTOM_VTABLE
    : ::google::protobuf::Message(arena) {
#endif  // PROTOBUF_CUSTOM_VTABLE
  dep2_cfg* const _this = this;
  (void)_this;
  _internal_metadata_.MergeFrom<::google::protobuf::UnknownFieldSet>(
      from._internal_metadata_);
  new (&_impl_) Impl_(internal_visibility(), arena, from._impl_, from);
  _impl_.id_ = from._impl_.id_;

  // @@protoc_insertion_point(copy_constructor:dep2_cfg)
}
inline PROTOBUF_NDEBUG_INLINE dep2_cfg::Impl_::Impl_(
    ::google::protobuf::internal::InternalVisibility visibility,
    ::google::protobuf::Arena* arena)
      : level_(arena),
        _cached_size_{0} {}

inline void dep2_cfg::SharedCtor(::_pb::Arena* arena) {
  new (&_impl_) Impl_(internal_visibility(), arena);
  _impl_.id_ = {};
}
dep2_cfg::~dep2_cfg() {
  // @@protoc_insertion_point(destructor:dep2_cfg)
  _internal_metadata_.Delete<::google::protobuf::UnknownFieldSet>();
  SharedDtor();
}
inline void dep2_cfg::SharedDtor() {
  ABSL_DCHECK(GetArena() == nullptr);
  _impl_.level_.Destroy();
  _impl_.~Impl_();
}

PROTOBUF_CONSTINIT
PROTOBUF_ATTRIBUTE_INIT_PRIORITY1
const ::google::protobuf::MessageLite::ClassDataFull
    dep2_cfg::_class_data_ = {
        ::google::protobuf::Message::ClassData{
            &_dep2_cfg_default_instance_._instance,
            &_table_.header,
            nullptr,  // OnDemandRegisterArenaDtor
            nullptr,  // IsInitialized
            &dep2_cfg::MergeImpl,
#if defined(PROTOBUF_CUSTOM_VTABLE)
            ::google::protobuf::Message::GetDeleteImpl<dep2_cfg>(),
            ::google::protobuf::Message::GetNewImpl<dep2_cfg>(),
            ::google::protobuf::Message::GetClearImpl<dep2_cfg>(), &dep2_cfg::ByteSizeLong,
                &dep2_cfg::_InternalSerialize,
#endif  // PROTOBUF_CUSTOM_VTABLE
            PROTOBUF_FIELD_OFFSET(dep2_cfg, _impl_._cached_size_),
            false,
        },
        &dep2_cfg::kDescriptorMethods,
        &descriptor_table_dep_5flevel2_2eproto,
        nullptr,  // tracker
};
const ::google::protobuf::MessageLite::ClassData* dep2_cfg::GetClassData() const {
  ::google::protobuf::internal::PrefetchToLocalCache(&_class_data_);
  ::google::protobuf::internal::PrefetchToLocalCache(_class_data_.tc_table);
  return _class_data_.base();
}
PROTOBUF_CONSTINIT PROTOBUF_ATTRIBUTE_INIT_PRIORITY1
const ::_pbi::TcParseTable<1, 2, 0, 22, 2> dep2_cfg::_table_ = {
  {
    0,  // no _has_bits_
    0, // no _extensions_
    2, 8,  // max_field_number, fast_idx_mask
    offsetof(decltype(_table_), field_lookup_table),
    4294967292,  // skipmap
    offsetof(decltype(_table_), field_entries),
    2,  // num_field_entries
    0,  // num_aux_entries
    offsetof(decltype(_table_), field_names),  // no aux_entries
    _class_data_.base(),
    nullptr,  // post_loop_handler
    ::_pbi::TcParser::GenericFallback,  // fallback
    #ifdef PROTOBUF_PREFETCH_PARSE_TABLE
    ::_pbi::TcParser::GetTable<::dep2_cfg>(),  // to_prefetch
    #endif  // PROTOBUF_PREFETCH_PARSE_TABLE
  }, {{
    // string level = 2;
    {::_pbi::TcParser::FastUS1,
     {18, 63, 0, PROTOBUF_FIELD_OFFSET(dep2_cfg, _impl_.level_)}},
    // uint32 id = 1;
    {::_pbi::TcParser::SingularVarintNoZag1<::uint32_t, offsetof(dep2_cfg, _impl_.id_), 63>(),
     {8, 63, 0, PROTOBUF_FIELD_OFFSET(dep2_cfg, _impl_.id_)}},
  }}, {{
    65535, 65535
  }}, {{
    // uint32 id = 1;
    {PROTOBUF_FIELD_OFFSET(dep2_cfg, _impl_.id_), 0, 0,
    (0 | ::_fl::kFcSingular | ::_fl::kUInt32)},
    // string level = 2;
    {PROTOBUF_FIELD_OFFSET(dep2_cfg, _impl_.level_), 0, 0,
    (0 | ::_fl::kFcSingular | ::_fl::kUtf8String | ::_fl::kRepAString)},
  }},
  // no aux_entries
  {{
    "\10\0\5\0\0\0\0\0"
    "dep2_cfg"
    "level"
  }},
};

PROTOBUF_NOINLINE void dep2_cfg::Clear() {
// @@protoc_insertion_point(message_clear_start:dep2_cfg)
  ::google::protobuf::internal::TSanWrite(&_impl_);
  ::uint32_t cached_has_bits = 0;
  // Prevent compiler warnings about cached_has_bits being unused
  (void) cached_has_bits;

  _impl_.level_.ClearToEmpty();
  _impl_.id_ = 0u;
  _internal_metadata_.Clear<::google::protobuf::UnknownFieldSet>();
}

#if defined(PROTOBUF_CUSTOM_VTABLE)
        ::uint8_t* dep2_cfg::_InternalSerialize(
            const MessageLite& base, ::uint8_t* target,
            ::google::protobuf::io::EpsCopyOutputStream* stream) {
          const dep2_cfg& this_ = static_cast<const dep2_cfg&>(base);
#else   // PROTOBUF_CUSTOM_VTABLE
        ::uint8_t* dep2_cfg::_InternalSerialize(
            ::uint8_t* target,
            ::google::protobuf::io::EpsCopyOutputStream* stream) const {
          const dep2_cfg& this_ = *this;
#endif  // PROTOBUF_CUSTOM_VTABLE
          // @@protoc_insertion_point(serialize_to_array_start:dep2_cfg)
          ::uint32_t cached_has_bits = 0;
          (void)cached_has_bits;

          // uint32 id = 1;
          if (this_._internal_id() != 0) {
            target = stream->EnsureSpace(target);
            target = ::_pbi::WireFormatLite::WriteUInt32ToArray(
                1, this_._internal_id(), target);
          }

          // string level = 2;
          if (!this_._internal_level().empty()) {
            const std::string& _s = this_._internal_level();
            ::google::protobuf::internal::WireFormatLite::VerifyUtf8String(
                _s.data(), static_cast<int>(_s.length()), ::google::protobuf::internal::WireFormatLite::SERIALIZE, "dep2_cfg.level");
            target = stream->WriteStringMaybeAliased(2, _s, target);
          }

          if (PROTOBUF_PREDICT_FALSE(this_._internal_metadata_.have_unknown_fields())) {
            target =
                ::_pbi::WireFormat::InternalSerializeUnknownFieldsToArray(
                    this_._internal_metadata_.unknown_fields<::google::protobuf::UnknownFieldSet>(::google::protobuf::UnknownFieldSet::default_instance), target, stream);
          }
          // @@protoc_insertion_point(serialize_to_array_end:dep2_cfg)
          return target;
        }

#if defined(PROTOBUF_CUSTOM_VTABLE)
        ::size_t dep2_cfg::ByteSizeLong(const MessageLite& base) {
          const dep2_cfg& this_ = static_cast<const dep2_cfg&>(base);
#else   // PROTOBUF_CUSTOM_VTABLE
        ::size_t dep2_cfg::ByteSizeLong() const {
          const dep2_cfg& this_ = *this;
#endif  // PROTOBUF_CUSTOM_VTABLE
          // @@protoc_insertion_point(message_byte_size_start:dep2_cfg)
          ::size_t total_size = 0;

          ::uint32_t cached_has_bits = 0;
          // Prevent compiler warnings about cached_has_bits being unused
          (void)cached_has_bits;

          ::_pbi::Prefetch5LinesFrom7Lines(&this_);
           {
            // string level = 2;
            if (!this_._internal_level().empty()) {
              total_size += 1 + ::google::protobuf::internal::WireFormatLite::StringSize(
                                              this_._internal_level());
            }
            // uint32 id = 1;
            if (this_._internal_id() != 0) {
              total_size += ::_pbi::WireFormatLite::UInt32SizePlusOne(
                  this_._internal_id());
            }
          }
          return this_.MaybeComputeUnknownFieldsSize(total_size,
                                                     &this_._impl_._cached_size_);
        }

void dep2_cfg::MergeImpl(::google::protobuf::MessageLite& to_msg, const ::google::protobuf::MessageLite& from_msg) {
  auto* const _this = static_cast<dep2_cfg*>(&to_msg);
  auto& from = static_cast<const dep2_cfg&>(from_msg);
  // @@protoc_insertion_point(class_specific_merge_from_start:dep2_cfg)
  ABSL_DCHECK_NE(&from, _this);
  ::uint32_t cached_has_bits = 0;
  (void) cached_has_bits;

  if (!from._internal_level().empty()) {
    _this->_internal_set_level(from._internal_level());
  }
  if (from._internal_id() != 0) {
    _this->_impl_.id_ = from._impl_.id_;
  }
  _this->_internal_metadata_.MergeFrom<::google::protobuf::UnknownFieldSet>(from._internal_metadata_);
}

void dep2_cfg::CopyFrom(const dep2_cfg& from) {
// @@protoc_insertion_point(class_specific_copy_from_start:dep2_cfg)
  if (&from == this) return;
  Clear();
  MergeFrom(from);
}


void dep2_cfg::InternalSwap(dep2_cfg* PROTOBUF_RESTRICT other) {
  using std::swap;
  auto* arena = GetArena();
  ABSL_DCHECK_EQ(arena, other->GetArena());
  _internal_metadata_.InternalSwap(&other->_internal_metadata_);
  ::_pbi::ArenaStringPtr::InternalSwap(&_impl_.level_, &other->_impl_.level_, arena);
        swap(_impl_.id_, other->_impl_.id_);
}

::google::protobuf::Metadata dep2_cfg::GetMetadata() const {
  return ::google::protobuf::Message::GetMetadataImpl(GetClassData()->full());
}
// @@protoc_insertion_point(namespace_scope)
namespace google {
namespace protobuf {
}  // namespace protobuf
}  // namespace google
// @@protoc_insertion_point(global_scope)
PROTOBUF_ATTRIBUTE_INIT_PRIORITY2 static ::std::false_type
    _static_init2_ PROTOBUF_UNUSED =
        (::_pbi::AddDescriptors(&descriptor_table_dep_5flevel2_2eproto),
         ::std::false_type{});
#include "google/protobuf/port_undef.inc"
