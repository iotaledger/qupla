#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/mman.h>
#include <unistd.h>

#include "Layout.h"
#include "Qlut.h"


#define LWFPGASLV_OFST 0xFF200000
#define HWFPGASLV_OFST 0xC0000000

#define QLUT_CONFIG_BASE 0x30000
#define QLUT_CONFIG_SPAN 8192
#define QLUT_CONFIG_END 0x31fff

#define QLUT_DATA_BASE 0x34000
#define QLUT_DATA_SPAN 512
#define QLUT_DATA_END 0x341ff

#define QLUT_PROC_BASE 0x36000
#define QLUT_PROC_SPAN 1024
#define QLUT_PROC_END 0x363ff


void qlutClose(Qlut * qlut)
{
  munmap(qlut->config, QLUT_CONFIG_SPAN);
  munmap(qlut->proc,   QLUT_PROC_SPAN);
  munmap(qlut->data,   QLUT_DATA_SPAN);
  close(qlut->fd);
  free(qlut);
}

Qlut * qlutOpen()
{
  int fd = open("/dev/mem", O_RDWR | O_SYNC);
  if (fd < 0)
  {
    return NULL;
  }
  
  Qlut * qlut = (Qlut *) malloc(sizeof(Qlut));
  qlut->fd     = fd;
  qlut->config = mmap(NULL, QLUT_CONFIG_SPAN, PROT_READ | PROT_WRITE, MAP_SHARED, fd, LWFPGASLV_OFST + QLUT_CONFIG_BASE);
  qlut->proc   = mmap(NULL, QLUT_PROC_SPAN,   PROT_READ | PROT_WRITE, MAP_SHARED, fd, LWFPGASLV_OFST + QLUT_PROC_BASE);
  qlut->data   = mmap(NULL, QLUT_DATA_SPAN,   PROT_READ | PROT_WRITE, MAP_SHARED, fd, LWFPGASLV_OFST + QLUT_DATA_BASE);
  return qlut;
}

void qlutProcessData(Qlut * qlut)
{
  //printf("QLUT process\n");
  //printf("QLUT STATUS: %04x\n", qlut->proc[0]);
  qlut->proc[0] = 0x0001;
  //printf("QLUT STATUS: %04x\n", qlut->proc[0]);

}
void qlutReadData(Qlut * qlut, AbraLayout * layout)
{
  //printf("QLUT read output data\n");

  BlockDef * block = &layout->blockDefs[0];
  uint8_t * qlutData = qlut->data - block->nrOfBlockInputTrits;
  for (int i = 0; i < block->nrOfBlockOutputTrits; i++)
  {
    block->outputTrits[i] = qlutData[block->outputIndexes[i]];
  }

//  for (uint16_t j = 0; j < 91; j++)
//  {
//    printf("OUTPUT LUT %d VALUE: %02x", 3*j, qlut->data[3*j]);
//    printf("          OUTPUT LUT %d VALUE: %02x", 3*j+1, qlut->data[3*j+1]);
//    printf("          OUTPUT LUT %d VALUE: %02x\n", 3*j+2, qlut->data[3*j+2]);
//  }
}

void qlutWaitComplete(Qlut * qlut)
{
  //printf("QLUT wait complete\n");

  while ((qlut->proc[0] | 0x0111) != 0x0111)
  {
  }

  //printf("QLUT complete\n");
}

void qlutWaitIdle(Qlut * qlut)
{
  //printf("QLUT wait idle\n");

  while ((qlut->proc[0] | 0x0100) != 0x0100)
  {
  }

  //printf("QLUT idle\n");
}

void qlutWriteConfig(Qlut * qlut, AbraLayout * layout)
{
  printf("QLUT write config\n");

  BlockDef * block = &layout->blockDefs[0];
  qlut->proc[1] = 0x0000;
  qlut->proc[2] = block->nrOfSites;

  for (int i = 0; i < block->nrOfSites; i++)
  {
    // start with merge operation
    uint32_t config0 = 0x00000000;
    uint32_t config1 = 0x00400000;

    SiteDef * site = &block->siteDefs[i];
    if (site->lutIndex != 0xFFFF)
    {
      // not a merge operation, but a normal lookup
      LutDef * lut = &layout->lutDefs[site->lutIndex];
      config0 = (lut->lutValues[1] << 16) | lut->lutValues[0];
      config1 = (lut->lutValues[3] << 16) | lut->lutValues[2];
    }

    qlut->config[i * 2    ] = config0;
    qlut->config[i * 2 + 1] = config1;

    uint16_t * inputIndexes = site->inputIndexes;
    uint16_t indexes[3];
    indexes[0] = site->inputIndexes[0];
    indexes[1] = site->inputIndexes[site->nrOfInputs < 2 ? 0 : 1];
    indexes[2] = site->inputIndexes[site->nrOfInputs < 3 ? 0 : 2];

    uint32_t config2 = 0;
    uint32_t enableBit = 0x00000200;

    printf("%04x indices:", i);
    int reverse = 1;
    for (int k = 0; k < 3; k++)
    {
      uint32_t inputIndex = indexes[reverse ? 2 - k : k];
      if (inputIndex >= block->nrOfBlockInputTrits)
        inputIndex = inputIndex - block->nrOfBlockInputTrits;
      else
        config2 = config2 | enableBit;
      printf(" %04x", inputIndex);
      inputIndex = inputIndex << 10 * k;
      config2 = config2 | inputIndex;
      enableBit = enableBit << 10;
    }

    qlut->config[(i + 512) * 2] = config2;
    printf(", config: %08x %08x %08x\n", config0, config1,config2);
  }
}

void qlutWriteData(Qlut * qlut, uint8_t * data, int length)
{
  //printf("QLUT write input data\n");

  for (int i = 0; i < length; i++)
  {
    qlut->data[i] = data[i];
  }
}