CREATE DATABASE telemedicine_auth;
CREATE DATABASE telemedicine_consultation;
CREATE DATABASE telemedicine_payment;
CREATE DATABASE telemedicine_notification;

GRANT ALL PRIVILEGES ON DATABASE telemedicine_auth TO postgres;
GRANT ALL PRIVILEGES ON DATABASE telemedicine_consultation TO postgres;
GRANT ALL PRIVILEGES ON DATABASE telemedicine_payment TO postgres;
GRANT ALL PRIVILEGES ON DATABASE telemedicine_notification TO postgres;
