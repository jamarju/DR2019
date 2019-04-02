#include <avr/wdt.h>
#include "motores.h"

#define MAX_BUF 32

/*
 * !!!
 * Cambiar en motores.h los pines de AIN1, AIN2, BIN1, BIN2
 * !!!
 */

char buf[MAX_BUF];
int ind = 0;

void reset() 
{
    cli();
    asm volatile ("mov r2, 0");
    wdt_enable(WDTO_15MS);
    for (;;);
}


void procesa_comando() 
{
  switch(buf[0]) {
    case 'b':
      // reinicia / vuelve al hexloader
      Serial.println("b ok");
      delay(100);
      reset();
    case 'v': {
      // cambia velocidades de motores
      int vsum, vdif;
      sscanf(buf + 1, "%d %d", &vsum, &vdif);
      velocidades(vsum + vdif / 2, vsum - vdif / 2);
      Serial.print("v ok");
    }
    default:
      Serial.println("???");
  }
}


void setup() {
  Serial.begin(115200);
  pinMode(13, OUTPUT);
  digitalWrite(13, HIGH);
  velocidades(0, 0);
}


void loop() {
  if (Serial.available()) {
    char c = Serial.read();
    Serial.print(c);
    if (c == 13 || c == 10) {
      buf[ind] = '\0';
      if (ind > 0) {
        procesa_comando();
      }
      ind = 0;
    } else if (ind < MAX_BUF - 1) {
      buf[ind] = c;
      ind++;
    }
  }
}
