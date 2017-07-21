cmake_minimum_required(VERSION 3.5)
project("JNI Olm" CXX)

find_package(JNI REQUIRED)
include_directories(${JNI_INCLUDE_DIRS})
set(CMAKE_INCLUDE_CURRENT_DIR ON)

set(SOURCES
    olm_account.cpp
    olm_session.cpp
    olm_jni_helper.cpp
    olm_inbound_group_session.cpp
    olm_outbound_group_session.cpp
    olm_utility.cpp
    olm_manager.cpp
)

if(DEFINED OLM_SOURCE)
    include_directories(${OLM_SOURCE}/include)
else()
    message(FATAL_ERROR "OLM_SOURCE not defined, please point to olm project root")
endif()

if(NOT DEFINED OLM_VERSION)
    message(FATAL_ERROR "OLM_VERSION not defined, unable to link library correctly")
endif()

add_custom_target(olm make WORKING_DIRECTORY ${OLM_SOURCE})
add_library(olmjava SHARED ${SOURCES})
add_dependencies(olmjava olm)

target_link_libraries(olmjava ${OLM_SOURCE}/build/libolm.so.${OLM_VERSION})

install(TARGETS olmjava DESTINATION res/)
install(FILES ${OLM_SOURCE}/build/libolm.so.${OLM_VERSION} DESTINATION res/ RENAME libolm.so)
