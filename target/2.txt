#include <stdio.h>
#include <stdlib.h>

void proc_imprime(char mensagem[80]){
printf("%s", mensagem);
printf("%s", "\n");
}

int main() {
proc_imprime("teste");
return 0;
}
