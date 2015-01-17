cmake_minimum_required(VERSION 2.8.9)
if ( "${CMAKE_MAJOR_VERSION}.${CMAKE_MINOR_VERSION}" VERSION_GREATER "3.0" )
    cmake_policy(SET CMP0054 NEW)
endif()

project(xresloader_cxx_sample)

########################################################################
find_package(Protobuf REQUIRED)
include_directories(${PROTOBUF_INCLUDE_DIRS})
include_directories(${CMAKE_CURRENT_BINARY_DIR})

########################################################################
file(GLOB SAMPLE_PROTO_FILES "${CMAKE_CURRENT_LIST_DIR}/../*.proto")
protobuf_generate_cpp(${CMAKE_CURRENT_LIST_DIR} ${CMAKE_CURRENT_LIST_DIR} ${SAMPLE_PROTO_FILES})


aux_source_directory(${CMAKE_CURRENT_LIST_DIR} SAMPLE_SRCS)


########################################################################
include_directories("${CMAKE_CURRENT_LIST_DIR}/../../loader-binding/cxx")
aux_source_directory("${CMAKE_CURRENT_LIST_DIR}/../../loader-binding/cxx" PROTO_HEADER_SRCS)


#####################################################################
add_executable(xresloader_cxx_sample ${SAMPLE_SRCS} ${PROTO_HEADER_SRCS})
target_link_libraries(xresloader_cxx_sample ${PROTOBUF_LITE_LIBRARIES})

