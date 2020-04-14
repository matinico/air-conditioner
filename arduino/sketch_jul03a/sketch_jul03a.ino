#include <Wire.h> 
#include <LiquidCrystal_I2C.h>
#include <IRremote.h>

#include <ArduinoJson.h>

#include <OneWire.h>
#include <DallasTemperature.h>

#define Pin 2
 
OneWire ourWire(Pin);
 
DallasTemperature sensors(&ourWire);

LiquidCrystal_I2C lcd(0x3F, 16, 2);

int buttonok = 7;
int buttonmas = 8;
int buttonmenos = 9;

int RECV_PIN = 11;

const int rele1 = 14;
const int rele2 = 15;

bool on = false;
bool frio = false;
bool change = false;

IRrecv irrecv(RECV_PIN);
decode_results results;

int temp = 20;

void setup() {
  lcd.begin();
  lcd.clear();
  lcd.cursor();
  lcd.print("Hola!");
  delay(3500);
  lcd.clear();
  lcd.print("Set temp");
  pinMode(buttonok, INPUT_PULLUP);
  pinMode(buttonmas, INPUT_PULLUP);
  pinMode(buttonmenos, INPUT_PULLUP);

  pinMode(rele1, OUTPUT);
  pinMode(rele2, OUTPUT);

  digitalWrite(rele1, HIGH);
  digitalWrite(rele2, HIGH);

  Serial.begin(9600);
  
  /*while(digitalRead(buttonok)){
  }
  if(!digitalRead(buttonok)){
    while(digitalRead(buttonok)){
    }
  }*/

  sensors.begin();

  irrecv.enableIRIn();
}

void loop() {
  float temperatura = sensors.getTempCByIndex(0);
  sensors.requestTemperatures();
  int t = 0;
  change = true;
  
  while(t <= 30){
    if(change){
      lcd.clear();
      lcd.print("Temperatura: ");
      lcd.print(temp);
      lcd.setCursor(0, 1);
      lcd.print("T AMB: ");
      lcd.print(temperatura);
      lcd.print(" ");
      if(on) lcd.print("ENC");
      else lcd.print("APA");
      change = false;
    }

    String data = "#{";
    data = data + "'type':'update','data':";
    data = data + temperatura;
    data = data + "}~";
    Serial.println(data);

    delay(10);

    String power = "#{";
    power = power + "'type':'power','data':";
    power = power + on;
    power = power + "}~";
    Serial.println(power);
  
    if(!digitalRead(buttonmas)){
      while(!digitalRead(buttonmas)){
      }
      temp = temp + 1;
      lcd.clear();
      if(temp >= 25) temp = 25;
      lcd.print("Temperatura: ");
      lcd.print(temp);
      lcd.setCursor(0, 1);
      lcd.print("T AMB: ");
      lcd.print(temperatura);
      change = true;
      sendTemp();
    }

    if(!digitalRead(buttonmenos)){
      while(!digitalRead(buttonmenos)){
      }
      temp = temp - 1;
      lcd.clear();
      if(temp <= 16) temp = 16;
      lcd.print("Temperatura: ");
      lcd.print(temp);
      lcd.setCursor(0, 1);
      lcd.print("T AMB: ");
      lcd.print(temperatura);
      change = true;
      sendTemp();
    }

    if(!digitalRead(buttonok)){
      while(!digitalRead(buttonok)){
      }
      if(!on){
        on = true;
        lcd.clear();
        lcd.print("Encendido");
      }
      else {
        on = false;
        lcd.clear();
        lcd.print("Apagado");
      }
      change = true;
    }

    if(on){
      if((temperatura - (float)temp) <= -2) {
        digitalWrite(rele1, LOW);
        digitalWrite(rele2, HIGH);
        frio = false;
      }
      else if((temperatura - (float)temp) >= 2) {
        digitalWrite(rele1, HIGH);
        digitalWrite(rele2, LOW);
        frio = true;
      }
      else if(frio){
        if(temperatura <= (float)temp) {
          digitalWrite(rele1, HIGH);
          digitalWrite(rele2, HIGH);
        }
      }
      else if(!frio){
        if(temperatura >= (float)temp) {
          digitalWrite(rele1, HIGH);
          digitalWrite(rele2, HIGH);
        }
      }
    }
    else {
      digitalWrite(rele1, HIGH);
      digitalWrite(rele2, HIGH);
    }

    if(Serial.available() > 0){
      String received = Serial.readString();

      //Serial.println(received);

      int size = received.length() + 1;
      char json[size];
      received.toCharArray(json, size);
  
      StaticJsonBuffer<200> jsonBuffer;
      JsonObject& root = jsonBuffer.parseObject(json);

      if (!root.success()) {
        //Serial.println("parseObject() failed");
        return;
      }

      String type = root["type"];
      /*Serial.println(root["type"] == "temp");
      Serial.println(type == "temp")¨*/
    
      if(type == "temp"){
        temp = root["data"];
    
        if(temp > 25) temp = 25;
        else if(temp < 16) temp = 16;
        sendTemp();
      }

      else if(type == "pair"){
        Serial.print("#{'type':[5]}~");
      }

      else if(type == "power") {
        String data = root["data"];
        if(data == "switch") on = !on;
        else if(data == "off") on = false;
        else if(data == "on") on = true;
        String power = "#{";
        power = power + "'type':'power','data':";
        power = power + on;
        power = power + "}~";
        change = true;
      }
      //delay(1000);
      Serial.flush();
    }

    if(irrecv.decode(&results)){
      switch(results.value){
        case 0xFFE01F:
          temp = temp - 1;
          lcd.clear();
          if(temp <= 16) temp = 16;
          lcd.print("Temperatura: ");
          lcd.print(temp);
          lcd.setCursor(0, 1);
          lcd.print("T AMB: ");
          lcd.print(sensors.getTempCByIndex(0));
          change = true;
          sendTemp();
          break;
          
        case 0xFFA857:
          temp = temp + 1;
          lcd.clear();
          if(temp >= 25) temp = 25;
          lcd.print("Temperatura: ");
          lcd.print(temp);
          lcd.setCursor(0, 1);
          lcd.print("T AMB: ");
          lcd.print(sensors.getTempCByIndex(0));
          change = true;
          sendTemp();
          break;
      
        case 0xFF906F:
          if(!on){
            on = true;
            lcd.clear();
            lcd.print("Encendido");
          }
          else {
            on = false;
            lcd.clear();
            lcd.print("Apagado");
          }
          change = true;
          break;
      }
      irrecv.resume();
    }
  
    delay(1);
    t = t + 1;
  }
}

void sendTemp(){
  delay(100);
  
  String data1 = "#{";
  data1 = data1 + "'type':'selected_temp','data':";
  data1 = data1 + temp;
  data1 = data1 + "}~";
  Serial.println(data1);
}
