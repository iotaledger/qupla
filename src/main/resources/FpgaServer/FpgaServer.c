#include <string.h>
#include <stdlib.h>
#include <stdio.h>
#include <errno.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <netinet/tcp.h>
#include <unistd.h>
#include <sys/time.h>
#include <time.h>

#include "Layout.h"
#include "Qlut.h"


#define check(condition, error)  if (!(condition)) { printf("%s\n", error); close(client); continue; }

void dump(uint8_t * buffer, int len);
void loadConfig(int client, uint8_t * data, int length);
void printConfig(int client, uint8_t * data, int length);
void processData(int client, uint8_t * data, int length);
void response(int client, int status, uint8_t * data, int length);

int server;
int client;
AbraLayout * layout;
Qlut * qlut;


int main(int argc, char* argv[])
{
  int    address_len;
  int    sendrc;
  int    bndrc;
  struct sockaddr_in  local_Address;
  address_len = sizeof(local_Address);

  memset(&local_Address,0x00,sizeof(local_Address));
  local_Address.sin_family = AF_INET;
  local_Address.sin_port = htons(6666);
  local_Address.sin_addr.s_addr = htonl(INADDR_ANY);

  qlut = qlutOpen();
  if (qlut == NULL)
  {
    printf("Cannot open /dev/mem\n");
    return -1;
  }

  server = socket(AF_INET, SOCK_STREAM, 0);
  if (server < 0)
  {
    printf("Socket failed\n");
    return -1;
  }

  if (bind(server,(struct sockaddr*)&local_Address, address_len) < 0)
  {
    printf("Bind failed\n");
    close(server);
    return -1;
  }

  char cmdLen[3];
  cmdLen[0] = '?';
  while (cmdLen[0] != 'q')
  {
    listen(server, 1);

    client = accept(server,(struct sockaddr*) NULL, 0);
    if (client < 0)
    {
      printf("Accept failed\n");
      continue;
    }

    clock_t start = clock();
    while (cmdLen[0] != 'q')
    {
      //printf("%ld ", (long)time(NULL));
      //clock_t end = clock();
      //printf("%ld\n", (long) (end - start));
      //start = end;

      int len = read(client, cmdLen, 3);
      if (len != 3)
      {
        printf("Read failed\n");
        break;
      }


      int bytes = (cmdLen[2] << 8) | cmdLen[1];
      //printf("\n%c %d\n", cmdLen[0], bytes);
      uint8_t * buffer = (uint8_t *) malloc(bytes);
      if (bytes > 0)
      {
        len = read(client, buffer, bytes);
        if (len != bytes)
        {
          dump(buffer, len);
          free(buffer);
          break;
        }
      }

      switch (cmdLen[0])
      {
      case 'c':
        loadConfig(client, buffer, bytes);
        break;

      case 'p':
        printConfig(client, buffer, bytes);
        break;

      case 'd':
        processData(client, buffer, bytes);
        break;

      case 'q':
        response(client, 1, NULL, 0);
        break;

      default:
        response(client, 0, NULL, 0);
        break;
      }

      free(buffer);
    }

    close(client);
  }

  close(server);
  qlutClose(qlut);
  return 0;
}

void dump(uint8_t * data, int length)
{
  for (int i = 0; i < length; i += 16)
  {
    printf("%04x: ", i);
    for (int j = 0; j < 16; j++)
    {
      if (i + j >= length) break;
      printf(" %02x", data[i + j]);
    }
    printf("\n");
  }
}

void loadConfig(int client, uint8_t * data, int length)
{
  if (!verifyLayout(data, length))
  {
    response(client, 0, NULL, 0);
    return;
  }

  layout = readLayout(data);

  qlutWaitIdle(qlut);
  qlutWriteConfig(qlut, layout);

  response(client, 1, NULL, 0);
}

void printConfig(int client, uint8_t * data, int length)
{
  dump(data, length);
  if (!verifyLayout(data, length))
  {
    response(client, 0, NULL, 0);
    return;
  }

  AbraLayout * layout = readLayout(data);
  printLayout(layout);

  response(client, 1, NULL, 0);
}

void processData(int client, uint8_t * data, int length)
{
  // dump(data, length);

  qlutWaitIdle(qlut);
  qlutWriteData(qlut, data, length);
  qlutProcessData(qlut);
  qlutWaitComplete(qlut);
  qlutReadData(qlut, layout);

  BlockDef * block = &layout->blockDefs[0];
  response(client, 1, block->outputTrits, block->nrOfBlockOutputTrits);

  // dump(block->outputTrits, block->nrOfBlockOutputTrits);
}

void response(int client, int status, uint8_t * data, int length)
{
  if (length == 0)
  {
    uint8_t cmdLen[3];
    cmdLen[0] = status;
    cmdLen[1] = 0;
    cmdLen[2] = 0;
    if (send(client, cmdLen, 3, 0) != 3)
    {
      printf("send cmdLen failed");
      close(client);
    }

    return;
  }

  uint8_t * buffer = (uint8_t *) malloc(length + 3);
  buffer[0] = status;
  buffer[1] = length;
  buffer[2] = length >> 8;
  memcpy(buffer + 3, data, length);
  if (send(client, buffer, length + 3, 0) != length + 3)
  {
    printf("send data failed");
    close(client);
  }
}