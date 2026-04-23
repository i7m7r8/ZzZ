cmake_minimum_required(VERSION 3.22.1)
project("zzz_core")

add_library(zzz_core SHARED IMPORTED)

set_target_properties(zzz_core PROPERTIES
    IMPORTED_LOCATION
    "${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libzzz_core.so"
)
