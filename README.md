# An Artificial Immune System for Fake News Classification

Simen Sverdrup-Thygeson, Master's thesis in Computer Science, Norwegian University of Science and Technology 2020/2021


## Prerequisites:
### Java:
* Apache HTTP-Client version 4.4.1
* Apache HTTP-Core version 4.4.14
* Apache Commons Logging version 1.2
* GSON version 2.8.5
* Java-JSON (org.json)
* Stanford CoreNLP version 4.2.0
* Stanford CoreNLP SRParser (Shift-Reduce Parser for speedup during sentiment analysis)
* JavaFX version 15.0.1
* JavaMI version 1.1

### Python:
* Python version 3.6.5 (3.5 <= version < 3.8 should work too)
* Tensorflow version 1.13.1 (NOTE, newer TF version not compatible with BERT-as-a-service!)
* bert-serving-server + bert-serving-client (see https://github.com/hanxiao/bert-as-service and https://bert-as-service.readthedocs.io/en/latest/tutorial/http-server.html)
* flask + flask-compress + flask-cors + flask-json (see https://bert-as-service.readthedocs.io/en/latest/tutorial/http-server.html)

### Datasets (not included in repo due to large sizes):
* Kaggle dataset (https://www.kaggle.com/c/fake-news/overview)
* LIAR dataset
* FakeNewsNet dataset
* BERT Cased Base pre-trained model

## Running the Model
* Navigate to project directory and run (full path to BERT model needed) in Anaconda terminal:
bert-serving-start -pooling_strategy=REDUCE_MEAN -model_dir=C:\Users\simen\Documents\A_Studier\Masteroppgave\Kode\Masteropg\cased_L-12_H-768_A-12 -http_port 8125
* Simply run main.java
