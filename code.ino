#include <DHT.h>

//Constants
#define DHTPIN 8     // what pin we're connected to
#define DHTTYPE DHT22   // DHT 22  (AM2302)
DHT dht(DHTPIN, DHTTYPE); //// Initialize DHT sensor for normal 16mhz Arduino


//Variables
float hum;  //Stores humidity value
float temp;//Stores temperature value
int timecheck = 0;
int fsrPin = 0;     // the FSR and 10K pulldown are connected to a0
int fsrReading;     // the analog reading from the FSR resistor divider
int fsrVoltage;     // the analog reading converted to voltage
unsigned long fsrResistance;  // The voltage converted to resistance, can be very big so make "long"
unsigned long fsrConductance; 
long fsrForce;       // Finally, the resistance converted to force

void setup()
{
  Serial.begin(9600);
  dht.begin();
}

void loop()
{
    fsrReading = analogRead(fsrPin);  
  //Serial.print("Analog reading = ");
  //Serial.println(fsrReading);
 
  // analog voltage reading ranges from about 0 to 1023 which maps to 0V to 5V (= 5000mV)
  fsrVoltage = map(fsrReading, 0, 1023, 0, 5000);
  //Serial.print("Voltage reading in mV = ");
  
 
  if (fsrVoltage == 0) {
    //Serial.println("No pressure");  
  } else {
    // The voltage = Vcc * R / (R + FSR) where R = 10K and Vcc = 5V
    // so FSR = ((Vcc - V) * R) / V        yay math!
    fsrResistance = 5000 - fsrVoltage;     // fsrVoltage is in millivolts so 5V = 5000mV
    fsrResistance *= 10000;                // 10K resistor
    fsrResistance /= fsrVoltage;
    //Serial.print("FSR resistance in ohms = ");
    
 
    fsrConductance = 1000000;           // we measure in micromhos so 
    fsrConductance /= fsrResistance;
    //Serial.print("Conductance in microMhos: ");
    
 
    // Use the two FSR guide graphs to approximate the force
      fsrForce = fsrConductance /30;
      //Serial.print("Force in Newtons: ");
      //Serial.println(fsrForce);      
     
  }
  //delay(2000);
    //Read data and store it to variables hum and temp
    hum = dht.readHumidity();
    temp= dht.readTemperature();
    //Print temp and humidity values to serial monitor
    //Serial.print("Humidity: ");
    Serial.print(timecheck);
    Serial.print(",");
    Serial.print(hum);
    Serial.print(",");
    //Serial.write((byte)hum);
    //Serial.print(" %, Temp: ");
    Serial.print(temp);
    Serial.print(",");
    Serial.print(fsrVoltage);
    Serial.print(",");
    Serial.print(fsrResistance);
    Serial.print(",");
    Serial.print(fsrConductance); 
    Serial.print(",");
    Serial.println(fsrForce); 
    
    //Serial.write((byte)temp);
    //Serial.println(" Celsius");
    delay(1000); //Delay 2 sec.
    timecheck = timecheck+1;
}
