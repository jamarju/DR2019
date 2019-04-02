#include <Arduino.h>

#include "motores.h"


void velocidades(long v_i, long v_d) {
    v_i = -v_i;
    v_d = -v_d;

    if (v_i > 255) {
      v_i = 255;
    }
    if (v_i < -255) {
      v_i = -255;
    }
    if (v_d > 255) {
      v_d = 255;
    }
    if (v_d < -255) {
      v_d = -255;
    }

    if (v_i > 0) {    
      digitalWrite(motor_AIN1, HIGH);
      analogWrite(motor_AIN2, 255 - v_i);  
    } else {    
      analogWrite(motor_AIN1, 255 + v_i);
      digitalWrite(motor_AIN2, HIGH);
    }

    if (v_d > 0) {
      digitalWrite(motor_BIN2, HIGH);
      analogWrite(motor_BIN1, 255 - v_d);
    } else {
      analogWrite(motor_BIN2, 255 + v_d);
      digitalWrite(motor_BIN1, HIGH);
    }
}
