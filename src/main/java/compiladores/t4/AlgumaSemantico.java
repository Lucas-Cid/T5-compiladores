package compiladores.t4;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.tree.TerminalNode;

import compiladores.t4.AlgumaParser.CmdAtribuicaoContext;
import compiladores.t4.AlgumaParser.CmdCasoContext;
import compiladores.t4.AlgumaParser.CmdChamadaContext;
import compiladores.t4.AlgumaParser.CmdRetorneContext;
import compiladores.t4.AlgumaParser.Declaracao_constanteContext;
import compiladores.t4.AlgumaParser.Declaracao_globalContext;
import compiladores.t4.AlgumaParser.Declaracao_tipoContext;
import compiladores.t4.AlgumaParser.Declaracao_variavelContext;
import compiladores.t4.AlgumaParser.IdentificadorContext;
import compiladores.t4.AlgumaParser.ParametroContext;
import compiladores.t4.AlgumaParser.ParametrosContext;
import compiladores.t4.AlgumaParser.Parcela_unarioContext;
import compiladores.t4.AlgumaParser.ProgramaContext;
import compiladores.t4.AlgumaParser.RegistroContext;
import compiladores.t4.AlgumaParser.Tipo_basico_identContext;
import compiladores.t4.AlgumaParser.VariavelContext;
import compiladores.t4.Table.InSymbol;

public class AlgumaSemantico extends AlgumaBaseVisitor {
    
    Escopo escopos = new Escopo(Table.Tipos.VOID);
    @Override
    public Object visitPrograma(ProgramaContext ctx) {
        return super.visitPrograma(ctx);
    }



    //ao declarar constante, apenas temos de garantir que o nome dela é novo
    @Override
    public Object visitDeclaracao_constante(Declaracao_constanteContext ctx) {
        Table escopoAtual = escopos.getEscopo();
        if (escopoAtual.exists(ctx.IDENT().getText())) {
            SemanticoUtils.adicionarErroSemantico(ctx.start, "constante" + ctx.IDENT().getText()
                    + " ja declarado anteriormente");
        } else {
            Table.Tipos tipo = Table.Tipos.INT;
            Table.Tipos aux = SemanticoUtils.getTipo(ctx.tipo_basico().getText()) ;
            if(aux != null)
                tipo = aux;
            escopoAtual.insert(ctx.IDENT().getText(), tipo, Table.Structure.CONST);
        }

        return super.visitDeclaracao_constante(ctx);
    }

    //ao declarar um tipo, esse pode ser um registro, devemos registrar entao, todos as variaveis dentro do mesmo
    //associandoas a ele, nesse caso o nome da variavel que vai ser um tipo, tem de ser registrada para ser utilizada
    //posteriormente, e o que tiver dentro do registro, é armazenado em uma tabela para tipos.
    @Override
    public Object visitDeclaracao_tipo(Declaracao_tipoContext ctx) {
        Table escopoAtual = escopos.getEscopo();
        if (escopoAtual.exists(ctx.IDENT().getText())) {
             SemanticoUtils.adicionarErroSemantico(ctx.start, "tipo " + ctx.IDENT().getText()
                    + " declarado duas vezes num mesmo escopo");
        } else {
            Table.Tipos tipo = SemanticoUtils.getTipo(ctx.tipo().getText());
            if(tipo != null)
                escopoAtual.insert(ctx.IDENT().getText(), tipo, Table.Structure.TIPO);
            else if(ctx.tipo().registro() != null){
                ArrayList<Table.InSymbol> varReg = new ArrayList<>();
                for(VariavelContext va : ctx.tipo().registro().variavel()){
                    Table.Tipos tipoReg =  SemanticoUtils.getTipo(va.tipo().getText());
                    for(IdentificadorContext id2 : va.identificador()){
                        varReg.add(escopoAtual.new InSymbol(id2.getText(), tipoReg, Table.Structure.TIPO));
                    }

                }

                if (escopoAtual.exists(ctx.IDENT().getText())) {
                    SemanticoUtils.adicionarErroSemantico(ctx.start, "identificador " + ctx.IDENT().getText()
                            + " ja declarado anteriormente");
                }
                else{
                    escopoAtual.insert(ctx.IDENT().getText(), Table.Tipos.REG, Table.Structure.TIPO);
                }

                for(Table.InSymbol re : varReg){
                    String nameVar = ctx.IDENT().getText() + '.' + re.name;
                    if (escopoAtual.exists(nameVar)) {
                        SemanticoUtils.adicionarErroSemantico(ctx.start, "identificador " + nameVar
                                + " ja declarado anteriormente");
                    }
                    else{
                        // SemanticoUtils.adicionarErroSemantico(id.start, "oi rs tamo adicionando " + re.name );
                        escopoAtual.insert(re);
                        escopoAtual.insert(ctx.IDENT().getText(), re);
                    }
                }
                // escopoAtual.insert(ctx.IDENT().getText(), Table.Tipos.REG, Table.Structure.TIPO);
            }
            Table.Tipos t =  SemanticoUtils.getTipo(ctx.tipo().getText());
            escopoAtual.insert(ctx.IDENT().getText(), t, Table.Structure.TIPO);
        }
        return super.visitDeclaracao_tipo(ctx);
    }

    //ao declarar variavel, verificamos se o identificador é novo, caso positivo podemos salvar
    //também devemos verificar caso seja um tipo registro para variavel, temos de associar essa
    //as variaveis do registro.
    @Override
    public Object visitDeclaracao_variavel(Declaracao_variavelContext ctx) {
        Table escopoAtual = escopos.getEscopo();
        for (IdentificadorContext id : ctx.variavel().identificador()) {
            String nomeId = "";
            int i = 0;
            for(TerminalNode ident : id.IDENT()){
                if(i++ > 0)
                    nomeId += ".";
                nomeId += ident.getText();
            }
            if (escopoAtual.exists(nomeId)) {
                SemanticoUtils.adicionarErroSemantico(id.start, "identificador " + nomeId
                        + " ja declarado anteriormente");
            } else {
                Table.Tipos tipo = SemanticoUtils.getTipo(ctx.variavel().tipo().getText());
                if(tipo != null)
                    escopoAtual.insert(nomeId, tipo, Table.Structure.VAR);
                else{
                    TerminalNode identTipo =    ctx.variavel().tipo() != null
                                                && ctx.variavel().tipo().tipo_estendido() != null 
                                                && ctx.variavel().tipo().tipo_estendido().tipo_basico_ident() != null  
                                                && ctx.variavel().tipo().tipo_estendido().tipo_basico_ident().IDENT() != null 
                                                ? ctx.variavel().tipo().tipo_estendido().tipo_basico_ident().IDENT() : null;
                    if(identTipo != null){
                        ArrayList<Table.InSymbol> regVars = null;
                        boolean found = false;
                        for(Table t: escopos.getPilha()){
                            if(!found){
                                if(t.exists(identTipo.getText())){
                                    regVars = t.getTypeProperties(identTipo.getText());
                                    found = true;
                                }
                            }
                        }
                        if(escopoAtual.exists(nomeId)){
                            SemanticoUtils.adicionarErroSemantico(id.start, "identificador " + nomeId
                                        + " ja declarado anteriormente");
                        } else{
                            escopoAtual.insert(nomeId, Table.Tipos.REG, Table.Structure.VAR);
                            for(Table.InSymbol s: regVars){
                                escopoAtual.insert(nomeId + "." + s.name, s.tipo, Table.Structure.VAR);
                            }   
                        }
                    }
                    else if(ctx.variavel().tipo().registro() != null){
                        ArrayList<Table.InSymbol> varReg = new ArrayList<>();
                        for(VariavelContext va : ctx.variavel().tipo().registro().variavel()){
                            Table.Tipos tipoReg =  SemanticoUtils.getTipo(va.tipo().getText());
                            for(IdentificadorContext id2 : va.identificador()){
                                varReg.add(escopoAtual.new InSymbol(id2.getText(), tipoReg, Table.Structure.VAR));
                            }
                        }  
                        escopoAtual.insert(nomeId, Table.Tipos.REG, Table.Structure.VAR);

                        for(Table.InSymbol re : varReg){
                            String nameVar = nomeId + '.' + re.name;
                            if (escopoAtual.exists(nameVar)) {
                                SemanticoUtils.adicionarErroSemantico(id.start, "identificador " + nameVar
                                        + " ja declarado anteriormente");
                            }
                            else{
                                // SemanticoUtils.adicionarErroSemantico(id.start, "oi rs tamo adicionando " + re.name );
                                escopoAtual.insert(re);
                                escopoAtual.insert(nameVar, re.tipo, Table.Structure.VAR);
                            }
                        }

                    }
                    else{//tipo registro estendido
                        escopoAtual.insert(id.getText(), Table.Tipos.INT, Table.Structure.VAR);
                    }
                }
            }
        }
        return super.visitDeclaracao_variavel(ctx);
    }


    //Para casos de função temos de declarar elas
    //tratando o fator de que o nome é único e suas variaveis também devem ter nome único,
    //para isso criamos um escopo para a função e tratamos as variaveis sendo criadas lá, para não interferirem em outros escopos
    @Override
    public Object visitDeclaracao_global(Declaracao_globalContext ctx) {
        Table escopoAtual = escopos.getEscopo();
        Object ret;
        if (escopoAtual.exists(ctx.IDENT().getText())) {
            SemanticoUtils.adicionarErroSemantico(ctx.start, ctx.IDENT().getText()
                    + " ja declarado anteriormente");
            ret = super.visitDeclaracao_global(ctx);
        } else {
            Table.Tipos returnTypeFunc = Table.Tipos.VOID;
            if(ctx.getText().startsWith("funcao")){
                returnTypeFunc = SemanticoUtils.getTipo(ctx.tipo_estendido().getText());
                escopoAtual.insert(ctx.IDENT().getText(), returnTypeFunc, Table.Structure.FUNC);
            }
            else{
                returnTypeFunc = Table.Tipos.VOID;
                escopoAtual.insert(ctx.IDENT().getText(), returnTypeFunc, Table.Structure.PROC);
            }
            escopos.create(returnTypeFunc);
            Table escopoAntigo = escopoAtual;
            escopoAtual = escopos.getEscopo();
            if(ctx.parametros() != null){
                for(ParametroContext p : ctx.parametros().parametro()){
                    for (IdentificadorContext id : p.identificador()) {
                        String nomeId = "";
                        int i = 0;
                        for(TerminalNode ident : id.IDENT()){
                            if(i++ > 0)
                                nomeId += ".";
                            nomeId += ident.getText();
                        }
                        if (escopoAtual.exists(nomeId)) {
                            SemanticoUtils.adicionarErroSemantico(id.start, "identificador " + nomeId
                                    + " ja declarado anteriormente");
                        } else {
                            Table.Tipos tipo = SemanticoUtils.getTipo(p.tipo_estendido().getText());
                            if(tipo != null){
                                InSymbol in = escopoAtual.new InSymbol(nomeId, tipo, Table.Structure.VAR);
                                escopoAtual.insert(in);
                                escopoAntigo.insert(ctx.IDENT().getText(), in);
                            }
                            else{
                                TerminalNode identTipo =    p.tipo_estendido().tipo_basico_ident() != null  
                                                            && p.tipo_estendido().tipo_basico_ident().IDENT() != null 
                                                            ? p.tipo_estendido().tipo_basico_ident().IDENT() : null;
                                if(identTipo != null){
                                    ArrayList<Table.InSymbol> regVars = null;
                                    boolean found = false;
                                    for(Table t: escopos.getPilha()){
                                        if(!found){
                                            if(t.exists(identTipo.getText())){
                                                regVars = t.getTypeProperties(identTipo.getText());
                                                found = true;
                                            }
                                        }
                                    }
                                    if(escopoAtual.exists(nomeId)){
                                        SemanticoUtils.adicionarErroSemantico(id.start, "identificador " + nomeId
                                                    + " ja declarado anteriormente");
                                    } else{
                                        InSymbol in = escopoAtual.new InSymbol(nomeId, Table.Tipos.REG, Table.Structure.VAR);
                                        escopoAtual.insert(in);
                                        escopoAntigo.insert(ctx.IDENT().getText(), in);

                                        for(Table.InSymbol s: regVars){
                                            escopoAtual.insert(nomeId + "." + s.name, s.tipo, Table.Structure.VAR);
                                        }   
                                    }
                                }
                            }
                        }
                    }
                }
            }
            ret = super.visitDeclaracao_global(ctx);
            escopos.dropEscopo();

        }
        return ret;
    }


    //verifica se o tipo é basico ou um nome de variavel, o importante é que caso seja nome ele deve existir
    @Override
    public Object visitTipo_basico_ident(Tipo_basico_identContext ctx) {
        if(ctx.IDENT() != null){
            boolean exists = false;
            for(Table escopo : escopos.getPilha()) {
                if(escopo.exists(ctx.IDENT().getText())) {
                    exists = true;
                }
            }
            if(!exists){
                SemanticoUtils.adicionarErroSemantico(ctx.start, "tipo " + ctx.IDENT().getText()
                            + " nao declarado");
            }
        }
        return super.visitTipo_basico_ident(ctx);
    }

    //verifica se o identificador existe na tabela, seu nome é composto como exemplo NOME1.NOME2.NOME....
    @Override
    public Object visitIdentificador(IdentificadorContext ctx) {
        String nomeVar = "";
        int i = 0;
        for(TerminalNode id : ctx.IDENT()){
            if(i++ > 0)
                nomeVar += ".";
            nomeVar += id.getText();
        }
        boolean erro = true;
        for(Table escopo : escopos.getPilha()) {

            if(escopo.exists(nomeVar)) {
                erro = false;
            }
        }
        if(erro)
            SemanticoUtils.adicionarErroSemantico(ctx.start, "identificador " + nomeVar + " nao declarado");
        return super.visitIdentificador(ctx);
    }

    //para casos de atribuição verifica-se se os tipos são compativeis
    @Override
    public Object visitCmdAtribuicao(CmdAtribuicaoContext ctx) {
        Table.Tipos tipoExpressao = SemanticoUtils.verificarTipo(escopos, ctx.expressao());
        boolean error = false;
        String pointerChar = ctx.getText().charAt(0) == '^' ? "^" : "";
        String nomeVar = "";
        int i = 0;
        for(TerminalNode id : ctx.identificador().IDENT()){
            if(i++ > 0)
                nomeVar += ".";
            nomeVar += id.getText();
        }
        if (tipoExpressao != Table.Tipos.INVALIDO) {
            boolean found = false;
            for(Table escopo : escopos.getPilha()){
                if (escopo.exists(nomeVar) && !found)  {
                    found = true;
                    Table.Tipos tipoVariavel = SemanticoUtils.verificarTipo(escopos, nomeVar);
                    Boolean varNumeric = tipoVariavel == Table.Tipos.REAL || tipoVariavel == Table.Tipos.INT;
                    Boolean expNumeric = tipoExpressao == Table.Tipos.REAL || tipoExpressao == Table.Tipos.INT;
                    if  (!(varNumeric && expNumeric) && tipoVariavel != tipoExpressao && tipoExpressao != Table.Tipos.INVALIDO) {
                        error = true;
                    }
                } 
            }
        } else{
            error = true;
        }

        if(error){
            nomeVar = ctx.identificador().getText();
            SemanticoUtils.adicionarErroSemantico(ctx.identificador().start, "atribuicao nao compativel para " + pointerChar + nomeVar );
        }

        return super.visitCmdAtribuicao(ctx);
    }

    //o comando de retorno deve ser diferente do tipo void que nao retorna nada
    @Override
    public Object visitCmdRetorne(CmdRetorneContext ctx) {
        if(escopos.getEscopo().returnType == Table.Tipos.VOID){
            SemanticoUtils.adicionarErroSemantico(ctx.start, "comando retorne nao permitido nesse escopo");
        } 
        return super.visitCmdRetorne(ctx);
    }

    //para parcela unarios, verificamos se a variavel existe
    @Override
    public Object visitParcela_unario(Parcela_unarioContext ctx) {
        Table escopoAtual = escopos.getEscopo();
        if(ctx.IDENT() != null){
            String name = ctx.IDENT().getText();
            if(escopoAtual.exists(ctx.IDENT().getText())){
                List<InSymbol> params = escopoAtual.getTypeProperties(name);
                boolean error = false;
                if(params.size() != ctx.expressao().size()){
                    error = true;
                } else {
                    for(int i = 0; i < params.size(); i++){
                        if(params.get(i).tipo != SemanticoUtils.verificarTipo(escopos, ctx.expressao().get(i))){
                            error = true;
                        }
                    }
                }
                if(error){
                    SemanticoUtils.adicionarErroSemantico(ctx.start, "incompatibilidade de parametros na chamada de " + name);
                }
            }
        }

        return super.visitParcela_unario(ctx);
    }
}
