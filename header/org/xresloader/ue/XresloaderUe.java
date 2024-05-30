// Generated by the protocol buffer compiler.  DO NOT EDIT!
// NO CHECKED-IN PROTOBUF GENCODE
// source: xresloader_ue.proto
// Protobuf Java Version: 4.27.0

package org.xresloader.ue;

public final class XresloaderUe {
  private XresloaderUe() {}
  static {
    com.google.protobuf.RuntimeVersion.validateProtobufGencodeVersion(
      com.google.protobuf.RuntimeVersion.RuntimeDomain.PUBLIC,
      /* major= */ 4,
      /* minor= */ 27,
      /* patch= */ 0,
      /* suffix= */ "",
      XresloaderUe.class.getName());
  }
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistryLite registry) {
    registry.add(org.xresloader.ue.XresloaderUe.keyTag);
    registry.add(org.xresloader.ue.XresloaderUe.ueTypeName);
    registry.add(org.xresloader.ue.XresloaderUe.ueTypeIsClass);
    registry.add(org.xresloader.ue.XresloaderUe.ueOriginTypeName);
    registry.add(org.xresloader.ue.XresloaderUe.ueOriginTypeDefaultValue);
    registry.add(org.xresloader.ue.XresloaderUe.helper);
    registry.add(org.xresloader.ue.XresloaderUe.notDataTable);
    registry.add(org.xresloader.ue.XresloaderUe.defaultLoader);
    registry.add(org.xresloader.ue.XresloaderUe.includeHeader);
  }

  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions(
        (com.google.protobuf.ExtensionRegistryLite) registry);
  }
  /**
   * Protobuf enum {@code org.xresloader.ue.loader_mode}
   */
  public enum loader_mode
      implements com.google.protobuf.ProtocolMessageEnum {
    /**
     * <code>EN_LOADER_MODE_DEFAULT = 0;</code>
     */
    EN_LOADER_MODE_DEFAULT(0),
    /**
     * <code>EN_LOADER_MODE_ENABLE = 1;</code>
     */
    EN_LOADER_MODE_ENABLE(1),
    /**
     * <code>EN_LOADER_MODE_DISABLE = 2;</code>
     */
    EN_LOADER_MODE_DISABLE(2),
    UNRECOGNIZED(-1),
    ;

    static {
      com.google.protobuf.RuntimeVersion.validateProtobufGencodeVersion(
        com.google.protobuf.RuntimeVersion.RuntimeDomain.PUBLIC,
        /* major= */ 4,
        /* minor= */ 27,
        /* patch= */ 0,
        /* suffix= */ "",
        loader_mode.class.getName());
    }
    /**
     * <code>EN_LOADER_MODE_DEFAULT = 0;</code>
     */
    public static final int EN_LOADER_MODE_DEFAULT_VALUE = 0;
    /**
     * <code>EN_LOADER_MODE_ENABLE = 1;</code>
     */
    public static final int EN_LOADER_MODE_ENABLE_VALUE = 1;
    /**
     * <code>EN_LOADER_MODE_DISABLE = 2;</code>
     */
    public static final int EN_LOADER_MODE_DISABLE_VALUE = 2;


    public final int getNumber() {
      if (this == UNRECOGNIZED) {
        throw new java.lang.IllegalArgumentException(
            "Can't get the number of an unknown enum value.");
      }
      return value;
    }

    /**
     * @param value The numeric wire value of the corresponding enum entry.
     * @return The enum associated with the given numeric wire value.
     * @deprecated Use {@link #forNumber(int)} instead.
     */
    @java.lang.Deprecated
    public static loader_mode valueOf(int value) {
      return forNumber(value);
    }

    /**
     * @param value The numeric wire value of the corresponding enum entry.
     * @return The enum associated with the given numeric wire value.
     */
    public static loader_mode forNumber(int value) {
      switch (value) {
        case 0: return EN_LOADER_MODE_DEFAULT;
        case 1: return EN_LOADER_MODE_ENABLE;
        case 2: return EN_LOADER_MODE_DISABLE;
        default: return null;
      }
    }

    public static com.google.protobuf.Internal.EnumLiteMap<loader_mode>
        internalGetValueMap() {
      return internalValueMap;
    }
    private static final com.google.protobuf.Internal.EnumLiteMap<
        loader_mode> internalValueMap =
          new com.google.protobuf.Internal.EnumLiteMap<loader_mode>() {
            public loader_mode findValueByNumber(int number) {
              return loader_mode.forNumber(number);
            }
          };

    public final com.google.protobuf.Descriptors.EnumValueDescriptor
        getValueDescriptor() {
      if (this == UNRECOGNIZED) {
        throw new java.lang.IllegalStateException(
            "Can't get the descriptor of an unrecognized enum value.");
      }
      return getDescriptor().getValues().get(ordinal());
    }
    public final com.google.protobuf.Descriptors.EnumDescriptor
        getDescriptorForType() {
      return getDescriptor();
    }
    public static final com.google.protobuf.Descriptors.EnumDescriptor
        getDescriptor() {
      return org.xresloader.ue.XresloaderUe.getDescriptor().getEnumTypes().get(0);
    }

    private static final loader_mode[] VALUES = values();

    public static loader_mode valueOf(
        com.google.protobuf.Descriptors.EnumValueDescriptor desc) {
      if (desc.getType() != getDescriptor()) {
        throw new java.lang.IllegalArgumentException(
          "EnumValueDescriptor is not for this type.");
      }
      if (desc.getIndex() == -1) {
        return UNRECOGNIZED;
      }
      return VALUES[desc.getIndex()];
    }

    private final int value;

    private loader_mode(int value) {
      this.value = value;
    }

    // @@protoc_insertion_point(enum_scope:org.xresloader.ue.loader_mode)
  }

  public static final int KEY_TAG_FIELD_NUMBER = 1101;
  /**
   * <pre>
   * key字段映射的系数
   * </pre>
   *
   * <code>extend .google.protobuf.FieldOptions { ... }</code>
   */
  public static final
    com.google.protobuf.GeneratedMessage.GeneratedExtension<
      com.google.protobuf.DescriptorProtos.FieldOptions,
      java.lang.Long> keyTag = com.google.protobuf.GeneratedMessage
          .newFileScopedGeneratedExtension(
        java.lang.Long.class,
        null);
  public static final int UE_TYPE_NAME_FIELD_NUMBER = 1102;
  /**
   * <pre>
   * UE内部类型(比如: UTexture, 会生成字段类型为
   * TSoftObjectPtr&lt;UTexture&gt;) 特殊的，当字段类型是Map时，这个插件仅影响Value类型
   * </pre>
   *
   * <code>extend .google.protobuf.FieldOptions { ... }</code>
   */
  public static final
    com.google.protobuf.GeneratedMessage.GeneratedExtension<
      com.google.protobuf.DescriptorProtos.FieldOptions,
      java.lang.String> ueTypeName = com.google.protobuf.GeneratedMessage
          .newFileScopedGeneratedExtension(
        java.lang.String.class,
        null);
  public static final int UE_TYPE_IS_CLASS_FIELD_NUMBER = 1103;
  /**
   * <pre>
   * UE内部类型是否是Class(如果为true，会生成: TSoftClassPtr&lt;T&gt; 而不是
   * TSoftObjectPtr&lt;T&gt;) 特殊的，当字段类型是Map时，这个插件仅影响Value类型
   * </pre>
   *
   * <code>extend .google.protobuf.FieldOptions { ... }</code>
   */
  public static final
    com.google.protobuf.GeneratedMessage.GeneratedExtension<
      com.google.protobuf.DescriptorProtos.FieldOptions,
      java.lang.Boolean> ueTypeIsClass = com.google.protobuf.GeneratedMessage
          .newFileScopedGeneratedExtension(
        java.lang.Boolean.class,
        null);
  public static final int UE_ORIGIN_TYPE_NAME_FIELD_NUMBER = 1104;
  /**
   * <pre>
   * 使用UE内部类型原始类型
   * </pre>
   *
   * <code>extend .google.protobuf.FieldOptions { ... }</code>
   */
  public static final
    com.google.protobuf.GeneratedMessage.GeneratedExtension<
      com.google.protobuf.DescriptorProtos.FieldOptions,
      java.lang.String> ueOriginTypeName = com.google.protobuf.GeneratedMessage
          .newFileScopedGeneratedExtension(
        java.lang.String.class,
        null);
  public static final int UE_ORIGIN_TYPE_DEFAULT_VALUE_FIELD_NUMBER = 1105;
  /**
   * <pre>
   * 使用UE内部类型原始类型时的默认值
   * </pre>
   *
   * <code>extend .google.protobuf.FieldOptions { ... }</code>
   */
  public static final
    com.google.protobuf.GeneratedMessage.GeneratedExtension<
      com.google.protobuf.DescriptorProtos.FieldOptions,
      java.lang.String> ueOriginTypeDefaultValue = com.google.protobuf.GeneratedMessage
          .newFileScopedGeneratedExtension(
        java.lang.String.class,
        null);
  public static final int HELPER_FIELD_NUMBER = 1101;
  /**
   * <pre>
   * 辅助函数的类名
   * </pre>
   *
   * <code>extend .google.protobuf.MessageOptions { ... }</code>
   */
  public static final
    com.google.protobuf.GeneratedMessage.GeneratedExtension<
      com.google.protobuf.DescriptorProtos.MessageOptions,
      java.lang.String> helper = com.google.protobuf.GeneratedMessage
          .newFileScopedGeneratedExtension(
        java.lang.String.class,
        null);
  public static final int NOT_DATA_TABLE_FIELD_NUMBER = 1102;
  /**
   * <pre>
   * 不是DataTable，helper类里不生成加载代码
   * </pre>
   *
   * <code>extend .google.protobuf.MessageOptions { ... }</code>
   */
  public static final
    com.google.protobuf.GeneratedMessage.GeneratedExtension<
      com.google.protobuf.DescriptorProtos.MessageOptions,
      java.lang.Boolean> notDataTable = com.google.protobuf.GeneratedMessage
          .newFileScopedGeneratedExtension(
        java.lang.Boolean.class,
        null);
  public static final int DEFAULT_LOADER_FIELD_NUMBER = 1103;
  /**
   * <pre>
   * 默认Loader模式
   * </pre>
   *
   * <code>extend .google.protobuf.MessageOptions { ... }</code>
   */
  public static final
    com.google.protobuf.GeneratedMessage.GeneratedExtension<
      com.google.protobuf.DescriptorProtos.MessageOptions,
      org.xresloader.ue.XresloaderUe.loader_mode> defaultLoader = com.google.protobuf.GeneratedMessage
          .newFileScopedGeneratedExtension(
        org.xresloader.ue.XresloaderUe.loader_mode.class,
        null);
  public static final int INCLUDE_HEADER_FIELD_NUMBER = 1104;
  /**
   * <pre>
   * 额外的包含头文件
   * </pre>
   *
   * <code>extend .google.protobuf.MessageOptions { ... }</code>
   */
  public static final
    com.google.protobuf.GeneratedMessage.GeneratedExtension<
      com.google.protobuf.DescriptorProtos.MessageOptions,
      java.util.List<java.lang.String>> includeHeader = com.google.protobuf.GeneratedMessage
          .newFileScopedGeneratedExtension(
        java.lang.String.class,
        null);

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static  com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n\023xresloader_ue.proto\022\021org.xresloader.ue" +
      "\032 google/protobuf/descriptor.proto*`\n\013lo" +
      "ader_mode\022\032\n\026EN_LOADER_MODE_DEFAULT\020\000\022\031\n" +
      "\025EN_LOADER_MODE_ENABLE\020\001\022\032\n\026EN_LOADER_MO" +
      "DE_DISABLE\020\002:/\n\007key_tag\022\035.google.protobu" +
      "f.FieldOptions\030\315\010 \001(\003:4\n\014ue_type_name\022\035." +
      "google.protobuf.FieldOptions\030\316\010 \001(\t:8\n\020u" +
      "e_type_is_class\022\035.google.protobuf.FieldO" +
      "ptions\030\317\010 \001(\010:;\n\023ue_origin_type_name\022\035.g" +
      "oogle.protobuf.FieldOptions\030\320\010 \001(\t:D\n\034ue" +
      "_origin_type_default_value\022\035.google.prot" +
      "obuf.FieldOptions\030\321\010 \001(\t:0\n\006helper\022\037.goo" +
      "gle.protobuf.MessageOptions\030\315\010 \001(\t:8\n\016no" +
      "t_data_table\022\037.google.protobuf.MessageOp" +
      "tions\030\316\010 \001(\010:X\n\016default_loader\022\037.google." +
      "protobuf.MessageOptions\030\317\010 \001(\0162\036.org.xre" +
      "sloader.ue.loader_mode:8\n\016include_header" +
      "\022\037.google.protobuf.MessageOptions\030\320\010 \003(\t" +
      "b\006proto3"
    };
    descriptor = com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
          com.google.protobuf.DescriptorProtos.getDescriptor(),
        });
    keyTag.internalInit(descriptor.getExtensions().get(0));
    ueTypeName.internalInit(descriptor.getExtensions().get(1));
    ueTypeIsClass.internalInit(descriptor.getExtensions().get(2));
    ueOriginTypeName.internalInit(descriptor.getExtensions().get(3));
    ueOriginTypeDefaultValue.internalInit(descriptor.getExtensions().get(4));
    helper.internalInit(descriptor.getExtensions().get(5));
    notDataTable.internalInit(descriptor.getExtensions().get(6));
    defaultLoader.internalInit(descriptor.getExtensions().get(7));
    includeHeader.internalInit(descriptor.getExtensions().get(8));
    descriptor.resolveAllFeaturesImmutable();
    com.google.protobuf.DescriptorProtos.getDescriptor();
  }

  // @@protoc_insertion_point(outer_class_scope)
}
