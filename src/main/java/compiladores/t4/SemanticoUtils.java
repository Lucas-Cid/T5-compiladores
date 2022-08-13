package compiladores.t4;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.Token;

import compiladores.t4.AlgumaParser.Exp_aritmeticaContext;
import compiladores.t4.AlgumaParser.ExpressaoContext;
import compiladores.t4.AlgumaParser.FatorContext;
import compiladores.t4.AlgumaParser.Fator_logicoContext;
import compiladores.t4.AlgumaParser.ParcelaContext;
import compiladores.t4.AlgumaParser.TermoContext;
import compiladores.t4.AlgumaParser.Termo_logicoContext;

public class SemanticoUtils {
    public static List<String> errosSemanticos = new ArrayList<>();
    
    public static void adicionarErroSemantico(Token t, String mensagem) {//adiciona um erro para a saida
        int linha = t.getLine();
        errosSemanticos.add(String.format("Linha %d: %s", linha, mensagem));
    }
    
    //verifica tipo de uma expressao, todos os termos devem ser do mesmo tipo
    public static Table.Tipos verificarTipo(Escopo escopos, AlgumaParser.ExpressaoContext ctx) {
        Table.Tipos ret = null;
        for (Termo_logicoContext ta : ctx.termo_logico()) {
            Table.Tipos aux = verificarTipo(escopos, ta);
            if (ret == null) {
                ret = aux;
            } else if (ret != aux && aux != Table.Tipos.INVALIDO) {
                ret = Table.Tipos.INVALIDO;
            }
        }
        return ret;
    }

    //verifica tipo de um termo logico, todos os fatores devem ser do mesmo tipo
    public static Table.Tipos verificarTipo(Escopo escopos, AlgumaParser.Termo_logicoContext ctx) {//
        Table.Tipos ret = null;
        for (Fator_logicoContext ta : ctx.fator_logico()) {
            Table.Tipos aux = verificarTipo(escopos, ta);
            if (ret == null) {
                ret = aux;
            } else if (ret != aux && aux != Table.Tipos.INVALIDO) {
                ret = Table.Tipos.INVALIDO;
            }
        }

        return ret;
    }

    //para verificar um fator, como ele e constituido por uma parcela, ele simplesmente verifica esta.
    public static Table.Tipos verificarTipo(Escopo escopos, AlgumaParser.Fator_logicoContext ctx) {
        //SemanticoUtils.adicionarErroSemantico(ctx.start, ctx.getText() + verificarTipo(escopos, ctx.parcela_logica()));
        return verificarTipo(escopos, ctx.parcela_logica());
    }

    public static Table.Tipos verificarTipo(Escopo escopos, AlgumaParser.Parcela_logicaContext ctx) {
        Table.Tipos ret = null;
        if(ctx.exp_relacional() != null){
            ret = verificarTipo(escopos, ctx.exp_relacional());
        } else{
            ret = Table.Tipos.LOGICO;
        }

        return ret;
    }

    //verifica o tipo da expressao relacional, que é constituida por expressoes aritmeticas
    public static Table.Tipos verificarTipo(Escopo escopos, AlgumaParser.Exp_relacionalContext ctx) {
        Table.Tipos ret = null;
        if(ctx.op_relacional() != null){
            for (Exp_aritmeticaContext ta : ctx.exp_aritmetica()) {
                Table.Tipos aux = verificarTipo(escopos, ta);
                Boolean auxNumeric = aux == Table.Tipos.REAL || aux == Table.Tipos.INT; //casos numericos inteiros e reais se correlacionam
                Boolean retNumeric = ret == Table.Tipos.REAL || ret == Table.Tipos.INT;
                if (ret == null) {
                    ret = aux;
                } else if (!(auxNumeric && retNumeric) && aux != ret) {
                    ret = Table.Tipos.INVALIDO;
                }
            }
            if(ret != Table.Tipos.INVALIDO){
                ret = Table.Tipos.LOGICO;
            }
        } else {
            ret = verificarTipo(escopos, ctx.exp_aritmetica(0));
        }

        return ret;
    }

    //verifica expressao aritmetica, verificando cada termo se são compativeis (mesmo tipo)
    public static Table.Tipos verificarTipo(Escopo escopos, AlgumaParser.Exp_aritmeticaContext ctx) {
        Table.Tipos ret = null;
        for (TermoContext ta : ctx.termo()) {
            Table.Tipos aux = verificarTipo(escopos, ta);
            if (ret == null) {
                ret = aux;
            } else if (ret != aux && aux != Table.Tipos.INVALIDO) {
                ret = Table.Tipos.INVALIDO;
            }
        }

        return ret;
    }

    //verifica um termo, composto por fatores que devem ser compativeis.
    public static Table.Tipos verificarTipo(Escopo escopos, AlgumaParser.TermoContext ctx) {
        Table.Tipos ret = null;

        for (FatorContext fa : ctx.fator()) {
            Table.Tipos aux = verificarTipo(escopos, fa);
            Boolean auxNumeric = aux == Table.Tipos.REAL || aux == Table.Tipos.INT; //casos numericos inteiros e reais se correlacionam
            Boolean retNumeric = ret == Table.Tipos.REAL || ret == Table.Tipos.INT;
            if (ret == null) {
                ret = aux;
            } else if (!(auxNumeric && retNumeric) && aux != ret) {
                ret = Table.Tipos.INVALIDO;
            }
        }
        return ret;
    }

    //para cada fator devemos verificar se as parcelas que os compoem são compativeis.
    public static Table.Tipos verificarTipo(Escopo escopos, AlgumaParser.FatorContext ctx) {
        Table.Tipos ret = null;

        for (ParcelaContext fa : ctx.parcela()) {
            Table.Tipos aux = verificarTipo(escopos, fa);
            if (ret == null) {
                ret = aux;
            } else if (ret != aux && aux != Table.Tipos.INVALIDO) {
                ret = Table.Tipos.INVALIDO;
            }
        }
        return ret;
    }

    //para caso de parcelas vamos verificar dependendo de seu tipo, pode ser unaria ou nao, um if para verificar o tipo em cada
    //caso foi utilizado.
    public static Table.Tipos verificarTipo(Escopo escopos, AlgumaParser.ParcelaContext ctx) {
        Table.Tipos ret = Table.Tipos.INVALIDO;

        if(ctx.parcela_nao_unario() != null){
            ret = verificarTipo(escopos, ctx.parcela_nao_unario());
        }
        else {
            ret = verificarTipo(escopos, ctx.parcela_unario());
        }
        return ret;
    }

    //na parcela nao unaria, temos um identificador ou uma cadeia, no caso de identificador temos de verificar seu tipo
    public static Table.Tipos verificarTipo(Escopo escopos, AlgumaParser.Parcela_nao_unarioContext ctx) {
        if (ctx.identificador() != null) {
            return verificarTipo(escopos, ctx.identificador());
        }
        return Table.Tipos.CADEIA;
    }

    //para verificar um identificador, verificamos seu nome completo, composto por exemplo NOME1.NOME2.NOME...
    //tendo o nome pronto, vemos se esse existe em algum escopo.
    public static Table.Tipos verificarTipo(Escopo escopos, AlgumaParser.IdentificadorContext ctx) {
        String nomeVar = "";
        Table.Tipos ret = Table.Tipos.INVALIDO;
        for(int i = 0; i < ctx.IDENT().size(); i++){
            nomeVar += ctx.IDENT(i).getText();
            if(i != ctx.IDENT().size() - 1){
                nomeVar += ".";
            }
        }
        for(Table tabela : escopos.getPilha()){
            if (tabela.exists(nomeVar)) {
                ret = verificarTipo(escopos, nomeVar);
            }
        }
        return ret;
    }
    
    //Para parcelas unarias, vemos qual seu tipo, ou seja o que esta escrito e o retornamos
    public static Table.Tipos verificarTipo(Escopo escopos, AlgumaParser.Parcela_unarioContext ctx) {
        if (ctx.NUM_INT() != null) {
            return Table.Tipos.INT;
        }
        if (ctx.NUM_REAL() != null) {
            return Table.Tipos.REAL;
        }
        if(ctx.identificador() != null){
            return verificarTipo(escopos, ctx.identificador());
        }
        if (ctx.IDENT() != null) {
            return verificarTipo(escopos, ctx.IDENT().getText());
        } else {
            Table.Tipos ret = null;
            for (ExpressaoContext fa : ctx.expressao()) {
                Table.Tipos aux = verificarTipo(escopos, fa);
                if (ret == null) {
                    ret = aux;
                } else if (ret != aux && aux != Table.Tipos.INVALIDO) {
                    ret = Table.Tipos.INVALIDO;
                }
            }
            return ret;
        }
    }
    
    //No caso de receber so uma string, vemos se ela existe, para descobrir se o nome da variavel foi criado ao ser utilizado.
    public static Table.Tipos verificarTipo(Escopo escopos, String nomeVar) {
        Table.Tipos type = Table.Tipos.INVALIDO;
        for(Table tabela : escopos.getPilha()){
            if(tabela.exists(nomeVar)){
                return tabela.verify(nomeVar);
            }
        }

        return type;
    }

    //modularizacao da função getTipo, relacionando a string lida a palavra reservada.
    public static Table.Tipos getTipo(String val){
        Table.Tipos tipo = null;
                switch(val) {
                    case "literal": 
                        tipo = Table.Tipos.CADEIA;
                        break;
                    case "inteiro": 
                        tipo = Table.Tipos.INT;
                        break;
                    case "real": 
                        tipo = Table.Tipos.REAL;
                        break;
                    case "logico": 
                        tipo = Table.Tipos.LOGICO;
                        break;
                    default:
                        break;
                }
        return tipo;
    }

    public static String getCType(String val){
        String tipo = null;
                switch(val) {
                    case "literal": 
                        tipo = "char";
                        break;
                    case "inteiro": 
                        tipo = "int";
                        break;
                    case "real": 
                        tipo = "float";
                        break;
                    default:
                        break;
                }
        return tipo;
    }

    public static String getCTypeSymbol(Table.Tipos tipo){
        String type = null;
                switch(tipo) {
                    case CADEIA: 
                        type = "s";
                        break;
                    case INT: 
                        type = "d";
                        break;
                    case REAL: 
                        type = "f";
                        break;
                    default:
                        break;
                }
        return type;
    }

}
