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
static bool onLoad;

extern "C"
JNIEXPORT jboolean JNICALL Java_com_heliosapm_jvmti_agent_NativeAgent_wasLoaded0(JNIEnv *env, jclass thisClass) {
  return onLoad;
}


JNIEXPORT jint JNICALL Agent_OnAttach(JavaVM* jvm, char *options, void *reserved) {
  cout << "Initializing Agent OnAttach..." << endl;
  onLoad = false;
  jvmtiEnv *jvmti = NULL;
  jvmtiCapabilities capa;
  //jvmtiError error;
  
  // put a jvmtiEnv instance at jvmti.
  jint result = jvm->GetEnv((void **) &jvmti, JVMTI_VERSION_1_2);
  if (result != JNI_OK) {
    printf("ERROR: Unable to access JVMTI!\n");
  }
  // add a capability to tag objects
  (void)memset(&capa, 0, sizeof(jvmtiCapabilities));
  capa.can_tag_objects = 1;
  capa.can_generate_compiled_method_load_events = 1;
  (jvmti)->AddCapabilities(&capa);
 
  // store jvmti in a global data
  gdata = new GlobalAgentData();
    //(GlobalAgentData*) malloc(sizeof(GlobalAgentData));
  gdata->jvmti = jvmti;
  cout << "Agent Initialized" << endl;

  return JNI_OK;

}
 
JNIEXPORT jint JNICALL Agent_OnLoad(JavaVM *jvm, char *options, void *reserved) {
  cout << "Initializing Agent OnLoad..." << endl;
  onLoad = true;
  jvmtiEnv *jvmti = NULL;
  jvmtiCapabilities capa;
  //jvmtiError error;
  
  // put a jvmtiEnv instance at jvmti.
  jint result = jvm->GetEnv((void **) &jvmti, JVMTI_VERSION_1_2);
  if (result != JNI_OK) {
    printf("ERROR: Unable to access JVMTI!\n");
  }
  // add a capability to tag objects
  (void)memset(&capa, 0, sizeof(jvmtiCapabilities));
  capa.can_tag_objects = 1;
  capa.can_generate_compiled_method_load_events = 1;
  (jvmti)->AddCapabilities(&capa);
 
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

extern "C"
JNICALL jvmtiIterationControl typeInstanceCountingCallback(jlong class_tag, jlong size, jlong* tag_ptr, void* user_data) {
    // *tag_ptr = 1;
    // return JVMTI_ITERATION_CONTINUE;

  TagContext* ctx = (TagContext*) user_data; 
  //printf("typeInstanceCountingCallback %d objects with tag\n", ctx->tagCount);
  if(ctx->tagMax!=0 && ctx->tagCount >= ctx->tagMax) {
    cout << "Aborting Instance Tagging after " << ctx->tagCount << " Instances" << endl;
    return JVMTI_ITERATION_ABORT;
  }
  ctx->tagCount++;
  *tag_ptr = *ctx->tag;
  return JVMTI_ITERATION_CONTINUE;
}

 
extern "C"
JNIEXPORT jint JNICALL Java_com_heliosapm_jvmti_agent_NativeAgent_countExactInstances0(JNIEnv *env, jclass thisClass, jclass klass) {
  int count = 0;
  jvmtiHeapCallbacks callbacks;
  (void)memset(&callbacks, 0, sizeof(callbacks));
  callbacks.heap_iteration_callback = &objectCountingCallback;
  //jvmtiError error = 
  gdata->jvmti->IterateThroughHeap(0, klass, &callbacks, &count);
  return count;
}


extern "C"
JNICALL jint objectTaggingCallback(jlong class_tag, jlong size, jlong* tag_ptr, jint length, void* user_data) {
  TagContext* ctx = (TagContext*) user_data; 
  if(ctx->tagMax!=0 && ctx->tagCount >= ctx->tagMax) {
    cout << "Aborting Instance Tagging after " << ctx->tagCount << " Instances" << endl;
    return JVMTI_VISIT_ABORT;
  }
  ctx->tagCount++;
  *tag_ptr = *ctx->tag;
  return JVMTI_VISIT_OBJECTS;
}



extern "C"
JNIEXPORT jobjectArray  JNICALL Java_com_heliosapm_jvmti_agent_NativeAgent_getExactInstances0(JNIEnv *env, jclass thisClass, jclass klass, jlong tag, jint max) {
  jvmtiHeapCallbacks callbacks;
  (void)memset(&callbacks, 0, sizeof(callbacks));
  callbacks.heap_iteration_callback = &objectTaggingCallback;  
  TagContext* ctx = new TagContext();
  ctx->tagCount = 0;
  ctx->tagMax = max;
  ctx->tag = &tag;
  //jvmtiError error = 
  gdata->jvmti->IterateThroughHeap(0, klass, &callbacks, ctx);
  jobject* objArr;
  jlong* tagArr;
  //jvmtiError errorGet = 
  gdata->jvmti->GetObjectsWithTags(1, &tag, &ctx->tagCount, &objArr, &tagArr);
  jobjectArray ret = env->NewObjectArray(ctx->tagCount, klass, NULL);
  for (int n=0; n<ctx->tagCount; n++) {
    env->SetObjectArrayElement(ret, n, objArr[n]);
  }
  gdata->jvmti->Deallocate((unsigned char*)objArr);
  gdata->jvmti->Deallocate((unsigned char*)tagArr);
  return ret; 
}


extern "C"
JNIEXPORT int  JNICALL Java_com_heliosapm_jvmti_agent_NativeAgent_countInstances0(JNIEnv* env, jclass ignored, jclass targetClass, jlong tg, jint max) {

    TagContext* ctx = new TagContext();
    ctx->tagCount = 0;
    ctx->tagMax = max;
    ctx->tag = &tg;


    jvmtiCapabilities capabilities = {0};
    capabilities.can_tag_objects = 1;
    gdata->jvmti->AddCapabilities(&capabilities);

    gdata->jvmti->IterateOverInstancesOfClass(targetClass, JVMTI_HEAP_OBJECT_EITHER, &typeInstanceCountingCallback, ctx);

    return ctx->tagCount;
}


extern "C"
JNIEXPORT jobjectArray  JNICALL Java_com_heliosapm_jvmti_agent_NativeAgent_getInstances0(JNIEnv* env, jclass ignored, jclass targetClass, jlong tg, jint max) {
    TagContext* ctx = new TagContext();
    ctx->tagCount = 0;
    ctx->tagMax = max;
    ctx->tag = &tg;


    jvmtiCapabilities capabilities = {0};
    capabilities.can_tag_objects = 1;
    gdata->jvmti->AddCapabilities(&capabilities);

    gdata->jvmti->IterateOverInstancesOfClass(targetClass, JVMTI_HEAP_OBJECT_EITHER, &typeInstanceCountingCallback, ctx);
  
    jobject* objArr;
    jlong* tagArr;
    //jvmtiError errorGet = 
    gdata->jvmti->GetObjectsWithTags(1, &tg, &ctx->tagCount, &objArr, &tagArr);
    jobjectArray ret = env->NewObjectArray(ctx->tagCount, targetClass, NULL);
    for (int n=0; n<ctx->tagCount; n++) {
      env->SetObjectArrayElement(ret, n, objArr[n]);
    }
    gdata->jvmti->Deallocate((unsigned char*)objArr);
    gdata->jvmti->Deallocate((unsigned char*)tagArr);
    return ret; 


}


