#ifndef H_QLUT
#define H_QLUT

#include "Layout.h"


typedef struct
{
  int        fd;
  uint32_t * config;
  uint16_t * proc;
  uint8_t  * data;
} Qlut;

extern void qlutClose(Qlut * qlut);
extern Qlut * qlutOpen();
extern void qlutProcessData(Qlut * qlut);
extern void qlutReadData(Qlut * qlut, AbraLayout * layout);
extern void qlutWaitComplete(Qlut * qlut);
extern void qlutWaitIdle(Qlut * qlut);
extern void qlutWriteConfig(Qlut * qlut, AbraLayout * layout);
extern void qlutWriteData(Qlut * qlut, uint8_t * data, int length);

#endif
