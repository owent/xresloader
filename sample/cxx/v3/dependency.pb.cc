// Generated by the protocol buffer compiler.  DO NOT EDIT!
// NO CHECKED-IN PROTOBUF GENCODE
// source: dependency.proto
// Protobuf C++ Version: 5.28.2

#include "dependency.pb.h"

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

inline constexpr dep_cfg::Impl_::Impl_(
    ::_pbi::ConstantInitialized) noexcept
      : _cached_size_{0},
        name_(
            &::google::protobuf::internal::fixed_address_empty_string,
            ::_pbi::ConstantInitialized()),
        dep2_{nullptr},
        id_{0u} {}

template <typename>
PROTOBUF_CONSTEXPR dep_cfg::dep_cfg(::_pbi::ConstantInitialized)
#if defined(PROTOBUF_CUSTOM_VTABLE)
    : ::google::protobuf::Message(_class_data_.base()),
#else   // PROTOBUF_CUSTOM_VTABLE
    : ::google::protobuf::Message(),
#endif  // PROTOBUF_CUSTOM_VTABLE
      _impl_(::_pbi::ConstantInitialized()) {
}
struct dep_cfgDefaultTypeInternal {
  PROTOBUF_CONSTEXPR dep_cfgDefaultTypeInternal() : _instance(::_pbi::ConstantInitialized{}) {}
  ~dep_cfgDefaultTypeInternal() {}
  union {
    dep_cfg _instance;
  };
};

PROTOBUF_ATTRIBUTE_NO_DESTROY PROTOBUF_CONSTINIT
    PROTOBUF_ATTRIBUTE_INIT_PRIORITY1 dep_cfgDefaultTypeInternal _dep_cfg_default_instance_;
static const ::_pb::EnumDescriptor* file_level_enum_descriptors_dependency_2eproto[2];
static constexpr const ::_pb::ServiceDescriptor**
    file_level_service_descriptors_dependency_2eproto = nullptr;
const ::uint32_t
    TableStruct_dependency_2eproto::offsets[] ABSL_ATTRIBUTE_SECTION_VARIABLE(
        protodesc_cold) = {
        PROTOBUF_FIELD_OFFSET(::dep_cfg, _impl_._has_bits_),
        PROTOBUF_FIELD_OFFSET(::dep_cfg, _internal_metadata_),
        ~0u,  // no _extensions_
        ~0u,  // no _oneof_case_
        ~0u,  // no _weak_field_map_
        ~0u,  // no _inlined_string_donated_
        ~0u,  // no _split_
        ~0u,  // no sizeof(Split)
        PROTOBUF_FIELD_OFFSET(::dep_cfg, _impl_.id_),
        PROTOBUF_FIELD_OFFSET(::dep_cfg, _impl_.name_),
        PROTOBUF_FIELD_OFFSET(::dep_cfg, _impl_.dep2_),
        ~0u,
        ~0u,
        0,
};

static const ::_pbi::MigrationSchema
    schemas[] ABSL_ATTRIBUTE_SECTION_VARIABLE(protodesc_cold) = {
        {0, 11, -1, sizeof(::dep_cfg)},
};
static const ::_pb::Message* const file_default_instances[] = {
    &::_dep_cfg_default_instance_._instance,
};
const char descriptor_table_protodef_dependency_2eproto[] ABSL_ATTRIBUTE_SECTION_VARIABLE(
    protodesc_cold) = {
    "\n\020dependency.proto\032\020xresloader.proto\032\020de"
    "p_level2.proto\"<\n\007dep_cfg\022\n\n\002id\030\001 \001(\r\022\014\n"
    "\004name\030\002 \001(\t\022\027\n\004dep2\030\003 \001(\0132\t.dep2_cfg*\313\002\n"
    "\021game_const_config\022\022\n\016EN_GCC_UNKNOWN\020\000\022\030"
    "\n\023EN_GCC_PERCENT_BASE\020\220N\022\034\n\030EN_GCC_RANDO"
    "M_RANGE_UNIT\020\n\022 \n\031EN_GCC_RESOURCE_MAX_LI"
    "MIT\020\377\254\342\004\022\027\n\022EN_GCC_LEVEL_LIMIT\020\347\007\022\034\n\030EN_"
    "GCC_SOLDIER_TYPE_MASK\020d\022\036\n\031EN_GCC_ACTIVI"
    "TY_TYPE_MASK\020\350\007\022\035\n\031EN_GCC_FORMULAR_TYPE_"
    "MASK\020\n\022\030\n\023EN_GCC_SCREEN_WIDTH\020\360\010\022\031\n\024EN_G"
    "CC_SCREEN_HEIGHT\020\200\005\022\031\n\024EN_GCC_CAMERA_OFF"
    "SET\020\214\002\032\002\020\001*Z\n\tcost_type\022\021\n\rEN_CT_UNKNOWN"
    "\020\000\022\033\n\013EN_CT_MONEY\020\221N\032\t\322>\006\351\207\221\345\270\201\022\035\n\rEN_CT"
    "_DIAMOND\020\365N\032\t\322>\006\351\222\273\347\237\263b\006proto3"
};
static const ::_pbi::DescriptorTable* const descriptor_table_dependency_2eproto_deps[2] =
    {
        &::descriptor_table_dep_5flevel2_2eproto,
        &::descriptor_table_xresloader_2eproto,
};
static ::absl::once_flag descriptor_table_dependency_2eproto_once;
PROTOBUF_CONSTINIT const ::_pbi::DescriptorTable descriptor_table_dependency_2eproto = {
    false,
    false,
    550,
    descriptor_table_protodef_dependency_2eproto,
    "dependency.proto",
    &descriptor_table_dependency_2eproto_once,
    descriptor_table_dependency_2eproto_deps,
    2,
    1,
    schemas,
    file_default_instances,
    TableStruct_dependency_2eproto::offsets,
    file_level_enum_descriptors_dependency_2eproto,
    file_level_service_descriptors_dependency_2eproto,
};
const ::google::protobuf::EnumDescriptor* game_const_config_descriptor() {
  ::google::protobuf::internal::AssignDescriptors(&descriptor_table_dependency_2eproto);
  return file_level_enum_descriptors_dependency_2eproto[0];
}
PROTOBUF_CONSTINIT const uint32_t game_const_config_internal_data_[] = {
    65536u, 524320u, 512u, 1000u, 640u, 10000u, 268u, 999u, 1136u, 9999999u, 100u, };
bool game_const_config_IsValid(int value) {
  return ::_pbi::ValidateEnum(value, game_const_config_internal_data_);
}
const ::google::protobuf::EnumDescriptor* cost_type_descriptor() {
  ::google::protobuf::internal::AssignDescriptors(&descriptor_table_dependency_2eproto);
  return file_level_enum_descriptors_dependency_2eproto[1];
}
PROTOBUF_CONSTINIT const uint32_t cost_type_internal_data_[] = {
    65536u, 131072u, 10101u, 10001u, };
bool cost_type_IsValid(int value) {
  return ::_pbi::ValidateEnum(value, cost_type_internal_data_);
}
// ===================================================================

class dep_cfg::_Internal {
 public:
  using HasBits =
      decltype(std::declval<dep_cfg>()._impl_._has_bits_);
  static constexpr ::int32_t kHasBitsOffset =
      8 * PROTOBUF_FIELD_OFFSET(dep_cfg, _impl_._has_bits_);
};

void dep_cfg::clear_dep2() {
  ::google::protobuf::internal::TSanWrite(&_impl_);
  if (_impl_.dep2_ != nullptr) _impl_.dep2_->Clear();
  _impl_._has_bits_[0] &= ~0x00000001u;
}
dep_cfg::dep_cfg(::google::protobuf::Arena* arena)
#if defined(PROTOBUF_CUSTOM_VTABLE)
    : ::google::protobuf::Message(arena, _class_data_.base()) {
#else   // PROTOBUF_CUSTOM_VTABLE
    : ::google::protobuf::Message(arena) {
#endif  // PROTOBUF_CUSTOM_VTABLE
  SharedCtor(arena);
  // @@protoc_insertion_point(arena_constructor:dep_cfg)
}
inline PROTOBUF_NDEBUG_INLINE dep_cfg::Impl_::Impl_(
    ::google::protobuf::internal::InternalVisibility visibility, ::google::protobuf::Arena* arena,
    const Impl_& from, const ::dep_cfg& from_msg)
      : _has_bits_{from._has_bits_},
        _cached_size_{0},
        name_(arena, from.name_) {}

dep_cfg::dep_cfg(
    ::google::protobuf::Arena* arena,
    const dep_cfg& from)
#if defined(PROTOBUF_CUSTOM_VTABLE)
    : ::google::protobuf::Message(arena, _class_data_.base()) {
#else   // PROTOBUF_CUSTOM_VTABLE
    : ::google::protobuf::Message(arena) {
#endif  // PROTOBUF_CUSTOM_VTABLE
  dep_cfg* const _this = this;
  (void)_this;
  _internal_metadata_.MergeFrom<::google::protobuf::UnknownFieldSet>(
      from._internal_metadata_);
  new (&_impl_) Impl_(internal_visibility(), arena, from._impl_, from);
  ::uint32_t cached_has_bits = _impl_._has_bits_[0];
  _impl_.dep2_ = (cached_has_bits & 0x00000001u) ? ::google::protobuf::Message::CopyConstruct<::dep2_cfg>(
                              arena, *from._impl_.dep2_)
                        : nullptr;
  _impl_.id_ = from._impl_.id_;

  // @@protoc_insertion_point(copy_constructor:dep_cfg)
}
inline PROTOBUF_NDEBUG_INLINE dep_cfg::Impl_::Impl_(
    ::google::protobuf::internal::InternalVisibility visibility,
    ::google::protobuf::Arena* arena)
      : _cached_size_{0},
        name_(arena) {}

inline void dep_cfg::SharedCtor(::_pb::Arena* arena) {
  new (&_impl_) Impl_(internal_visibility(), arena);
  ::memset(reinterpret_cast<char *>(&_impl_) +
               offsetof(Impl_, dep2_),
           0,
           offsetof(Impl_, id_) -
               offsetof(Impl_, dep2_) +
               sizeof(Impl_::id_));
}
dep_cfg::~dep_cfg() {
  // @@protoc_insertion_point(destructor:dep_cfg)
  _internal_metadata_.Delete<::google::protobuf::UnknownFieldSet>();
  SharedDtor();
}
inline void dep_cfg::SharedDtor() {
  ABSL_DCHECK(GetArena() == nullptr);
  _impl_.name_.Destroy();
  delete _impl_.dep2_;
  _impl_.~Impl_();
}

PROTOBUF_CONSTINIT
PROTOBUF_ATTRIBUTE_INIT_PRIORITY1
const ::google::protobuf::MessageLite::ClassDataFull
    dep_cfg::_class_data_ = {
        ::google::protobuf::Message::ClassData{
            &_dep_cfg_default_instance_._instance,
            &_table_.header,
            nullptr,  // OnDemandRegisterArenaDtor
            nullptr,  // IsInitialized
            &dep_cfg::MergeImpl,
#if defined(PROTOBUF_CUSTOM_VTABLE)
            ::google::protobuf::Message::GetDeleteImpl<dep_cfg>(),
            ::google::protobuf::Message::GetNewImpl<dep_cfg>(),
            ::google::protobuf::Message::GetClearImpl<dep_cfg>(), &dep_cfg::ByteSizeLong,
                &dep_cfg::_InternalSerialize,
#endif  // PROTOBUF_CUSTOM_VTABLE
            PROTOBUF_FIELD_OFFSET(dep_cfg, _impl_._cached_size_),
            false,
        },
        &dep_cfg::kDescriptorMethods,
        &descriptor_table_dependency_2eproto,
        nullptr,  // tracker
};
const ::google::protobuf::MessageLite::ClassData* dep_cfg::GetClassData() const {
  ::google::protobuf::internal::PrefetchToLocalCache(&_class_data_);
  ::google::protobuf::internal::PrefetchToLocalCache(_class_data_.tc_table);
  return _class_data_.base();
}
PROTOBUF_CONSTINIT PROTOBUF_ATTRIBUTE_INIT_PRIORITY1
const ::_pbi::TcParseTable<2, 3, 1, 20, 2> dep_cfg::_table_ = {
  {
    PROTOBUF_FIELD_OFFSET(dep_cfg, _impl_._has_bits_),
    0, // no _extensions_
    3, 24,  // max_field_number, fast_idx_mask
    offsetof(decltype(_table_), field_lookup_table),
    4294967288,  // skipmap
    offsetof(decltype(_table_), field_entries),
    3,  // num_field_entries
    1,  // num_aux_entries
    offsetof(decltype(_table_), aux_entries),
    _class_data_.base(),
    nullptr,  // post_loop_handler
    ::_pbi::TcParser::GenericFallback,  // fallback
    #ifdef PROTOBUF_PREFETCH_PARSE_TABLE
    ::_pbi::TcParser::GetTable<::dep_cfg>(),  // to_prefetch
    #endif  // PROTOBUF_PREFETCH_PARSE_TABLE
  }, {{
    {::_pbi::TcParser::MiniParse, {}},
    // uint32 id = 1;
    {::_pbi::TcParser::SingularVarintNoZag1<::uint32_t, offsetof(dep_cfg, _impl_.id_), 63>(),
     {8, 63, 0, PROTOBUF_FIELD_OFFSET(dep_cfg, _impl_.id_)}},
    // string name = 2;
    {::_pbi::TcParser::FastUS1,
     {18, 63, 0, PROTOBUF_FIELD_OFFSET(dep_cfg, _impl_.name_)}},
    // .dep2_cfg dep2 = 3;
    {::_pbi::TcParser::FastMtS1,
     {26, 0, 0, PROTOBUF_FIELD_OFFSET(dep_cfg, _impl_.dep2_)}},
  }}, {{
    65535, 65535
  }}, {{
    // uint32 id = 1;
    {PROTOBUF_FIELD_OFFSET(dep_cfg, _impl_.id_), -1, 0,
    (0 | ::_fl::kFcSingular | ::_fl::kUInt32)},
    // string name = 2;
    {PROTOBUF_FIELD_OFFSET(dep_cfg, _impl_.name_), -1, 0,
    (0 | ::_fl::kFcSingular | ::_fl::kUtf8String | ::_fl::kRepAString)},
    // .dep2_cfg dep2 = 3;
    {PROTOBUF_FIELD_OFFSET(dep_cfg, _impl_.dep2_), _Internal::kHasBitsOffset + 0, 0,
    (0 | ::_fl::kFcOptional | ::_fl::kMessage | ::_fl::kTvTable)},
  }}, {{
    {::_pbi::TcParser::GetTable<::dep2_cfg>()},
  }}, {{
    "\7\0\4\0\0\0\0\0"
    "dep_cfg"
    "name"
  }},
};

PROTOBUF_NOINLINE void dep_cfg::Clear() {
// @@protoc_insertion_point(message_clear_start:dep_cfg)
  ::google::protobuf::internal::TSanWrite(&_impl_);
  ::uint32_t cached_has_bits = 0;
  // Prevent compiler warnings about cached_has_bits being unused
  (void) cached_has_bits;

  _impl_.name_.ClearToEmpty();
  cached_has_bits = _impl_._has_bits_[0];
  if (cached_has_bits & 0x00000001u) {
    ABSL_DCHECK(_impl_.dep2_ != nullptr);
    _impl_.dep2_->Clear();
  }
  _impl_.id_ = 0u;
  _impl_._has_bits_.Clear();
  _internal_metadata_.Clear<::google::protobuf::UnknownFieldSet>();
}

#if defined(PROTOBUF_CUSTOM_VTABLE)
        ::uint8_t* dep_cfg::_InternalSerialize(
            const MessageLite& base, ::uint8_t* target,
            ::google::protobuf::io::EpsCopyOutputStream* stream) {
          const dep_cfg& this_ = static_cast<const dep_cfg&>(base);
#else   // PROTOBUF_CUSTOM_VTABLE
        ::uint8_t* dep_cfg::_InternalSerialize(
            ::uint8_t* target,
            ::google::protobuf::io::EpsCopyOutputStream* stream) const {
          const dep_cfg& this_ = *this;
#endif  // PROTOBUF_CUSTOM_VTABLE
          // @@protoc_insertion_point(serialize_to_array_start:dep_cfg)
          ::uint32_t cached_has_bits = 0;
          (void)cached_has_bits;

          // uint32 id = 1;
          if (this_._internal_id() != 0) {
            target = stream->EnsureSpace(target);
            target = ::_pbi::WireFormatLite::WriteUInt32ToArray(
                1, this_._internal_id(), target);
          }

          // string name = 2;
          if (!this_._internal_name().empty()) {
            const std::string& _s = this_._internal_name();
            ::google::protobuf::internal::WireFormatLite::VerifyUtf8String(
                _s.data(), static_cast<int>(_s.length()), ::google::protobuf::internal::WireFormatLite::SERIALIZE, "dep_cfg.name");
            target = stream->WriteStringMaybeAliased(2, _s, target);
          }

          cached_has_bits = this_._impl_._has_bits_[0];
          // .dep2_cfg dep2 = 3;
          if (cached_has_bits & 0x00000001u) {
            target = ::google::protobuf::internal::WireFormatLite::InternalWriteMessage(
                3, *this_._impl_.dep2_, this_._impl_.dep2_->GetCachedSize(), target,
                stream);
          }

          if (PROTOBUF_PREDICT_FALSE(this_._internal_metadata_.have_unknown_fields())) {
            target =
                ::_pbi::WireFormat::InternalSerializeUnknownFieldsToArray(
                    this_._internal_metadata_.unknown_fields<::google::protobuf::UnknownFieldSet>(::google::protobuf::UnknownFieldSet::default_instance), target, stream);
          }
          // @@protoc_insertion_point(serialize_to_array_end:dep_cfg)
          return target;
        }

#if defined(PROTOBUF_CUSTOM_VTABLE)
        ::size_t dep_cfg::ByteSizeLong(const MessageLite& base) {
          const dep_cfg& this_ = static_cast<const dep_cfg&>(base);
#else   // PROTOBUF_CUSTOM_VTABLE
        ::size_t dep_cfg::ByteSizeLong() const {
          const dep_cfg& this_ = *this;
#endif  // PROTOBUF_CUSTOM_VTABLE
          // @@protoc_insertion_point(message_byte_size_start:dep_cfg)
          ::size_t total_size = 0;

          ::uint32_t cached_has_bits = 0;
          // Prevent compiler warnings about cached_has_bits being unused
          (void)cached_has_bits;

          ::_pbi::Prefetch5LinesFrom7Lines(&this_);
           {
            // string name = 2;
            if (!this_._internal_name().empty()) {
              total_size += 1 + ::google::protobuf::internal::WireFormatLite::StringSize(
                                              this_._internal_name());
            }
          }
           {
            // .dep2_cfg dep2 = 3;
            cached_has_bits =
                this_._impl_._has_bits_[0];
            if (cached_has_bits & 0x00000001u) {
              total_size += 1 +
                            ::google::protobuf::internal::WireFormatLite::MessageSize(*this_._impl_.dep2_);
            }
          }
           {
            // uint32 id = 1;
            if (this_._internal_id() != 0) {
              total_size += ::_pbi::WireFormatLite::UInt32SizePlusOne(
                  this_._internal_id());
            }
          }
          return this_.MaybeComputeUnknownFieldsSize(total_size,
                                                     &this_._impl_._cached_size_);
        }

void dep_cfg::MergeImpl(::google::protobuf::MessageLite& to_msg, const ::google::protobuf::MessageLite& from_msg) {
  auto* const _this = static_cast<dep_cfg*>(&to_msg);
  auto& from = static_cast<const dep_cfg&>(from_msg);
  ::google::protobuf::Arena* arena = _this->GetArena();
  // @@protoc_insertion_point(class_specific_merge_from_start:dep_cfg)
  ABSL_DCHECK_NE(&from, _this);
  ::uint32_t cached_has_bits = 0;
  (void) cached_has_bits;

  if (!from._internal_name().empty()) {
    _this->_internal_set_name(from._internal_name());
  }
  cached_has_bits = from._impl_._has_bits_[0];
  if (cached_has_bits & 0x00000001u) {
    ABSL_DCHECK(from._impl_.dep2_ != nullptr);
    if (_this->_impl_.dep2_ == nullptr) {
      _this->_impl_.dep2_ =
          ::google::protobuf::Message::CopyConstruct<::dep2_cfg>(arena, *from._impl_.dep2_);
    } else {
      _this->_impl_.dep2_->MergeFrom(*from._impl_.dep2_);
    }
  }
  if (from._internal_id() != 0) {
    _this->_impl_.id_ = from._impl_.id_;
  }
  _this->_impl_._has_bits_[0] |= cached_has_bits;
  _this->_internal_metadata_.MergeFrom<::google::protobuf::UnknownFieldSet>(from._internal_metadata_);
}

void dep_cfg::CopyFrom(const dep_cfg& from) {
// @@protoc_insertion_point(class_specific_copy_from_start:dep_cfg)
  if (&from == this) return;
  Clear();
  MergeFrom(from);
}


void dep_cfg::InternalSwap(dep_cfg* PROTOBUF_RESTRICT other) {
  using std::swap;
  auto* arena = GetArena();
  ABSL_DCHECK_EQ(arena, other->GetArena());
  _internal_metadata_.InternalSwap(&other->_internal_metadata_);
  swap(_impl_._has_bits_[0], other->_impl_._has_bits_[0]);
  ::_pbi::ArenaStringPtr::InternalSwap(&_impl_.name_, &other->_impl_.name_, arena);
  ::google::protobuf::internal::memswap<
      PROTOBUF_FIELD_OFFSET(dep_cfg, _impl_.id_)
      + sizeof(dep_cfg::_impl_.id_)
      - PROTOBUF_FIELD_OFFSET(dep_cfg, _impl_.dep2_)>(
          reinterpret_cast<char*>(&_impl_.dep2_),
          reinterpret_cast<char*>(&other->_impl_.dep2_));
}

::google::protobuf::Metadata dep_cfg::GetMetadata() const {
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
        (::_pbi::AddDescriptors(&descriptor_table_dependency_2eproto),
         ::std::false_type{});
#include "google/protobuf/port_undef.inc"
