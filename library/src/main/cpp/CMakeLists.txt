cmake_minimum_required(VERSION 3.22.1)

project("textutils")

# Apply flags BEFORE building the library
set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} -s -fvisibility=hidden")
set(CMAKE_C_FLAGS_RELEASE "${CMAKE_C_FLAGS_RELEASE} -s -fvisibility=hidden")

# Mark JNI functions as default visibility
#set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -DJNIEXPORT=__attribute__((visibility(\"default\")))")
#set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -DJNIEXPORT=__attribute__((visibility(\"default\")))")

add_library(
        textutils
        SHARED
        textutils.cpp
)

find_library(
        log-lib
        log
)

target_link_libraries(
        textutils
        ${log-lib}
)

target_link_options(textutils PRIVATE
        "-Wl,-z,max-page-size=16384"
        "-Wl,-z,common-page-size=16384"
)