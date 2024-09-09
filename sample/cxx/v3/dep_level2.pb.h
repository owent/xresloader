// Generated by the protocol buffer compiler.  DO NOT EDIT!
// NO CHECKED-IN PROTOBUF GENCODE
// source: dep_level2.proto
// Protobuf C++ Version: 5.28.0

#ifndef GOOGLE_PROTOBUF_INCLUDED_dep_5flevel2_2eproto_2epb_2eh
#define GOOGLE_PROTOBUF_INCLUDED_dep_5flevel2_2eproto_2epb_2eh

#include <limits>
#include <string>
#include <type_traits>
#include <utility>

#include "google/protobuf/runtime_version.h"
#if PROTOBUF_VERSION != 5028000
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
#include "google/protobuf/unknown_field_set.h"
// @@protoc_insertion_point(includes)

// Must be included last.
#include "google/protobuf/port_def.inc"

#define PROTOBUF_INTERNAL_EXPORT_dep_5flevel2_2eproto

namespace google {
namespace protobuf {
namespace internal {
class AnyMetadata;
}  // namespace internal
}  // namespace protobuf
}  // namespace google

// Internal implementation detail -- do not use these members.
struct TableStruct_dep_5flevel2_2eproto {
  static const ::uint32_t offsets[];
};
extern const ::google::protobuf::internal::DescriptorTable
    descriptor_table_dep_5flevel2_2eproto;
class dep2_cfg;
struct dep2_cfgDefaultTypeInternal;
extern dep2_cfgDefaultTypeInternal _dep2_cfg_default_instance_;
namespace google {
namespace protobuf {
}  // namespace protobuf
}  // namespace google


// ===================================================================


// -------------------------------------------------------------------

class dep2_cfg final : public ::google::protobuf::Message
/* @@protoc_insertion_point(class_definition:dep2_cfg) */ {
 public:
  inline dep2_cfg() : dep2_cfg(nullptr) {}
  ~dep2_cfg() PROTOBUF_FINAL;
  template <typename = void>
  explicit PROTOBUF_CONSTEXPR dep2_cfg(
      ::google::protobuf::internal::ConstantInitialized);

  inline dep2_cfg(const dep2_cfg& from) : dep2_cfg(nullptr, from) {}
  inline dep2_cfg(dep2_cfg&& from) noexcept
      : dep2_cfg(nullptr, std::move(from)) {}
  inline dep2_cfg& operator=(const dep2_cfg& from) {
    CopyFrom(from);
    return *this;
  }
  inline dep2_cfg& operator=(dep2_cfg&& from) noexcept {
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
  static const dep2_cfg& default_instance() {
    return *internal_default_instance();
  }
  static inline const dep2_cfg* internal_default_instance() {
    return reinterpret_cast<const dep2_cfg*>(
        &_dep2_cfg_default_instance_);
  }
  static constexpr int kIndexInFileMessages = 0;
  friend void swap(dep2_cfg& a, dep2_cfg& b) { a.Swap(&b); }
  inline void Swap(dep2_cfg* other) {
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
  void UnsafeArenaSwap(dep2_cfg* other) {
    if (other == this) return;
    ABSL_DCHECK(GetArena() == other->GetArena());
    InternalSwap(other);
  }

  // implements Message ----------------------------------------------

  dep2_cfg* New(::google::protobuf::Arena* arena = nullptr) const PROTOBUF_FINAL {
    return ::google::protobuf::Message::DefaultConstruct<dep2_cfg>(arena);
  }
  using ::google::protobuf::Message::CopyFrom;
  void CopyFrom(const dep2_cfg& from);
  using ::google::protobuf::Message::MergeFrom;
  void MergeFrom(const dep2_cfg& from) { dep2_cfg::MergeImpl(*this, from); }

  private:
  static void MergeImpl(
      ::google::protobuf::MessageLite& to_msg,
      const ::google::protobuf::MessageLite& from_msg);

  public:
  bool IsInitialized() const {
    return true;
  }
  ABSL_ATTRIBUTE_REINITIALIZES void Clear() PROTOBUF_FINAL;
  #if defined(PROTOBUF_CUSTOM_VTABLE)
  private:
  static ::size_t ByteSizeLong(const ::google::protobuf::MessageLite& msg);
  static ::uint8_t* _InternalSerialize(
      const MessageLite& msg, ::uint8_t* target,
      ::google::protobuf::io::EpsCopyOutputStream* stream);

  public:
  ::size_t ByteSizeLong() const { return ByteSizeLong(*this); }
  ::uint8_t* _InternalSerialize(
      ::uint8_t* target,
      ::google::protobuf::io::EpsCopyOutputStream* stream) const {
    return _InternalSerialize(*this, target, stream);
  }
  #else   // PROTOBUF_CUSTOM_VTABLE
  ::size_t ByteSizeLong() const final;
  ::uint8_t* _InternalSerialize(
      ::uint8_t* target,
      ::google::protobuf::io::EpsCopyOutputStream* stream) const final;
  #endif  // PROTOBUF_CUSTOM_VTABLE
  int GetCachedSize() const { return _impl_._cached_size_.Get(); }

  private:
  void SharedCtor(::google::protobuf::Arena* arena);
  void SharedDtor();
  void InternalSwap(dep2_cfg* other);
 private:
  friend class ::google::protobuf::internal::AnyMetadata;
  static ::absl::string_view FullMessageName() { return "dep2_cfg"; }

 protected:
  explicit dep2_cfg(::google::protobuf::Arena* arena);
  dep2_cfg(::google::protobuf::Arena* arena, const dep2_cfg& from);
  dep2_cfg(::google::protobuf::Arena* arena, dep2_cfg&& from) noexcept
      : dep2_cfg(arena) {
    *this = ::std::move(from);
  }
  const ::google::protobuf::Message::ClassData* GetClassData() const PROTOBUF_FINAL;
  static const ::google::protobuf::Message::ClassDataFull _class_data_;

 public:
  ::google::protobuf::Metadata GetMetadata() const;
  // nested types ----------------------------------------------------

  // accessors -------------------------------------------------------
  enum : int {
    kLevelFieldNumber = 2,
    kIdFieldNumber = 1,
  };
  // string level = 2;
  void clear_level() ;
  const std::string& level() const;
  template <typename Arg_ = const std::string&, typename... Args_>
  void set_level(Arg_&& arg, Args_... args);
  std::string* mutable_level();
  PROTOBUF_NODISCARD std::string* release_level();
  void set_allocated_level(std::string* value);

  private:
  const std::string& _internal_level() const;
  inline PROTOBUF_ALWAYS_INLINE void _internal_set_level(
      const std::string& value);
  std::string* _internal_mutable_level();

  public:
  // uint32 id = 1;
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
  friend class ::google::protobuf::internal::TcParser;
  static const ::google::protobuf::internal::TcParseTable<
      1, 2, 0,
      22, 2>
      _table_;

  static constexpr const void* _raw_default_instance_ =
      &_dep2_cfg_default_instance_;

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
                          const dep2_cfg& from_msg);
    ::google::protobuf::internal::ArenaStringPtr level_;
    ::uint32_t id_;
    mutable ::google::protobuf::internal::CachedSize _cached_size_;
    PROTOBUF_TSAN_DECLARE_MEMBER
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

// uint32 id = 1;
inline void dep2_cfg::clear_id() {
  ::google::protobuf::internal::TSanWrite(&_impl_);
  _impl_.id_ = 0u;
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
  ::google::protobuf::internal::TSanRead(&_impl_);
  return _impl_.id_;
}
inline void dep2_cfg::_internal_set_id(::uint32_t value) {
  ::google::protobuf::internal::TSanWrite(&_impl_);
  _impl_.id_ = value;
}

// string level = 2;
inline void dep2_cfg::clear_level() {
  ::google::protobuf::internal::TSanWrite(&_impl_);
  _impl_.level_.ClearToEmpty();
}
inline const std::string& dep2_cfg::level() const
    ABSL_ATTRIBUTE_LIFETIME_BOUND {
  // @@protoc_insertion_point(field_get:dep2_cfg.level)
  return _internal_level();
}
template <typename Arg_, typename... Args_>
inline PROTOBUF_ALWAYS_INLINE void dep2_cfg::set_level(Arg_&& arg,
                                                     Args_... args) {
  ::google::protobuf::internal::TSanWrite(&_impl_);
  _impl_.level_.Set(static_cast<Arg_&&>(arg), args..., GetArena());
  // @@protoc_insertion_point(field_set:dep2_cfg.level)
}
inline std::string* dep2_cfg::mutable_level() ABSL_ATTRIBUTE_LIFETIME_BOUND {
  std::string* _s = _internal_mutable_level();
  // @@protoc_insertion_point(field_mutable:dep2_cfg.level)
  return _s;
}
inline const std::string& dep2_cfg::_internal_level() const {
  ::google::protobuf::internal::TSanRead(&_impl_);
  return _impl_.level_.Get();
}
inline void dep2_cfg::_internal_set_level(const std::string& value) {
  ::google::protobuf::internal::TSanWrite(&_impl_);
  _impl_.level_.Set(value, GetArena());
}
inline std::string* dep2_cfg::_internal_mutable_level() {
  ::google::protobuf::internal::TSanWrite(&_impl_);
  return _impl_.level_.Mutable( GetArena());
}
inline std::string* dep2_cfg::release_level() {
  ::google::protobuf::internal::TSanWrite(&_impl_);
  // @@protoc_insertion_point(field_release:dep2_cfg.level)
  return _impl_.level_.Release();
}
inline void dep2_cfg::set_allocated_level(std::string* value) {
  ::google::protobuf::internal::TSanWrite(&_impl_);
  _impl_.level_.SetAllocated(value, GetArena());
  #ifdef PROTOBUF_FORCE_COPY_DEFAULT_STRING
        if (_impl_.level_.IsDefault()) {
          _impl_.level_.Set("", GetArena());
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
