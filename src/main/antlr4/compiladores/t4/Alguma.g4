grammar Alguma;

programa: declaracoes 'algoritmo' corpo 'fim_algoritmo' EOF;
declaracoes: (decl_local_global)*;
decl_local_global: declaracao_local | declaracao_global;
declaracao_local: declaracao_variavel |
                declaracao_constante |
                declaracao_tipo;
declaracao_variavel: 'declare' variavel;
declaracao_constante: 'constante' IDENT ':' tipo_basico '=' valor_constante;
declaracao_tipo: 'tipo' IDENT ':' tipo;
variavel: identificador (',' identificador)* ':' tipo;
identificador: IDENT ('.' IDENT)* dimensao;
dimensao: ('[' exp_aritmetica ']')*;
tipo: registro | tipo_estendido;
tipo_basico: 'literal' | 'inteiro' | 'real' | 'logico';
tipo_basico_ident: tipo_basico | IDENT;
tipo_estendido: '^'? tipo_basico_ident;
valor_constante: CADEIA | NUM_INT | NUM_REAL | 'verdadeiro' | 'falso';
registro: 'registro' (variavel)* 'fim_registro';
declaracao_global: 'procedimento' IDENT '(' (parametros)? ')' (declaracao_local)* (cmd)* 'fim_procedimento' |
                'funcao' IDENT '(' (parametros)? ')' ':' tipo_estendido (declaracao_local)* (cmd)* 'fim_funcao';
parametro: 'var'? identificador (',' identificador)* ':' tipo_estendido;
parametros: parametro (',' parametro)*;
corpo: (declaracao_local)* (cmd)*;
cmd: cmdLeia | cmdEscreva | cmdSe | cmdCaso | cmdPara | cmdEnquanto |
    cmdFaca | cmdAtribuicao | cmdChamada | cmdRetorne;
cmdLeia: 'leia' '(' '^'? identificador (',' '^'? identificador)* ')';
cmdEscreva: 'escreva' '(' expressao (',' expressao)* ')';
cmdSe: 'se' expressao 'entao' (cmd)* cmdSenao? 'fim_se';
cmdSenao: 'senao' (cmd)*;
cmdCaso: 'caso' exp_aritmetica 'seja' selecao cmdSenao? 'fim_caso';
cmdPara: 'para' IDENT '<-' exp_aritmetica 'ate' exp_aritmetica 'faca' (cmd)* 'fim_para';
cmdEnquanto: 'enquanto' expressao 'faca' (cmd)* 'fim_enquanto';
cmdFaca: 'faca' (cmd)* 'ate' expressao;
cmdAtribuicao: '^'? identificador '<-' expressao;
cmdChamada: IDENT '(' expressao (',' expressao)* ')';
cmdRetorne: 'retorne' expressao;
selecao: (item_selecao)*;
item_selecao: constantes ':' (cmd)*;
constantes: numero_intervalo (',' numero_intervalo)*;
numero_intervalo: (op_unario)? NUM_INT ('..' (op_unario)? NUM_INT)?;
op_unario: '-';
exp_aritmetica: termo (op1 termo)*;
termo: fator (op2 fator)*;
fator: parcela (op3 parcela)*;
op1: '+' | '-';
op2: '*' | '/';
op3: '%';
parcela: (op_unario)? parcela_unario | parcela_nao_unario;
parcela_unario: '^'? identificador | IDENT '(' expressao (',' expressao)* ')' |
            NUM_INT | NUM_REAL | parentesis_expressao;
parentesis_expressao: '(' expressao ')';
parcela_nao_unario: '&' identificador | CADEIA;
exp_relacional: exp_aritmetica (op_relacional exp_aritmetica)?;
op_relacional: '=' | '<>' | '>=' | '<=' | '>' | '<';
expressao: CADEIA | IDENT | termo_logico (op_logico_1 termo_logico)*;
termo_logico: fator_logico (op_logico_2 fator_logico)*;
fator_logico: ('nao')? parcela_logica;
parcela_logica: ('verdadeiro' | 'falso') | exp_relacional;
op_logico_1: 'ou';
op_logico_2: 'e';
NUM_INT	: ('0'..'9')+; // Definição de numeros inteiros, qualquer combinação que tenha pelo menos um
NUM_REAL	: ('0'..'9')+ ('.' ('0'..'9')+)?; //Definição de numeros reais, que devem possuir um ponto e outra sequencia de numero após
Digito	:	'0'..'9'; //Definição de um digito, que pode vir a estar combinado em um nome de variavel
IDENT	:	([a-zA-Z])([a-zA-Z]|Digito|'_')*; //O identificador é uma sequência de caracteres começada em letra
//que aceita letras digitos e _
//Ignora comentario, sendo esse qualquer sequencia de caracteres em uma linha dentro de {}
Comentario  :  '{' (~('\n'|'\r'|'{'|'}'))* '}' '\r'? '\n'? {skip();}; 
//Para comentarios nao fechados é verificado se ocorre uma abertura de parenteses sem fecha na mesma linha
Nao_Fechado  :  '{' (~('\n'|'\r'|'{'|'}'))* '\r'? '\n'?;
//Uma Cadeia é uma sequencia de caracteres que estejam dentro de ""
CADEIA 	: '"' ( ESC_SEQ | ~('"'|'\\'|'\n'|'\r') )* '"';
//A mesma logica do comentario utilizada para cadeias nao fechadas
Literal_Nao_Fechada: '"' ( ESC_SEQ | ~('"'|'\\') )* '\r'? '\n'?;
ESC_SEQ	: '\\\'';
WS  :   ( ' '
        | '\t'
        | '\r'
        | '\n'
        ) {skip();}
    ;
//Para erro qualquer outro caracter que não for achado até agora, vai ser um erro
ERR : ~('a');