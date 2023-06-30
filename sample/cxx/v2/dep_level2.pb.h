// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: dep_level2.proto

#ifndef GOOGLE_PROTOBUF_INCLUDED_dep_5flevel2_2eproto_2epb_2eh
#define GOOGLE_PROTOBUF_INCLUDED_dep_5flevel2_2eproto_2epb_2eh

#include <limits>
#include <string>
#include <type_traits>

#include "google/protobuf/port_def.inc"
#if PROTOBUF_VERSION < 4023000
#error "This file was generated by a newer version of protoc which is"
#error "incompatible with your Protocol Buffer headers. Please update"
#error "your headers."
#endif  // PROTOBUF_VERSION

#if 4023003 < PROTOBUF_MIN_PROTOC_VERSION
#error "This file was generated by an older version of protoc which is"
#error "incompatible with your Protocol Buffer headers. Please"
#error "regenerate this file with a newer version of protoc."
#endif  // PROTOBUF_MIN_PROTOC_VERSION
#include "google/protobuf/port_undef.inc"
#include "google/protobuf/io/coded_stream.h"
#include "google/protobuf/arena.h"
#include "google/protobuf/arenastring.h"
#include "google/protobuf/generated_message_util.h"
#include "google/protobuf/metadata_lite.h"
#include "google/protobuf/generated_message_reflection.h"
#include "google/protobuf/message.h"
#include "google/protobuf/repeated_field.h"  // IWYU pragma: export
#include "google/protobuf/extension_set.h"  // IWYU pragma: export
#include "google/protobuf/unknown_field_set.h"
// @@protoc_insertion_point(includes)

// Must be included last.
#include "google/protobuf/port_def.inc"

#define PROTOBUF_INTERNAL_EXPORT_dep_5flevel2_2eproto

PROTOBUF_NAMESPACE_OPEN
namespace internal {
class AnyMetadata;
}  // namespace internal
PROTOBUF_NAMESPACE_CLOSE

// Internal implementation detail -- do not use these members.
struct TableStruct_dep_5flevel2_2eproto {
  static const ::uint32_t offsets[];
};
extern const ::PROTOBUF_NAMESPACE_ID::internal::DescriptorTable
    descriptor_table_dep_5flevel2_2eproto;
class dep2_cfg;
struct dep2_cfgDefaultTypeInternal;
extern dep2_cfgDefaultTypeInternal _dep2_cfg_default_instance_;
PROTOBUF_NAMESPACE_OPEN
template <>
::dep2_cfg* Arena::CreateMaybeMessage<::dep2_cfg>(Arena*);
PROTOBUF_NAMESPACE_CLOSE


// ===================================================================


// -------------------------------------------------------------------

class dep2_cfg final :
    public ::PROTOBUF_NAMESPACE_ID::Message /* @@protoc_insertion_point(class_definition:dep2_cfg) */ {
 public:
  inline dep2_cfg() : dep2_cfg(nullptr) {}
  ~dep2_cfg() override;
  template<typename = void>
  explicit PROTOBUF_CONSTEXPR dep2_cfg(::PROTOBUF_NAMESPACE_ID::internal::ConstantInitialized);

  dep2_cfg(const dep2_cfg& from);
  dep2_cfg(dep2_cfg&& from) noexcept
    : dep2_cfg() {
    *this = ::std::move(from);
  }

  inline dep2_cfg& operator=(const dep2_cfg& from) {
    CopyFrom(from);
    return *this;
  }
  inline dep2_cfg& operator=(dep2_cfg&& from) noexcept {
    if (this == &from) return *this;
    if (GetOwningArena() == from.GetOwningArena()
  #ifdef PROTOBUF_FORCE_COPY_IN_MOVE
        && GetOwningArena() != nullptr
  #endif  // !PROTOBUF_FORCE_COPY_IN_MOVE
    ) {
      InternalSwap(&from);
    } else {
      CopyFrom(from);
    }
    return *this;
  }

  inline const ::PROTOBUF_NAMESPACE_ID::UnknownFieldSet& unknown_fields() const {
    return _internal_metadata_.unknown_fields<::PROTOBUF_NAMESPACE_ID::UnknownFieldSet>(::PROTOBUF_NAMESPACE_ID::UnknownFieldSet::default_instance);
  }
  inline ::PROTOBUF_NAMESPACE_ID::UnknownFieldSet* mutable_unknown_fields() {
    return _internal_metadata_.mutable_unknown_fields<::PROTOBUF_NAMESPACE_ID::UnknownFieldSet>();
  }

  static const ::PROTOBUF_NAMESPACE_ID::Descriptor* descriptor() {
    return GetDescriptor();
  }
  static const ::PROTOBUF_NAMESPACE_ID::Descriptor* GetDescriptor() {
    return default_instance().GetMetadata().descriptor;
  }
  static const ::PROTOBUF_NAMESPACE_ID::Reflection* GetReflection() {
    return default_instance().GetMetadata().reflection;
  }
  static const dep2_cfg& default_instance() {
    return *internal_default_instance();
  }
  static inline const dep2_cfg* internal_default_instance() {
    return reinterpret_cast<const dep2_cfg*>(
               &_dep2_cfg_default_instance_);
  }
  static constexpr int kIndexInFileMessages =
    0;

  friend void swap(dep2_cfg& a, dep2_cfg& b) {
    a.Swap(&b);
  }
  inline void Swap(dep2_cfg* other) {
    if (other == this) return;
  #ifdef PROTOBUF_FORCE_COPY_IN_SWAP
    if (GetOwningArena() != nullptr &&
        GetOwningArena() == other->GetOwningArena()) {
   #else  // PROTOBUF_FORCE_COPY_IN_SWAP
    if (GetOwningArena() == other->GetOwningArena()) {
  #endif  // !PROTOBUF_FORCE_COPY_IN_SWAP
      InternalSwap(other);
    } else {
      ::PROTOBUF_NAMESPACE_ID::internal::GenericSwap(this, other);
    }
  }
  void UnsafeArenaSwap(dep2_cfg* other) {
    if (other == this) return;
    ABSL_DCHECK(GetOwningArena() == other->GetOwningArena());
    InternalSwap(other);
  }

  // implements Message ----------------------------------------------

  dep2_cfg* New(::PROTOBUF_NAMESPACE_ID::Arena* arena = nullptr) const final {
    return CreateMaybeMessage<dep2_cfg>(arena);
  }
  using ::PROTOBUF_NAMESPACE_ID::Message::CopyFrom;
  void CopyFrom(const dep2_cfg& from);
  using ::PROTOBUF_NAMESPACE_ID::Message::MergeFrom;
  void MergeFrom( const dep2_cfg& from) {
    dep2_cfg::MergeImpl(*this, from);
  }
  private:
  static void MergeImpl(::PROTOBUF_NAMESPACE_ID::Message& to_msg, const ::PROTOBUF_NAMESPACE_ID::Message& from_msg);
  public:
  PROTOBUF_ATTRIBUTE_REINITIALIZES void Clear() final;
  bool IsInitialized() const final;

  ::size_t ByteSizeLong() const final;
  const char* _InternalParse(const char* ptr, ::PROTOBUF_NAMESPACE_ID::internal::ParseContext* ctx) final;
  ::uint8_t* _InternalSerialize(
      ::uint8_t* target, ::PROTOBUF_NAMESPACE_ID::io::EpsCopyOutputStream* stream) const final;
  int GetCachedSize() const final { return _impl_._cached_size_.Get(); }

  private:
  void SharedCtor(::PROTOBUF_NAMESPACE_ID::Arena* arena);
  void SharedDtor();
  void SetCachedSize(int size) const final;
  void InternalSwap(dep2_cfg* other);

  private:
  friend class ::PROTOBUF_NAMESPACE_ID::internal::AnyMetadata;
  static ::absl::string_view FullMessageName() {
    return "dep2_cfg";
  }
  protected:
  explicit dep2_cfg(::PROTOBUF_NAMESPACE_ID::Arena* arena);
  public:

  static const ClassData _class_data_;
  const ::PROTOBUF_NAMESPACE_ID::Message::ClassData*GetClassData() const final;

  ::PROTOBUF_NAMESPACE_ID::Metadata GetMetadata() const final;

  // nested types ----------------------------------------------------

  // accessors -------------------------------------------------------

  enum : int {
    kLevelFieldNumber = 2,
    kIdFieldNumber = 1,
  };
  // optional string level = 2;
  bool has_level() const;
  void clear_level() ;
  const std::string& level() const;




  template <typename Arg_ = const std::string&, typename... Args_>
  void set_level(Arg_&& arg, Args_... args);
  std::string* mutable_level();
  PROTOBUF_NODISCARD std::string* release_level();
  void set_allocated_level(std::string* ptr);

  private:
  const std::string& _internal_level() const;
  inline PROTOBUF_ALWAYS_INLINE void _internal_set_level(
      const std::string& value);
  std::string* _internal_mutable_level();

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
  // @@protoc_insertion_point(class_scope:dep2_cfg)
 private:
  class _Internal;

  template <typename T> friend class ::PROTOBUF_NAMESPACE_ID::Arena::InternalHelper;
  typedef void InternalArenaConstructable_;
  typedef void DestructorSkippable_;
  struct Impl_ {
    ::PROTOBUF_NAMESPACE_ID::internal::HasBits<1> _has_bits_;
    mutable ::PROTOBUF_NAMESPACE_ID::internal::CachedSize _cached_size_;
    ::PROTOBUF_NAMESPACE_ID::internal::ArenaStringPtr level_;
    ::uint32_t id_;
  };
  union { Impl_ _impl_; };
  friend struct ::TableStruct_dep_5flevel2_2eproto;
};

// ===================================================================




// ===================================================================


#ifdef __GNUC__
#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wstrict-aliasing"
#endif  // __GNUC__
// -------------------------------------------------------------------

// dep2_cfg

// optional uint32 id = 1;
inline bool dep2_cfg::has_id() const {
  bool value = (_impl_._has_bits_[0] & 0x00000002u) != 0;
  return value;
}
inline void dep2_cfg::clear_id() {
  _impl_.id_ = 0u;
  _impl_._has_bits_[0] &= ~0x00000002u;
}
inline ::uint32_t dep2_cfg::id() const {
  // @@protoc_insertion_point(field_get:dep2_cfg.id)
  return _internal_id();
}
inline void dep2_cfg::set_id(::uint32_t value) {
  _internal_set_id(value);
  // @@protoc_insertion_point(field_set:dep2_cfg.id)
}
inline ::uint32_t dep2_cfg::_internal_id() const {
  return _impl_.id_;
}
inline void dep2_cfg::_internal_set_id(::uint32_t value) {
  _impl_._has_bits_[0] |= 0x00000002u;
  _impl_.id_ = value;
}

// optional string level = 2;
inline bool dep2_cfg::has_level() const {
  bool value = (_impl_._has_bits_[0] & 0x00000001u) != 0;
  return value;
}
inline void dep2_cfg::clear_level() {
  _impl_.level_.ClearToEmpty();
  _impl_._has_bits_[0] &= ~0x00000001u;
}
inline const std::string& dep2_cfg::level() const {
  // @@protoc_insertion_point(field_get:dep2_cfg.level)
  return _internal_level();
}
template <typename Arg_, typename... Args_>
inline PROTOBUF_ALWAYS_INLINE void dep2_cfg::set_level(Arg_&& arg,
                                                     Args_... args) {
  _impl_._has_bits_[0] |= 0x00000001u;
  _impl_.level_.Set(static_cast<Arg_&&>(arg), args..., GetArenaForAllocation());
  // @@protoc_insertion_point(field_set:dep2_cfg.level)
}
inline std::string* dep2_cfg::mutable_level() {
  std::string* _s = _internal_mutable_level();
  // @@protoc_insertion_point(field_mutable:dep2_cfg.level)
  return _s;
}
inline const std::string& dep2_cfg::_internal_level() const {
  return _impl_.level_.Get();
}
inline void dep2_cfg::_internal_set_level(const std::string& value) {
  _impl_._has_bits_[0] |= 0x00000001u;


  _impl_.level_.Set(value, GetArenaForAllocation());
}
inline std::string* dep2_cfg::_internal_mutable_level() {
  _impl_._has_bits_[0] |= 0x00000001u;
  return _impl_.level_.Mutable( GetArenaForAllocation());
}
inline std::string* dep2_cfg::release_level() {
  // @@protoc_insertion_point(field_release:dep2_cfg.level)
  if ((_impl_._has_bits_[0] & 0x00000001u) == 0) {
    return nullptr;
  }
  _impl_._has_bits_[0] &= ~0x00000001u;
  auto* released = _impl_.level_.Release();
  #ifdef PROTOBUF_FORCE_COPY_DEFAULT_STRING
  _impl_.level_.Set("", GetArenaForAllocation());
  #endif  // PROTOBUF_FORCE_COPY_DEFAULT_STRING
  return released;
}
inline void dep2_cfg::set_allocated_level(std::string* value) {
  if (value != nullptr) {
    _impl_._has_bits_[0] |= 0x00000001u;
  } else {
    _impl_._has_bits_[0] &= ~0x00000001u;
  }
  _impl_.level_.SetAllocated(value, GetArenaForAllocation());
  #ifdef PROTOBUF_FORCE_COPY_DEFAULT_STRING
        if (_impl_.level_.IsDefault()) {
          _impl_.level_.Set("", GetArenaForAllocation());
        }
  #endif  // PROTOBUF_FORCE_COPY_DEFAULT_STRING
  // @@protoc_insertion_point(field_set_allocated:dep2_cfg.level)
}

#ifdef __GNUC__
#pragma GCC diagnostic pop
#endif  // __GNUC__

// @@protoc_insertion_point(namespace_scope)


// @@protoc_insertion_point(global_scope)

#include "google/protobuf/port_undef.inc"

#endif  // GOOGLE_PROTOBUF_INCLUDED_dep_5flevel2_2eproto_2epb_2eh
