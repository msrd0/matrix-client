/*
 * Copyright 2016 OpenMarket Ltd
 * Copyright 2016 Vector Creations Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef _OMLJNI_H
#define _OMLJNI_H

#include <cstdlib>
#include <cstdio>
#include <cstring>
#include <sstream>
#include <jni.h>

#define TAG "OlmJniNative"

/* logging macros */
//#define ENABLE_JNI_LOG

#ifdef NDK_DEBUG
    #warning NDK_DEBUG is defined!
#endif

#ifdef ENABLE_JNI_LOG
    #warning ENABLE_JNI_LOG is defined!
#endif

#define LOGE(...)
#define LOGD(...)
#define LOGW(...)


#define FUNC_DEF(class_name,func_name) JNICALL Java_org_matrix_olm_##class_name##_##func_name

namespace AndroidOlmSdk
{

}


#ifdef __cplusplus
extern "C" {
#endif

// internal helper functions
bool setRandomInBuffer(JNIEnv *env, uint8_t **aBuffer2Ptr, size_t aRandomSize);

struct OlmSession* getSessionInstanceId(JNIEnv* aJniEnv, jobject aJavaObject);
struct OlmAccount* getAccountInstanceId(JNIEnv* aJniEnv, jobject aJavaObject);
struct OlmInboundGroupSession* getInboundGroupSessionInstanceId(JNIEnv* aJniEnv, jobject aJavaObject);
struct OlmOutboundGroupSession* getOutboundGroupSessionInstanceId(JNIEnv* aJniEnv, jobject aJavaObject);
struct OlmUtility* getUtilityInstanceId(JNIEnv* aJniEnv, jobject aJavaObject);

#ifdef __cplusplus
}
#endif


#endif
