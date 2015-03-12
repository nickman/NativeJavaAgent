#include <jvmti.h>
#include <iostream>
#include <stdlib.h>
#include <stdio.h>
#include <cstring>

 
using namespace std;
 
typedef struct {
 jvmtiEnv *jvmti;
} GlobalAgentData;

typedef struct {
 int tagCount;
 int tagMax;
 jlong* tag;
} TagContext;

 
static GlobalAgentData *gdata;
 
JNIEXPORT jint JNICALL Agent_OnLoad(JavaVM *jvm, char *options, void *reserved)
{
  jvmtiEnv *jvmti = NULL;
  jvmtiCapabilities capa;
  jvmtiError error;
  
  // put a jvmtiEnv instance at jvmti.
  jint result = jvm->GetEnv((void **) &jvmti, JVMTI_VERSION_1_1);
  if (result != JNI_OK) {
    printf("ERROR: Unable to access JVMTI!\n");
  }
  // add a capability to tag objects
  (void)memset(&capa, 0, sizeof(jvmtiCapabilities));
  capa.can_tag_objects = 1;
  capa.can_generate_compiled_method_load_events = 1;
  error = (jvmti)->AddCapabilities(&capa);
 
  // store jvmti in a global data
  gdata = new GlobalAgentData();
  	//(GlobalAgentData*) malloc(sizeof(GlobalAgentData));
  gdata->jvmti = jvmti;
  cout << "Agent Initialized" << endl;

  return JNI_OK;
}




extern "C"
JNICALL jint objectCountingCallback(jlong class_tag, jlong size, jlong* tag_ptr, jint length, void* user_data) {
 int* count = (int*) user_data;
 *count += 1; 
 return JVMTI_VISIT_OBJECTS;
}

// IterateThroughHeap(jvmtiEnv* env,
//             jint heap_filter,
//             jclass klass,
//             const jvmtiHeapCallbacks* callbacks,
//             const void* user_data)
//             
// GetObjectsWithTags(jvmtiEnv* env,
//             jint tag_count,
//             const jlong* tags,
//             jint* count_ptr,
//             jobject** object_result_ptr,
//             jlong** tag_result_ptr)
 
extern "C"
// JNIEXPORT jint JNICALL Java_org_shelajev_Main_countInstances(JNIEnv *env, jclass thisClass, jclass klass) 
JNIEXPORT jint JNICALL Java_com_heliosapm_jvmti_agent_Agent_countInstances(JNIEnv *env, jclass thisClass, jclass klass) {
  int count = 0;
  jvmtiHeapCallbacks callbacks;
  (void)memset(&callbacks, 0, sizeof(callbacks));
  callbacks.heap_iteration_callback = &objectCountingCallback;
  jvmtiError error = gdata->jvmti->IterateThroughHeap(0, klass, &callbacks, &count);
  return count;
}


// private static native Object[] getAllInstances(Class klass);
// 
extern "C"
JNICALL jint objectTaggingCallback(jlong class_tag, jlong size, jlong* tag_ptr, jint length, void* user_data) {
  TagContext* ctx = (TagContext*) user_data; 
  //jlong* tag = (jlong*) user_data;
  ctx->tagCount++;
  //cout << "Tagging. tag:" << tag << ", Size:" << size << ", tagCount:" << ctx->tagCount << endl;
  *tag_ptr = *ctx->tag;
  return JVMTI_VISIT_OBJECTS;
}

extern "C"
JNIEXPORT jobjectArray  JNICALL Java_com_heliosapm_jvmti_agent_Agent_getAllInstances(JNIEnv *env, jclass thisClass, jclass klass, jlong tag, jint max) {
  jvmtiHeapCallbacks callbacks;
  (void)memset(&callbacks, 0, sizeof(callbacks));
  callbacks.heap_iteration_callback = &objectTaggingCallback;  
  //int count = 0;
  TagContext* ctx = new TagContext();
  ctx->tagCount = 3;
  ctx->tagMax = max;
  ctx->tag = &tag;
  cout << "Ctx [tagCount:" << ctx->tagCount << ", tagMax: " << ctx->tagMax << ", tag:" <<  ctx->tag << "]" << endl;
  jvmtiError error = gdata->jvmti->IterateThroughHeap(0, klass, &callbacks, ctx);
  jobject* objArr;
  jlong* tagArr;
  jvmtiError errorGet = gdata->jvmti->GetObjectsWithTags(1, &tag, &ctx->tagCount, &objArr, &tagArr);
  cout << "Ctx [tagCount:" << ctx->tagCount << ", tagMax: " << ctx->tagMax << ", tag:" <<  ctx->tag << "]" << endl;
  jobjectArray ret = env->NewObjectArray(ctx->tagCount, klass, NULL);
  for (int n=0; n<ctx->tagCount; n++) {
    env->SetObjectArrayElement(ret, n, objArr[n]);
  } 
  return ret; 
}


