
#include "fbcon.h"
#include <string.h>
#include <errno.h>
#include <sys/mman.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/mman.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/ipc.h>

#include <sys/resource.h>
#include <sys/syscall.h>

#include <linux/fb.h>
#define LOG_TAG    "JNI/libandroidmultiseat"
#define LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)
#define SMEM_LEN (1280*720*32/8)
#define RGBAtoBGRA(colorByte) ((colorByte & 0xFF000000) | ((colorByte & 0x00FF0000) >> 16) | (colorByte & 0x0000FF00) | ((colorByte & 0x000000FF) << 16))
typedef unsigned int uint32_t;

class JniBitmap{
public:
	uint32_t* _storedBitmapPixels;
	AndroidBitmapInfo _bitmapInfo;
	JniBitmap(){
		_storedBitmapPixels = NULL;
	}

};

JniBitmap* getJniBitmap(JNIEnv*, jobject);
//char* getPath(char*, int);
int fd;
unsigned short *p_mmap;

JniBitmap *jniBitmap;

JNIEXPORT int JNICALL Java_org_secmem_amsserver_FBController_openFrameBuffer
(JNIEnv *env, jobject object, jstring fbNum){

	LOGD("Open File...");
	char *filePath;
	const char *fbNumChar = env->GetStringUTFChars(fbNum, 0);
	filePath = (char*)malloc(strlen("/dev/graphics/fb") + strlen(fbNumChar) + 1);
	strcpy(filePath, "/dev/graphics/fb");
	strcat(filePath, fbNumChar);
	LOGD("FilePath Init : %s", filePath);
	env->ReleaseStringUTFChars(fbNum, fbNumChar);
	LOGD("%s", filePath);
	fd = open(filePath, O_RDWR);
	if(fd == -1)
		LOGD("File Open Error : %d, %s", errno, strerror(errno));
	else
		LOGD("Open Success");

	p_mmap =(unsigned short*)mmap( 0, SMEM_LEN, PROT_WRITE | PROT_READ, MAP_SHARED, fd, 0);

	jniBitmap = new JniBitmap();

	LOGD("%d", fd);
	if( p_mmap == MAP_FAILED)
		LOGD("MAP_FAILED : %d, %s", errno, strerror(errno));
	else
		LOGD("Map Success");

	memset( p_mmap, 0, SMEM_LEN );
	return fd;
}

JNIEXPORT void JNICALL Java_org_secmem_amsserver_FBController_setBitmap
(JNIEnv *env, jobject object, jobject bitmap){

	//JniBitmap *jniBitmap = NULL;

	//unsigned int *imageBuffer;
	//unsigned long i;

	//jniBitmap =

	getJniBitmap(env, bitmap);

	//imageBuffer = (unsigned int*)malloc(SMEM_LEN);


	//LOGD("jniBitmap : Copy to imageBuffer");

	//memcpy( imageBuffer, jniBitmap->_storedBitmapPixels, SMEM_LEN);


	//LOGD("memcpy size = %d byte", SMEM_LEN);

	//	LOGD("Format Convert : RGBA to BGRA");
	/*for(i=0;i<SMEM_LEN/4;i++){
		//LOGD("%d : %4x", i, *(imageBuffer+i));
	 *(imageBuffer+i) = RGBAtoBGRA(*(imageBuffer+i));
	}*/
	//memcpy(image_buffer, jniBitmap->_storedBitmapPixels, sizeof(jniBitmap->_storedBitmapPixels));//sizeof pointer�� ������ 4�� ũ�Ⱑ ��.

	//LOGD("memcpy size = %d byte", SMEM_LEN);

	//memcpy( p_mmap, image_buffer, sizeof(image_buffer));//sizeof pointer
	memcpy( p_mmap, jniBitmap->_storedBitmapPixels, SMEM_LEN);

	delete(jniBitmap->_storedBitmapPixels);

	//memcpy( p_mmap, acdog, sizeof(acdog));

	/*LOGD("Writing...");
	while(1){
		LOGD("Writing...");
		write(fd, buffer, sizeof(buffer));
		sleep(100);
	}*/
}


JNIEXPORT void JNICALL Java_org_secmem_amsserver_FBController_closeFrameBuffer
(JNIEnv *env, jobject object, jstring fbNum){
	close(fd);
}

/*
char* getPath(char* filePath, int fbNumInt){
	char *fbNumChar;
	fbNumChar = itoa(fbNumInt, 10);
	strcat(filePath, fbNumChar);
	return filePath;
}
 */

JniBitmap* getJniBitmap(JNIEnv *env, jobject bitmap){
	AndroidBitmapInfo bitmapInfo;
	uint32_t* storedBitmapPixels = NULL;
	uint32_t* src = NULL;
	void* bitmapPixels;

	//LOGD("reading bitmap info...");
	int ret;
	if ((ret = AndroidBitmap_getInfo(env, bitmap, &bitmapInfo)) < 0){
		LOGE("AndroidBitmap_getInfo() failed ! error=%d", ret);
		return NULL;
	}

	//LOGD("width:%d height:%d stride:%d", bitmapInfo.width, bitmapInfo.height, bitmapInfo.stride);
	if (bitmapInfo.format != ANDROID_BITMAP_FORMAT_RGBA_8888){
		LOGE("Bitmap format is not RGBA_8888!");
		return NULL;
	}

	//LOGD("reading bitmap pixels...");
	if ((ret = AndroidBitmap_lockPixels(env, bitmap, &bitmapPixels)) < 0){
		LOGE("AndroidBitmap_lockPixels() failed ! error=%d", ret);
		return NULL;
	}

	src = (uint32_t*) bitmapPixels;

	storedBitmapPixels = new uint32_t[bitmapInfo.height * bitmapInfo.width];
	int pixelsCount = bitmapInfo.height * bitmapInfo.width;
	memcpy(storedBitmapPixels, src, sizeof(uint32_t) * pixelsCount);
	AndroidBitmap_unlockPixels(
			env, bitmap);

	jniBitmap->_bitmapInfo = bitmapInfo;
	jniBitmap->_storedBitmapPixels = storedBitmapPixels;

	//delete(storedBitmapPixels);

	return jniBitmap;
}
