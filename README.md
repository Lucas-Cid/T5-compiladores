# T4-compiladores

## Integrantes
- Gabriel de Jesus Dantas
- Lucas Machado Cid

Para compilar e executar o compilador, em uma pasta baixe todos os arquivos do github,
quanto as ferramentas utilizadas, foram java que pode ser baixado <a href = "https://www.oracle.com/java/technologies/downloads/#jdk18-linux">aqui </a>, suportada nas versões 17 ou superior por testes, e maven para empacotamento que pode ser encontrado links para download e tutoriais de instalação <a href = "https://maven.apache.org">aqui </a>, testado na versão 3.6.3.

Quanto aos passos, basta na pasta onde estão os arquivos desse github rodar o comando para empacotar o codigo para um .jar e depois com esse .jar basta executá-lo com dois argumentos, um o caminho para um arquivo onde está o texto que deve ser compilado, e outro aonde será armazenado o resultado, recomenda-se fazer isso na pasta do .jar:

```bash
mvn package
cd target
java -jar alguma-semantico-1.0-SNAPSHOT-jar-with-dependencies.jar \Caminho\Para\Entrada.txt \Caminho\Para\Saida.txt
```
