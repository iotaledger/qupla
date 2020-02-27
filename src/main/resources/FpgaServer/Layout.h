#ifndef H_LAYOUT
#define H_LAYOUT

#include <stdbool.h>
#include <stdint.h>


typedef struct
{
  uint16_t lutValues[4];
} LutDef;

typedef struct
{
  uint16_t lutIndex;
  uint16_t nrOfInputs;
  uint16_t * inputIndexes;
} SiteDef;

typedef struct
{
  uint16_t nrOfBlockInputTrits;
  uint16_t nrOfSites;
  SiteDef * siteDefs;
  uint16_t nrOfBlockOutputTrits;
  uint16_t * outputIndexes;
  uint8_t * outputTrits;
} BlockDef;

typedef struct
{
  uint16_t nrOfLuts;
  LutDef * lutDefs;
  uint16_t nrOfBlocks;
  BlockDef * blockDefs;
} AbraLayout;


extern void printLayout(AbraLayout *layout);
extern AbraLayout * readLayout(uint8_t *addr);
extern void saveLayout(AbraLayout *layout, char filename[31]);
extern bool verifyLayout(uint8_t * addr, int u8fileSize);

#endif
