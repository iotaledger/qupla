#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "Layout.h"


static uint8_t * readBytes(uint8_t *src, void *dest, uint16_t size);


//--------------------------------------------------------------------------------------------------------------------------------------------------------------

void printLayout(AbraLayout *layout)
{
  printf("\nNumber of Luts: %d", layout->nrOfLuts);
  for (uint16_t i = 0; i < layout->nrOfLuts; i++)
    printf("\nLut Definition #%d: %04x%04x%04x%04x", i, layout->lutDefs[i].lutValues[3], layout->lutDefs[i].lutValues[2], layout->lutDefs[i].lutValues[1], layout->lutDefs[i].lutValues[0]);
  printf("\n\nNumber of Block Definitions: %d", layout->nrOfBlocks);
  for (uint16_t i = 0; i < layout->nrOfBlocks; i++)
  {
    printf("\n\nBlock Definition #%d", i);
    printf("\n\tNumber of block input trits: %d", layout->blockDefs[i].nrOfBlockInputTrits);
    printf("\n\tNumber of sites: %d", layout->blockDefs[i].nrOfSites);
    for (uint16_t j = 0; j < layout->blockDefs[i].nrOfSites; j++)
    {
      printf("\n\tSite Definition #%d", j);
      printf("\n\t\tLut index: #%04x", layout->blockDefs[i].siteDefs[j].lutIndex);
      printf("\n\t\tNumber of inputs: %04x", layout->blockDefs[i].siteDefs[j].nrOfInputs);
      for (uint16_t k = 0; k < layout->blockDefs[i].siteDefs[j].nrOfInputs; k++)
        printf("\n\t\t\tInput index %d: %04x", k, layout->blockDefs[i].siteDefs[j].inputIndexes[k]);
    }
    printf("\n\tNumber of block output trits: %04x", layout->blockDefs[i].nrOfBlockOutputTrits);
    for (uint16_t n = 0; n < layout->blockDefs[i].nrOfBlockOutputTrits; n++)
      printf("\n\t\t\tOutput index %d: %04x", n, layout->blockDefs[i].outputIndexes[n]);
  }
  printf("\n");
}

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

AbraLayout * readLayout(uint8_t *addr)
{
  AbraLayout * layout = (AbraLayout *) malloc(sizeof(AbraLayout));
  addr = readBytes(addr, &layout->nrOfLuts, sizeof(uint16_t));
  layout->lutDefs = malloc(sizeof(LutDef) * layout->nrOfLuts);
  addr = readBytes(addr, layout->lutDefs, layout->nrOfLuts * sizeof(LutDef));
  addr = readBytes(addr, &layout->nrOfBlocks, sizeof(uint16_t));
  layout->blockDefs = malloc(sizeof(BlockDef) * layout->nrOfBlocks);
  for (uint16_t i = 0; i < layout->nrOfBlocks; i++)
  {
    BlockDef *block = &layout->blockDefs[i];
    addr = readBytes(addr, &block->nrOfBlockInputTrits, sizeof(uint16_t));
    addr = readBytes(addr, &block->nrOfSites, sizeof(uint16_t));
    block->siteDefs = malloc(sizeof(SiteDef) * block->nrOfSites);
    for (uint16_t j = 0; j < block->nrOfSites; j++)
    {
      SiteDef *site = &block->siteDefs[j];
      addr = readBytes(addr, &site->lutIndex, sizeof(uint16_t));
      addr = readBytes(addr, &site->nrOfInputs, sizeof(uint16_t));
      site->inputIndexes = malloc(sizeof(uint16_t) * site->nrOfInputs);
      addr = readBytes(addr, site->inputIndexes, sizeof(uint16_t) * site->nrOfInputs);
    }
    addr = readBytes(addr, &block->nrOfBlockOutputTrits, sizeof(uint16_t));
    block->outputIndexes = malloc(sizeof(uint16_t) * block->nrOfBlockOutputTrits);
    addr = readBytes(addr, block->outputIndexes, sizeof(uint16_t) * block->nrOfBlockOutputTrits);
    block->outputTrits = malloc(sizeof(uint8_t) * block->nrOfBlockOutputTrits);
  }
  printf("\nConfiguration data copied successfully!\n");
  return layout;
}

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

void saveLayout(AbraLayout *layout, char filename[31])
{
  FILE * textFile = fopen(strncat(filename, ".txt", 4), "w");
  fprintf(textFile, "Number of Luts: %d", layout->nrOfLuts);
  for (uint16_t i = 0; i < layout->nrOfLuts; i++)
    fprintf(textFile, "\nLut Definition #%d: %04x%04x%04x%04x", i, layout->lutDefs[i].lutValues[3], layout->lutDefs[i].lutValues[2], layout->lutDefs[i].lutValues[1], layout->lutDefs[i].lutValues[0]);
  fprintf(textFile, "\n\nNumber of Block Definitions: %d", layout->nrOfBlocks);
  for (uint16_t i = 0; i < layout->nrOfBlocks; i++)
  {
    fprintf(textFile, "\n\nBlock Definition #%d", i);
    fprintf(textFile, "\n\tNumber of block input trits: %d", layout->blockDefs[i].nrOfBlockInputTrits);
    fprintf(textFile, "\n\tNumber of sites: %d", layout->blockDefs[i].nrOfSites);
    for (uint16_t j = 0; j < layout->blockDefs[i].nrOfSites; j++)
    {
      fprintf(textFile, "\n\tSite Definition #%d", j);
      fprintf(textFile, "\n\t\tLut index: #%04x", layout->blockDefs[i].siteDefs[j].lutIndex);
      fprintf(textFile, "\n\t\tNumber of inputs: %04x", layout->blockDefs[i].siteDefs[j].nrOfInputs);
      for (uint16_t k = 0; k < layout->blockDefs[i].siteDefs[j].nrOfInputs; k++)
        fprintf(textFile, "\n\t\t\tInput index %d: %04x", k, layout->blockDefs[i].siteDefs[j].inputIndexes[k]);
    }
    fprintf(textFile, "\n\tNumber of block output trits: %04x", layout->blockDefs[i].nrOfBlockOutputTrits);
    for (uint16_t n = 0; n < layout->blockDefs[i].nrOfBlockOutputTrits; n++)
      fprintf(textFile, "\n\t\t\tOutput index %d: %04x", n, layout->blockDefs[i].outputIndexes[n]);
  }
  fclose(textFile);
  printf("\nConfiguration data saved to text file.\n");
}

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

bool verifyLayout(uint8_t * addr, int u8fileSize)
{
  AbraLayout tempLayout;

  if (u8fileSize < sizeof(uint16_t))
    return false;
  addr = readBytes(addr, &tempLayout.nrOfLuts, sizeof(uint16_t));
  u8fileSize -= sizeof(uint16_t);

  if (u8fileSize < tempLayout.nrOfLuts * sizeof(LutDef))
    return false;
  addr += tempLayout.nrOfLuts * sizeof(LutDef); //skip LUT data
  u8fileSize -= tempLayout.nrOfLuts * sizeof(LutDef);

  if (u8fileSize < sizeof(uint16_t))
    return false;
  addr = readBytes(addr, &tempLayout.nrOfBlocks, sizeof(uint16_t));
  u8fileSize -= sizeof(uint16_t);

  for (int i = 0; i < tempLayout.nrOfBlocks; i++)
  {
    BlockDef block;

    if (u8fileSize < sizeof(uint16_t))
      return false;
    addr = readBytes(addr, &block.nrOfBlockInputTrits, sizeof(uint16_t));
    u8fileSize -= sizeof(uint16_t);

    if (u8fileSize < sizeof(uint16_t))
      return false;
    addr = readBytes(addr, &block.nrOfSites, sizeof(uint16_t));
    u8fileSize -= sizeof(uint16_t);

    uint16_t totalSites = block.nrOfBlockInputTrits + block.nrOfSites;

    for (int j = 0; j < block.nrOfSites; j++)
    {
      SiteDef site;

      if (u8fileSize < sizeof(uint16_t))
        return false;
      addr = readBytes(addr, &site.lutIndex, sizeof(uint16_t));
      u8fileSize -= sizeof(uint16_t);

      if ((site.lutIndex >= tempLayout.nrOfLuts) && (site.lutIndex != 0xFFFF))
        return false;

      if (u8fileSize < sizeof(uint16_t))
        return false;
      addr = readBytes(addr, &site.nrOfInputs, sizeof(uint16_t));
      u8fileSize -= sizeof(uint16_t);

      for (uint16_t k = 0; k < site.nrOfInputs; k++)
      {
        uint16_t inputIndex;
        if (u8fileSize < sizeof(uint16_t))
          return false;
        addr = readBytes(addr, &inputIndex, sizeof(uint16_t));
        u8fileSize -= sizeof(uint16_t);

        if (inputIndex >= totalSites)
          return false;
      }
    }

    if (u8fileSize < sizeof(uint16_t))
      return false;
    addr = readBytes(addr, &block.nrOfBlockOutputTrits, sizeof(uint16_t));
    u8fileSize -= sizeof(uint16_t);

    for (int n = 0; n < block.nrOfBlockOutputTrits; n++)
    {
      uint16_t outputIndex;
      if (u8fileSize < sizeof(uint16_t))
        return false;
      addr = readBytes(addr, &outputIndex, sizeof(uint16_t));
      u8fileSize -= sizeof(uint16_t);

      if (outputIndex >= totalSites)
        return false;
    }
  }

  return u8fileSize == 0;
}

//--------------------------------------------------------------------------------------------------------------------------------------------------------------

static uint8_t * readBytes(uint8_t *src, void *dest, uint16_t size)
{
  memcpy(dest, src, size);
  return src + size;
}
