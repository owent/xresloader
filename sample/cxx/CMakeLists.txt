cmake_minimum_required(VERSION 2.8.9)
if ( "${CMAKE_MAJOR_VERSION}.${CMAKE_MINOR_VERSION}" VERSION_GREATER "3.0" )
    cmake_policy(SET CMP0054 NEW)
endif()

project(xresloader_cxx_sample)

set(CMAKE_BUILD_TYPE Debug)

########################################################################
find_package(Protobuf REQUIRED)
include_directories(${PROTOBUF_INCLUDE_DIRS})
message(STATUS "protobuf include dir=${PROTOBUF_INCLUDE_DIRS}")
message(STATUS "protobuf-lite libs=${PROTOBUF_LIBRARIES}")

set(CMAKE_C_STANDARD 11)
set(CMAKE_CXX_STANDARD 11)

########################################################################
aux_source_directory(${CMAKE_CURRENT_LIST_DIR} SAMPLE_SRCS)

########################################################################
include_directories("${CMAKE_CURRENT_LIST_DIR}/../../loader-binding/cxx")

########################################################################
set(PROTOBUF_DETECT_TEST_SRC "#include <stdio.h>
#include <google/protobuf/stubs/common.h>

int main () {
#if GOOGLE_PROTOBUF_VERSION < 3000000
    printf(\"2\")\;
#else
    printf(\"3\")\;
#endif
    return 0\; 
}")

file(WRITE "${CMAKE_BINARY_DIR}/try_run_protobuf_test.cpp" ${PROTOBUF_DETECT_TEST_SRC})

try_run(PROTOBUF_DETECT_RUN_RESULT PROTOBUF_DETECT_COMPILE_RESULT
    ${CMAKE_BINARY_DIR} "${CMAKE_BINARY_DIR}/try_run_protobuf_test.cpp"
    COMPILE_OUTPUT_VARIABLE PROTOBUF_DETECT_COMPILE_DYN_MSG
    RUN_OUTPUT_VARIABLE PROTOBUF_DETECT_RUN_OUT
)

if ( "2" STREQUAL "${PROTOBUF_DETECT_RUN_OUT}" )
    message(STATUS "PROTOBUF_DETECT_RUN_OUT=${PROTOBUF_DETECT_RUN_OUT} using proto v2")
    list(APPEND PROTO_HEADER_SRCS "${CMAKE_CURRENT_LIST_DIR}/../../loader-binding/cxx/pb_header.pb.cc")
    aux_source_directory(${CMAKE_CURRENT_LIST_DIR}/v2 PROTO_GEN_SRCS)
    include_directories(${CMAKE_CURRENT_LIST_DIR}/v2)
else()
    message(STATUS "PROTOBUF_DETECT_RUN_OUT=${PROTOBUF_DETECT_RUN_OUT} using proto v3")
    list(APPEND PROTO_HEADER_SRCS "${CMAKE_CURRENT_LIST_DIR}/../../loader-binding/cxx/pb_header_v3.pb.cc")
    aux_source_directory(${CMAKE_CURRENT_LIST_DIR}/v3 PROTO_GEN_SRCS)
    include_directories(${CMAKE_CURRENT_LIST_DIR}/v3)
endif()

#####################################################################
add_executable(xresloader_cxx_sample ${SAMPLE_SRCS} ${PROTO_GEN_SRCS} ${PROTO_HEADER_SRCS})
target_link_libraries(xresloader_cxx_sample ${PROTOBUF_LIBRARIES})

