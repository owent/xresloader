// Generated by the protocol buffer compiler.  DO NOT EDIT!
// NO CHECKED-IN PROTOBUF GENCODE
// source: dependency.proto
// Protobuf C++ Version: 5.27.0

#ifndef GOOGLE_PROTOBUF_INCLUDED_dependency_2eproto_2epb_2eh
#define GOOGLE_PROTOBUF_INCLUDED_dependency_2eproto_2epb_2eh

#include <limits>
#include <string>
#include <type_traits>
#include <utility>

#include "google/protobuf/runtime_version.h"
#if PROTOBUF_VERSION != 5027000
#error "Protobuf C++ gencode is built with an incompatible version of"
#error "Protobuf C++ headers/runtime. See"
#error "https://protobuf.dev/support/cross-version-runtime-guarantee/#cpp"
#endif
#include "google/protobuf/io/coded_stream.h"
#include "google/protobuf/arena.h"
#include "google/protobuf/arenastring.h"
#include "google/protobuf/generated_message_tctable_decl.h"
#include "google/protobuf/generated_message_util.h"
#include "google/protobuf/metadata_lite.h"
#include "google/protobuf/generated_message_reflection.h"
#include "google/protobuf/message.h"
#include "google/protobuf/repeated_field.h"  // IWYU pragma: export
#include "google/protobuf/extension_set.h"  // IWYU pragma: export
#include "google/protobuf/generated_enum_reflection.h"
#include "google/protobuf/unknown_field_set.h"
#include "xresloader.pb.h"
#include "dep_level2.pb.h"
// @@protoc_insertion_point(includes)

// Must be included last.
#include "google/protobuf/port_def.inc"

#define PROTOBUF_INTERNAL_EXPORT_dependency_2eproto

namespace google {
namespace protobuf {
namespace internal {
class AnyMetadata;
}  // namespace internal
}  // namespace protobuf
}  // namespace google

// Internal implementation detail -- do not use these members.
struct TableStruct_dependency_2eproto {
  static const ::uint32_t offsets[];
};
extern const ::google::protobuf::internal::DescriptorTable
    descriptor_table_dependency_2eproto;
class dep_cfg;
struct dep_cfgDefaultTypeInternal;
extern dep_cfgDefaultTypeInternal _dep_cfg_default_instance_;
namespace google {
namespace protobuf {
}  // namespace protobuf
}  // namespace google

enum game_const_config : int {
  EN_GCC_PERCENT_BASE = 10000,
  EN_GCC_RANDOM_RANGE_UNIT = 10,
  EN_GCC_RESOURCE_MAX_LIMIT = 9999999,
  EN_GCC_LEVEL_LIMIT = 999,
  EN_GCC_SOLDIER_TYPE_MASK = 100,
  EN_GCC_ACTIVITY_TYPE_MASK = 1000,
  EN_GCC_FORMULAR_TYPE_MASK = 10,
  EN_GCC_SCREEN_WIDTH = 1136,
  EN_GCC_SCREEN_HEIGHT = 640,
  EN_GCC_CAMERA_OFFSET = 268,
};

bool game_const_config_IsValid(int value);
extern const uint32_t game_const_config_internal_data_[];
constexpr game_const_config game_const_config_MIN = static_cast<game_const_config>(10);
constexpr game_const_config game_const_config_MAX = static_cast<game_const_config>(9999999);
constexpr int game_const_config_ARRAYSIZE = 9999999 + 1;
const ::google::protobuf::EnumDescriptor*
game_const_config_descriptor();
template <typename T>
const std::string& game_const_config_Name(T value) {
  static_assert(std::is_same<T, game_const_config>::value ||
                    std::is_integral<T>::value,
                "Incorrect type passed to game_const_config_Name().");
  return ::google::protobuf::internal::NameOfEnum(game_const_config_descriptor(), value);
}
inline bool game_const_config_Parse(absl::string_view name, game_const_config* value) {
  return ::google::protobuf::internal::ParseNamedEnum<game_const_config>(
      game_const_config_descriptor(), name, value);
}
enum cost_type : int {
  EN_CT_UNKNOWN = 0,
  EN_CT_MONEY = 10001,
  EN_CT_DIAMOND = 10101,
};

bool cost_type_IsValid(int value);
extern const uint32_t cost_type_internal_data_[];
constexpr cost_type cost_type_MIN = static_cast<cost_type>(0);
constexpr cost_type cost_type_MAX = static_cast<cost_type>(10101);
constexpr int cost_type_ARRAYSIZE = 10101 + 1;
const ::google::protobuf::EnumDescriptor*
cost_type_descriptor();
template <typename T>
const std::string& cost_type_Name(T value) {
  static_assert(std::is_same<T, cost_type>::value ||
                    std::is_integral<T>::value,
                "Incorrect type passed to cost_type_Name().");
  return ::google::protobuf::internal::NameOfEnum(cost_type_descriptor(), value);
}
inline bool cost_type_Parse(absl::string_view name, cost_type* value) {
  return ::google::protobuf::internal::ParseNamedEnum<cost_type>(
      cost_type_descriptor(), name, value);
}

// ===================================================================


// -------------------------------------------------------------------

class dep_cfg final : public ::google::protobuf::Message
/* @@protoc_insertion_point(class_definition:dep_cfg) */ {
 public:
  inline dep_cfg() : dep_cfg(nullptr) {}
  ~dep_cfg() override;
  template <typename = void>
  explicit PROTOBUF_CONSTEXPR dep_cfg(
      ::google::protobuf::internal::ConstantInitialized);

  inline dep_cfg(const dep_cfg& from) : dep_cfg(nullptr, from) {}
  inline dep_cfg(dep_cfg&& from) noexcept
      : dep_cfg(nullptr, std::move(from)) {}
  inline dep_cfg& operator=(const dep_cfg& from) {
    CopyFrom(from);
    return *this;
  }
  inline dep_cfg& operator=(dep_cfg&& from) noexcept {
    if (this == &from) return *this;
    if (GetArena() == from.GetArena()
#ifdef PROTOBUF_FORCE_COPY_IN_MOVE
        && GetArena() != nullptr
#endif  // !PROTOBUF_FORCE_COPY_IN_MOVE
    ) {
      InternalSwap(&from);
    } else {
      CopyFrom(from);
    }
    return *this;
  }

  inline const ::google::protobuf::UnknownFieldSet& unknown_fields() const
      ABSL_ATTRIBUTE_LIFETIME_BOUND {
    return _internal_metadata_.unknown_fields<::google::protobuf::UnknownFieldSet>(::google::protobuf::UnknownFieldSet::default_instance);
  }
  inline ::google::protobuf::UnknownFieldSet* mutable_unknown_fields()
      ABSL_ATTRIBUTE_LIFETIME_BOUND {
    return _internal_metadata_.mutable_unknown_fields<::google::protobuf::UnknownFieldSet>();
  }

  static const ::google::protobuf::Descriptor* descriptor() {
    return GetDescriptor();
  }
  static const ::google::protobuf::Descriptor* GetDescriptor() {
    return default_instance().GetMetadata().descriptor;
  }
  static const ::google::protobuf::Reflection* GetReflection() {
    return default_instance().GetMetadata().reflection;
  }
  static const dep_cfg& default_instance() {
    return *internal_default_instance();
  }
  static inline const dep_cfg* internal_default_instance() {
    return reinterpret_cast<const dep_cfg*>(
        &_dep_cfg_default_instance_);
  }
  static constexpr int kIndexInFileMessages = 0;
  friend void swap(dep_cfg& a, dep_cfg& b) { a.Swap(&b); }
  inline void Swap(dep_cfg* other) {
    if (other == this) return;
#ifdef PROTOBUF_FORCE_COPY_IN_SWAP
    if (GetArena() != nullptr && GetArena() == other->GetArena()) {
#else   // PROTOBUF_FORCE_COPY_IN_SWAP
    if (GetArena() == other->GetArena()) {
#endif  // !PROTOBUF_FORCE_COPY_IN_SWAP
      InternalSwap(other);
    } else {
      ::google::protobuf::internal::GenericSwap(this, other);
    }
  }
  void UnsafeArenaSwap(dep_cfg* other) {
    if (other == this) return;
    ABSL_DCHECK(GetArena() == other->GetArena());
    InternalSwap(other);
  }

  // implements Message ----------------------------------------------

  dep_cfg* New(::google::protobuf::Arena* arena = nullptr) const final {
    return ::google::protobuf::Message::DefaultConstruct<dep_cfg>(arena);
  }
  using ::google::protobuf::Message::CopyFrom;
  void CopyFrom(const dep_cfg& from);
  using ::google::protobuf::Message::MergeFrom;
  void MergeFrom(const dep_cfg& from) { dep_cfg::MergeImpl(*this, from); }

  private:
  static void MergeImpl(
      ::google::protobuf::MessageLite& to_msg,
      const ::google::protobuf::MessageLite& from_msg);

  public:
  bool IsInitialized() const {
    return true;
  }
  ABSL_ATTRIBUTE_REINITIALIZES void Clear() final;
  ::size_t ByteSizeLong() const final;
  ::uint8_t* _InternalSerialize(
      ::uint8_t* target,
      ::google::protobuf::io::EpsCopyOutputStream* stream) const final;
  int GetCachedSize() const { return _impl_._cached_size_.Get(); }

  private:
  void SharedCtor(::google::protobuf::Arena* arena);
  void SharedDtor();
  void InternalSwap(dep_cfg* other);
 private:
  friend class ::google::protobuf::internal::AnyMetadata;
  static ::absl::string_view FullMessageName() { return "dep_cfg"; }

 protected:
  explicit dep_cfg(::google::protobuf::Arena* arena);
  dep_cfg(::google::protobuf::Arena* arena, const dep_cfg& from);
  dep_cfg(::google::protobuf::Arena* arena, dep_cfg&& from) noexcept
      : dep_cfg(arena) {
    *this = ::std::move(from);
  }
  const ::google::protobuf::Message::ClassData* GetClassData() const final;

 public:
  ::google::protobuf::Metadata GetMetadata() const;
  // nested types ----------------------------------------------------

  // accessors -------------------------------------------------------
  enum : int {
    kNameFieldNumber = 2,
    kDep2FieldNumber = 3,
    kIdFieldNumber = 1,
  };
  // optional string name = 2;
  bool has_name() const;
  void clear_name() ;
  const std::string& name() const;
  template <typename Arg_ = const std::string&, typename... Args_>
  void set_name(Arg_&& arg, Args_... args);
  std::string* mutable_name();
  PROTOBUF_NODISCARD std::string* release_name();
  void set_allocated_name(std::string* value);

  private:
  const std::string& _internal_name() const;
  inline PROTOBUF_ALWAYS_INLINE void _internal_set_name(
      const std::string& value);
  std::string* _internal_mutable_name();

  public:
  // optional .dep2_cfg dep2 = 3;
  bool has_dep2() const;
  void clear_dep2() ;
  const ::dep2_cfg& dep2() const;
  PROTOBUF_NODISCARD ::dep2_cfg* release_dep2();
  ::dep2_cfg* mutable_dep2();
  void set_allocated_dep2(::dep2_cfg* value);
  void unsafe_arena_set_allocated_dep2(::dep2_cfg* value);
  ::dep2_cfg* unsafe_arena_release_dep2();

  private:
  const ::dep2_cfg& _internal_dep2() const;
  ::dep2_cfg* _internal_mutable_dep2();

  public:
  // optional uint32 id = 1;
  bool has_id() const;
  void clear_id() ;
  ::uint32_t id() const;
  void set_id(::uint32_t value);

  private:
  ::uint32_t _internal_id() const;
  void _internal_set_id(::uint32_t value);

  public:
  // @@protoc_insertion_point(class_scope:dep_cfg)
 private:
  class _Internal;
  friend class ::google::protobuf::internal::TcParser;
  static const ::google::protobuf::internal::TcParseTable<
      2, 3, 1,
      20, 2>
      _table_;

  static constexpr const void* _raw_default_instance_ =
      &_dep_cfg_default_instance_;

  friend class ::google::protobuf::MessageLite;
  friend class ::google::protobuf::Arena;
  template <typename T>
  friend class ::google::protobuf::Arena::InternalHelper;
  using InternalArenaConstructable_ = void;
  using DestructorSkippable_ = void;
  struct Impl_ {
    inline explicit constexpr Impl_(
        ::google::protobuf::internal::ConstantInitialized) noexcept;
    inline explicit Impl_(::google::protobuf::internal::InternalVisibility visibility,
                          ::google::protobuf::Arena* arena);
    inline explicit Impl_(::google::protobuf::internal::InternalVisibility visibility,
                          ::google::protobuf::Arena* arena, const Impl_& from,
                          const dep_cfg& from_msg);
    ::google::protobuf::internal::HasBits<1> _has_bits_;
    mutable ::google::protobuf::internal::CachedSize _cached_size_;
    ::google::protobuf::internal::ArenaStringPtr name_;
    ::dep2_cfg* dep2_;
    ::uint32_t id_;
    PROTOBUF_TSAN_DECLARE_MEMBER
  };
  union { Impl_ _impl_; };
  friend struct ::TableStruct_dependency_2eproto;
};

// ===================================================================




// ===================================================================


#ifdef __GNUC__
#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wstrict-aliasing"
#endif  // __GNUC__
// -------------------------------------------------------------------

// dep_cfg

// optional uint32 id = 1;
inline bool dep_cfg::has_id() const {
  bool value = (_impl_._has_bits_[0] & 0x00000004u) != 0;
  return value;
}
inline void dep_cfg::clear_id() {
  ::google::protobuf::internal::TSanWrite(&_impl_);
  _impl_.id_ = 0u;
  _impl_._has_bits_[0] &= ~0x00000004u;
}
inline ::uint32_t dep_cfg::id() const {
  // @@protoc_insertion_point(field_get:dep_cfg.id)
  return _internal_id();
}
inline void dep_cfg::set_id(::uint32_t value) {
  _internal_set_id(value);
  _impl_._has_bits_[0] |= 0x00000004u;
  // @@protoc_insertion_point(field_set:dep_cfg.id)
}
inline ::uint32_t dep_cfg::_internal_id() const {
  ::google::protobuf::internal::TSanRead(&_impl_);
  return _impl_.id_;
}
inline void dep_cfg::_internal_set_id(::uint32_t value) {
  ::google::protobuf::internal::TSanWrite(&_impl_);
  _impl_.id_ = value;
}

// optional string name = 2;
inline bool dep_cfg::has_name() const {
  bool value = (_impl_._has_bits_[0] & 0x00000001u) != 0;
  return value;
}
inline void dep_cfg::clear_name() {
  ::google::protobuf::internal::TSanWrite(&_impl_);
  _impl_.name_.ClearToEmpty();
  _impl_._has_bits_[0] &= ~0x00000001u;
}
inline const std::string& dep_cfg::name() const
    ABSL_ATTRIBUTE_LIFETIME_BOUND {
  // @@protoc_insertion_point(field_get:dep_cfg.name)
  return _internal_name();
}
template <typename Arg_, typename... Args_>
inline PROTOBUF_ALWAYS_INLINE void dep_cfg::set_name(Arg_&& arg,
                                                     Args_... args) {
  ::google::protobuf::internal::TSanWrite(&_impl_);
  _impl_._has_bits_[0] |= 0x00000001u;
  _impl_.name_.Set(static_cast<Arg_&&>(arg), args..., GetArena());
  // @@protoc_insertion_point(field_set:dep_cfg.name)
}
inline std::string* dep_cfg::mutable_name() ABSL_ATTRIBUTE_LIFETIME_BOUND {
  std::string* _s = _internal_mutable_name();
  // @@protoc_insertion_point(field_mutable:dep_cfg.name)
  return _s;
}
inline const std::string& dep_cfg::_internal_name() const {
  ::google::protobuf::internal::TSanRead(&_impl_);
  return _impl_.name_.Get();
}
inline void dep_cfg::_internal_set_name(const std::string& value) {
  ::google::protobuf::internal::TSanWrite(&_impl_);
  _impl_._has_bits_[0] |= 0x00000001u;
  _impl_.name_.Set(value, GetArena());
}
inline std::string* dep_cfg::_internal_mutable_name() {
  ::google::protobuf::internal::TSanWrite(&_impl_);
  _impl_._has_bits_[0] |= 0x00000001u;
  return _impl_.name_.Mutable( GetArena());
}
inline std::string* dep_cfg::release_name() {
  ::google::protobuf::internal::TSanWrite(&_impl_);
  // @@protoc_insertion_point(field_release:dep_cfg.name)
  if ((_impl_._has_bits_[0] & 0x00000001u) == 0) {
    return nullptr;
  }
  _impl_._has_bits_[0] &= ~0x00000001u;
  auto* released = _impl_.name_.Release();
  #ifdef PROTOBUF_FORCE_COPY_DEFAULT_STRING
  _impl_.name_.Set("", GetArena());
  #endif  // PROTOBUF_FORCE_COPY_DEFAULT_STRING
  return released;
}
inline void dep_cfg::set_allocated_name(std::string* value) {
  ::google::protobuf::internal::TSanWrite(&_impl_);
  if (value != nullptr) {
    _impl_._has_bits_[0] |= 0x00000001u;
  } else {
    _impl_._has_bits_[0] &= ~0x00000001u;
  }
  _impl_.name_.SetAllocated(value, GetArena());
  #ifdef PROTOBUF_FORCE_COPY_DEFAULT_STRING
        if (_impl_.name_.IsDefault()) {
          _impl_.name_.Set("", GetArena());
        }
  #endif  // PROTOBUF_FORCE_COPY_DEFAULT_STRING
  // @@protoc_insertion_point(field_set_allocated:dep_cfg.name)
}

// optional .dep2_cfg dep2 = 3;
inline bool dep_cfg::has_dep2() const {
  bool value = (_impl_._has_bits_[0] & 0x00000002u) != 0;
  PROTOBUF_ASSUME(!value || _impl_.dep2_ != nullptr);
  return value;
}
inline const ::dep2_cfg& dep_cfg::_internal_dep2() const {
  ::google::protobuf::internal::TSanRead(&_impl_);
  const ::dep2_cfg* p = _impl_.dep2_;
  return p != nullptr ? *p : reinterpret_cast<const ::dep2_cfg&>(::_dep2_cfg_default_instance_);
}
inline const ::dep2_cfg& dep_cfg::dep2() const ABSL_ATTRIBUTE_LIFETIME_BOUND {
  // @@protoc_insertion_point(field_get:dep_cfg.dep2)
  return _internal_dep2();
}
inline void dep_cfg::unsafe_arena_set_allocated_dep2(::dep2_cfg* value) {
  ::google::protobuf::internal::TSanWrite(&_impl_);
  if (GetArena() == nullptr) {
    delete reinterpret_cast<::google::protobuf::MessageLite*>(_impl_.dep2_);
  }
  _impl_.dep2_ = reinterpret_cast<::dep2_cfg*>(value);
  if (value != nullptr) {
    _impl_._has_bits_[0] |= 0x00000002u;
  } else {
    _impl_._has_bits_[0] &= ~0x00000002u;
  }
  // @@protoc_insertion_point(field_unsafe_arena_set_allocated:dep_cfg.dep2)
}
inline ::dep2_cfg* dep_cfg::release_dep2() {
  ::google::protobuf::internal::TSanWrite(&_impl_);

  _impl_._has_bits_[0] &= ~0x00000002u;
  ::dep2_cfg* released = _impl_.dep2_;
  _impl_.dep2_ = nullptr;
#ifdef PROTOBUF_FORCE_COPY_IN_RELEASE
  auto* old = reinterpret_cast<::google::protobuf::MessageLite*>(released);
  released = ::google::protobuf::internal::DuplicateIfNonNull(released);
  if (GetArena() == nullptr) {
    delete old;
  }
#else   // PROTOBUF_FORCE_COPY_IN_RELEASE
  if (GetArena() != nullptr) {
    released = ::google::protobuf::internal::DuplicateIfNonNull(released);
  }
#endif  // !PROTOBUF_FORCE_COPY_IN_RELEASE
  return released;
}
inline ::dep2_cfg* dep_cfg::unsafe_arena_release_dep2() {
  ::google::protobuf::internal::TSanWrite(&_impl_);
  // @@protoc_insertion_point(field_release:dep_cfg.dep2)

  _impl_._has_bits_[0] &= ~0x00000002u;
  ::dep2_cfg* temp = _impl_.dep2_;
  _impl_.dep2_ = nullptr;
  return temp;
}
inline ::dep2_cfg* dep_cfg::_internal_mutable_dep2() {
  ::google::protobuf::internal::TSanWrite(&_impl_);
  if (_impl_.dep2_ == nullptr) {
    auto* p = ::google::protobuf::Message::DefaultConstruct<::dep2_cfg>(GetArena());
    _impl_.dep2_ = reinterpret_cast<::dep2_cfg*>(p);
  }
  return _impl_.dep2_;
}
inline ::dep2_cfg* dep_cfg::mutable_dep2() ABSL_ATTRIBUTE_LIFETIME_BOUND {
  _impl_._has_bits_[0] |= 0x00000002u;
  ::dep2_cfg* _msg = _internal_mutable_dep2();
  // @@protoc_insertion_point(field_mutable:dep_cfg.dep2)
  return _msg;
}
inline void dep_cfg::set_allocated_dep2(::dep2_cfg* value) {
  ::google::protobuf::Arena* message_arena = GetArena();
  ::google::protobuf::internal::TSanWrite(&_impl_);
  if (message_arena == nullptr) {
    delete reinterpret_cast<::google::protobuf::MessageLite*>(_impl_.dep2_);
  }

  if (value != nullptr) {
    ::google::protobuf::Arena* submessage_arena = reinterpret_cast<::google::protobuf::MessageLite*>(value)->GetArena();
    if (message_arena != submessage_arena) {
      value = ::google::protobuf::internal::GetOwnedMessage(message_arena, value, submessage_arena);
    }
    _impl_._has_bits_[0] |= 0x00000002u;
  } else {
    _impl_._has_bits_[0] &= ~0x00000002u;
  }

  _impl_.dep2_ = reinterpret_cast<::dep2_cfg*>(value);
  // @@protoc_insertion_point(field_set_allocated:dep_cfg.dep2)
}

#ifdef __GNUC__
#pragma GCC diagnostic pop
#endif  // __GNUC__

// @@protoc_insertion_point(namespace_scope)


namespace google {
namespace protobuf {

template <>
struct is_proto_enum<::game_const_config> : std::true_type {};
template <>
inline const EnumDescriptor* GetEnumDescriptor<::game_const_config>() {
  return ::game_const_config_descriptor();
}
template <>
struct is_proto_enum<::cost_type> : std::true_type {};
template <>
inline const EnumDescriptor* GetEnumDescriptor<::cost_type>() {
  return ::cost_type_descriptor();
}

}  // namespace protobuf
}  // namespace google

// @@protoc_insertion_point(global_scope)

#include "google/protobuf/port_undef.inc"

#endif  // GOOGLE_PROTOBUF_INCLUDED_dependency_2eproto_2epb_2eh
