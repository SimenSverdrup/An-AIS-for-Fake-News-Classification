# An Artificial Immune System for Fake News Classification

Simen Sverdrup-Thygeson, Master thesis in Computer Science, NTNU 2020/2021


## Prerequisites:
### Java:
* Apache HTTP-Client version 4.4.1
* Apache HTTP-Core version 4.4.14
* Apache Commons Logging version 1.2
* GSON version 2.8.5
* Java-JSON (org.json)
* Stanford CoreNLP version 4.2.0
* JavaFX version 15.0.1

### Python:
* Python version 3.6.5 (3.5 <= version < 3.8 should work too)
* Tensorflow version 1.13.1 (NOTE, newer TF version not compatible with BERT-as-a-service!)
* bert-serving-server + bert-serving-client (see https://github.com/hanxiao/bert-as-service and https://bert-as-service.readthedocs.io/en/latest/tutorial/http-server.html)
* flask + flask-compress + flask-cors + flask-json (see https://bert-as-service.readthedocs.io/en/latest/tutorial/http-server.html)

### Datasets (not included in repo due to large sizes):
* LIAR dataset
* FakeNewsNet dataset
* BERT Cased Base pre-trained model



## Running the Model
* Run (full path to BERT model needed) in terminal:
    bert-serving-start -model_dir=/YOUR_MODEL -http_port 8125
* Simply run main.java
