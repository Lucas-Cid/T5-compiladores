package compiladores.t4;

import java.util.ArrayList;
import java.util.Arrays;

import org.antlr.v4.runtime.tree.TerminalNode;

import compiladores.t4.AlgumaParser.CmdAtribuicaoContext;
import compiladores.t4.AlgumaParser.CmdCasoContext;
import compiladores.t4.AlgumaParser.CmdChamadaContext;
import compiladores.t4.AlgumaParser.CmdContext;
import compiladores.t4.AlgumaParser.CmdEnquantoContext;
import compiladores.t4.AlgumaParser.CmdEscrevaContext;
import compiladores.t4.AlgumaParser.CmdFacaContext;
import compiladores.t4.AlgumaParser.CmdLeiaContext;
import compiladores.t4.AlgumaParser.CmdParaContext;
import compiladores.t4.AlgumaParser.CmdRetorneContext;
import compiladores.t4.AlgumaParser.CmdSeContext;
import compiladores.t4.AlgumaParser.CmdSenaoContext;
import compiladores.t4.AlgumaParser.CorpoContext;
import compiladores.t4.AlgumaParser.Decl_local_globalContext;
import compiladores.t4.AlgumaParser.Declaracao_constanteContext;
import compiladores.t4.AlgumaParser.Declaracao_globalContext;
import compiladores.t4.AlgumaParser.Declaracao_localContext;
import compiladores.t4.AlgumaParser.Declaracao_tipoContext;
import compiladores.t4.AlgumaParser.Declaracao_variavelContext;
import compiladores.t4.AlgumaParser.DimensaoContext;
import compiladores.t4.AlgumaParser.Exp_aritmeticaContext;
import compiladores.t4.AlgumaParser.Exp_relacionalContext;
import compiladores.t4.AlgumaParser.ExpressaoContext;
import compiladores.t4.AlgumaParser.FatorContext;
import compiladores.t4.AlgumaParser.Fator_logicoContext;
import compiladores.t4.AlgumaParser.IdentificadorContext;
import compiladores.t4.AlgumaParser.Item_selecaoContext;
import compiladores.t4.AlgumaParser.ParametroContext;
import compiladores.t4.AlgumaParser.ParcelaContext;
import compiladores.t4.AlgumaParser.Parcela_logicaContext;
import compiladores.t4.AlgumaParser.Parcela_nao_unarioContext;
import compiladores.t4.AlgumaParser.Parcela_unarioContext;
import compiladores.t4.AlgumaParser.RegistroContext;
import compiladores.t4.AlgumaParser.SelecaoContext;
import compiladores.t4.AlgumaParser.TermoContext;
import compiladores.t4.AlgumaParser.Termo_logicoContext;
import compiladores.t4.AlgumaParser.TipoContext;
import compiladores.t4.AlgumaParser.Tipo_basico_identContext;
import compiladores.t4.AlgumaParser.Tipo_estendidoContext;
import compiladores.t4.AlgumaParser.Valor_constanteContext;
import compiladores.t4.AlgumaParser.VariavelContext;
import compiladores.t4.Table.InSymbol;

public class AlgumaGeradorC extends AlgumaBaseVisitor<Void> {
    StringBuilder saida;
    Table tabela;

    public AlgumaGeradorC() {
        saida = new StringBuilder();
        this.tabela = new Table();
    }

    @Override
    public Void visitPrograma(AlgumaParser.ProgramaContext ctx) {//constroi a estrutura padrao do programa, visitando declaracoes e corpo
        saida.append("#include <stdio.h>\n");
        saida.append("#include <stdlib.h>\n");
        saida.append("\n");
        ctx.declaracoes().decl_local_global().forEach(dec -> visitDecl_local_global(dec));
        saida.append("\n");
        saida.append("int main() {\n");

        visitCorpo(ctx.corpo());
        saida.append("return 0;\n");
        saida.append("}\n");
        return null;
    }

    @Override
    public Void visitDecl_local_global(Decl_local_globalContext ctx) {//define se a declaracao e local, ou global e redireciona
        // TODO Auto-generated method stub
        if(ctx.declaracao_local() != null){
            visitDeclaracao_local(ctx.declaracao_local());
        }
        else if(ctx.declaracao_global() != null){
            visitDeclaracao_global(ctx.declaracao_global());
        }
        return null;
    }

    @Override
    public Void visitCorpo(CorpoContext ctx) {// visita cada declaracao no corpo do codigo, em seguida todos os comandos
        for(AlgumaParser.Declaracao_localContext dec : ctx.declaracao_local()) {
            visitDeclaracao_local(dec);
        }

        for(AlgumaParser.CmdContext com : ctx.cmd()) {
            visitCmd(com);
        }

        return null;
    }

    @Override
    public Void visitDeclaracao_global(Declaracao_globalContext ctx) {//declara ou procedimento ou funcoes
        // TODO Auto-generated method stub
        if(ctx.getText().contains("procedimento")){
            saida.append("void " + ctx.IDENT().getText() + "(");
        }
        else{
            String cTipo = SemanticoUtils.getCType(ctx.tipo_estendido().getText().replace("^", ""));
            Table.Tipos tipo = SemanticoUtils.getTipo(ctx.tipo_estendido().getText());
            visitTipo_estendido(ctx.tipo_estendido());
            if(cTipo == "char"){
                saida.append("[80]");
            }
            saida.append(" " + ctx.IDENT().getText() + "(");
            tabela.insert(ctx.IDENT().getText(), tipo, Table.Structure.FUNC);
        }
            ctx.parametros().parametro().forEach(var -> visitParametro(var));
            saida.append("){\n");
            ctx.declaracao_local().forEach(var -> visitDeclaracao_local(var));
            ctx.cmd().forEach(var -> visitCmd(var));
            saida.append("}\n");

        return null;
    }

    @Override
    public Void visitIdentificador(IdentificadorContext ctx) {//criado para imprimir identificadores com dimensoes
        // TODO Auto-generated method stub
        saida.append(" ");
        int i = 0;
        for(TerminalNode id : ctx.IDENT()){
            if(i++ > 0)
                saida.append(".");
            saida.append(id.getText());
        }
        visitDimensao(ctx.dimensao());
        return null;
    }

    @Override
    public Void visitDimensao(DimensaoContext ctx) {//imprime a dimensao
        // TODO Auto-generated method stub
        for(Exp_aritmeticaContext exp : ctx.exp_aritmetica()){
            saida.append("[");
            visitExp_aritmetica(exp);
            saida.append("]");
        }

        return null;
    }

    @Override
    public Void visitParametro(ParametroContext ctx) {//para converter parametros de funções 1 a 1
        // TODO Auto-generated method stub
        int i = 0;
        String cTipo = SemanticoUtils.getCType(ctx.tipo_estendido().getText().replace("^", ""));
        Table.Tipos tipo = SemanticoUtils.getTipo(ctx.tipo_estendido().getText());
        for(IdentificadorContext id : ctx.identificador()){
            if(i++ > 0)
                saida.append(",");
            visitTipo_estendido(ctx.tipo_estendido());
            // saida.append(" " + id.getText());
            visitIdentificador(id);

            if(cTipo == "char"){
                saida.append("[80]");
            }
            tabela.insert(id.getText(),tipo,Table.Structure.VAR);
        }
        return null;
    }

    @Override
    public Void visitDeclaracao_local(Declaracao_localContext ctx) {// ve o tipo de declaracao e redireciona
        System.out.println("Declaring " + ctx.getText());
        if(ctx.declaracao_variavel() != null){
            visitDeclaracao_variavel(ctx.declaracao_variavel());
        }
        if(ctx.declaracao_constante() != null){
            visitDeclaracao_constante(ctx.declaracao_constante());
        } 
        else if(ctx.declaracao_tipo() != null){
            visitDeclaracao_tipo(ctx.declaracao_tipo());
        }

        return null;
    }

    @Override
    public Void visitDeclaracao_tipo(Declaracao_tipoContext ctx) {//cria um tipo (typedef)
        // TODO Auto-generated method stub
        saida.append("typedef ");
        String cTipo = SemanticoUtils.getCType(ctx.tipo().getText().replace("^", ""));
        Table.Tipos tipo = SemanticoUtils.getTipo(ctx.tipo().getText());
       
        if(ctx.tipo().getText().contains("registro")){
            for(VariavelContext sub : ctx.tipo().registro().variavel()){
                for(IdentificadorContext idIns : sub.identificador()){
                    Table.Tipos tipoIns = SemanticoUtils.getTipo(sub.tipo().getText());
                    System.out.println("Inserting reg " + sub.getText() + "." + idIns.getText());
                    tabela.insert(ctx.IDENT().getText() + "." + idIns.getText(), tipoIns, Table.Structure.VAR);
                    tabela.insert(ctx.IDENT().getText(), tabela.new InSymbol(idIns.getText(), tipoIns, Table.Structure.TIPO));
                }
            }
        }
        tabela.insert(ctx.IDENT().getText(), tipo, Table.Structure.VAR);
        visitTipo(ctx.tipo());
        saida.append(ctx.IDENT() + ";\n");
        return null;
    }

    @Override
    public Void visitDeclaracao_variavel(Declaracao_variavelContext ctx) {//declaracao variavel, chama a variavel
        visitVariavel(ctx.variavel());
        return null;
    }

    @Override
    public Void visitVariavel(VariavelContext ctx) {//aqui onde realmente as variaveis sao desclarados
        // TODO Auto-generated method stub
        String cTipo = SemanticoUtils.getCType(ctx.tipo().getText().replace("^", ""));
        System.out.println("Visiting " + ctx.getText());
        Table.Tipos tipo = SemanticoUtils.getTipo(ctx.tipo().getText());
        for(AlgumaParser.IdentificadorContext id: ctx.identificador()) {
            if(ctx.tipo().getText().contains("registro")){
                for(VariavelContext sub : ctx.tipo().registro().variavel()){
                    for(IdentificadorContext idIns : sub.identificador()){
                        Table.Tipos tipoIns = SemanticoUtils.getTipo(sub.tipo().getText());
                        tabela.insert(id.getText() + "." + idIns.getText(), tipoIns, Table.Structure.VAR);
                    }
                }
            }
            else if(cTipo == null && tipo == null){
                ArrayList<InSymbol> arg = tabela.getTypeProperties(ctx.tipo().getText());
                if(arg != null){
                    for(Table.InSymbol val : arg){
                        tabela.insert(id.getText() + "." + val.name, val.tipo, Table.Structure.VAR);
                    }
                }
            }
            if(id.getText().contains("[")){
                int ini = id.getText().indexOf("[", 0);
                int end = id.getText().indexOf("]", 0);
                System.out.println("ini = " + (ini+1) + " end = " + (end-1) + " out of " + id.getText());
                String tam;
                if(end-ini == 2)
                    tam = String.valueOf(id.getText().charAt(ini+1));
                else
                    tam = id.getText().substring(ini + 1, end - 1);
                String name = id.IDENT().get(0).getText();
                for(int i = 0; i < Integer.parseInt(tam); i++){
                    System.out.println("Cadastrano " + name + "[" + i + "]");
                    tabela.insert(name + "[" + i + "]", tipo, Table.Structure.VAR);
                }

            }
            else{
                tabela.insert(id.getText(), tipo, Table.Structure.VAR);
            }
            visitTipo(ctx.tipo());
            // saida.append(id.getText());
            visitIdentificador(id);
            if(cTipo == "char"){
                saida.append("[80]");
            }
            saida.append(";\n");
        }
        return null;
    }

    @Override
    public Void visitTipo(TipoContext ctx) {//visita o tipo para definir se é registro, ou estendido, ou normal
        // TODO Auto-generated method stub
        String cTipo = SemanticoUtils.getCType(ctx.getText().replace("^", ""));
        Table.Tipos tipo = SemanticoUtils.getTipo(ctx.getText());
        boolean pointer = ctx.getText().contains("^");
        if(cTipo != null){
            saida.append(cTipo);
        }
        else if(ctx.registro() != null){
            visitRegistro(ctx.registro());
        }
        else{
            visitTipo_estendido(ctx.tipo_estendido());
        }
        if(pointer)
            saida.append("*");
        saida.append(" ");

        return null;
    }
    @Override
    public Void visitTipo_estendido(Tipo_estendidoContext ctx) {//imprime tipo estendido, nada mais é que um ponteiro
        // TODO Auto-generated method stub
        visitTipo_basico_ident(ctx.tipo_basico_ident());
        if(ctx.getText().contains("^"))
            saida.append("*");
        return null;
    }
    @Override
    public Void visitTipo_basico_ident(Tipo_basico_identContext ctx) {//tipos basicos, ou palavras reservadas, ou variaveis tipo
        // TODO Auto-generated method stub
        if(ctx.IDENT() != null){
            saida.append(ctx.IDENT().getText());
        }
        else{
            saida.append(SemanticoUtils.getCType(ctx.getText().replace("^", "")));
        }
        return null;
    }

    @Override
    public Void visitRegistro(RegistroContext ctx) {//cria o struct
        // TODO Auto-generated method stub
        saida.append("struct {\n");
        ctx.variavel().forEach(var -> visitVariavel(var));
        saida.append("} ");
        return null;
    }

    @Override
    public Void visitDeclaracao_constante(Declaracao_constanteContext ctx) {//declara constante, por meioi do prefixo const
        // TODO Auto-generated method stub
        String type = SemanticoUtils.getCType(ctx.tipo_basico().getText());
        Table.Tipos typeVar = SemanticoUtils.getTipo(ctx.tipo_basico().getText());
        tabela.insert(ctx.IDENT().getText(),typeVar,Table.Structure.VAR);
        saida.append("const " + type + " " + ctx.IDENT().getText() + " = ");
        visitValor_constante(ctx.valor_constante());
        saida.append(";\n");
        return null;
    }

    @Override
    public Void visitValor_constante(Valor_constanteContext ctx) {// retorna o valor, convertendo para sintaxe de c
        // TODO Auto-generated method stub
        if(ctx.getText().equals("verdadeiro")){
            saida.append("true");
        }
        else if(ctx.getText().equals("falso")){
            saida.append("false");
        }
        else{
            saida.append(ctx.getText());
        }
        return null;
    }

    @Override
    public Void visitCmd(CmdContext ctx) {// redireciona para o cmd
        if(ctx.cmdLeia() != null){
            visitCmdLeia(ctx.cmdLeia());
        } else if(ctx.cmdEscreva() != null){
            visitCmdEscreva(ctx.cmdEscreva());
        } else if(ctx.cmdAtribuicao() != null){
            visitCmdAtribuicao(ctx.cmdAtribuicao());
        } 
        else if(ctx.cmdSe() != null){
            visitCmdSe(ctx.cmdSe());
        }
        else if(ctx.cmdCaso() != null){
            visitCmdCaso(ctx.cmdCaso());
        }
        else if(ctx.cmdPara() != null){
            visitCmdPara(ctx.cmdPara());
        }
        else if(ctx.cmdEnquanto() != null){
            visitCmdEnquanto(ctx.cmdEnquanto());
        }
        else if(ctx.cmdFaca() != null){
            visitCmdFaca(ctx.cmdFaca());
        }
        else if(ctx.cmdChamada() != null){
            visitCmdChamada(ctx.cmdChamada());
        }
        else if(ctx.cmdRetorne() != null){
            visitCmdRetorne(ctx.cmdRetorne());
        }
        return null;
    }

    @Override
    public Void visitCmdRetorne(CmdRetorneContext ctx) {//adiciona return, e pega a expressao que vai retornar
        // TODO Auto-generated method stub
        saida.append("return ");
        visitExpressao(ctx.expressao());
        saida.append(";\n");
        return null;
    }

    @Override
    public Void visitCmdChamada(CmdChamadaContext ctx) {//comando de chamada de função
        // TODO Auto-generated method stub
        saida.append(ctx.IDENT().getText() + "(");
        int i = 0;
        for(ExpressaoContext exp : ctx.expressao()){
            if(i++ > 0)
                saida.append(",");
            visitExpressao(exp);
        }
        saida.append(");\n");
        return null;
    }

    @Override
    public Void visitCmdLeia(CmdLeiaContext ctx) {// comando de ler variavel
        for(AlgumaParser.IdentificadorContext id: ctx.identificador()) {
            Table.Tipos idType = tabela.verify(id.getText());
            if(idType != Table.Tipos.CADEIA){
                saida.append("scanf(\"%");
                saida.append(SemanticoUtils.getCTypeSymbol(idType));
                saida.append("\", &");
                saida.append(id.getText());
                saida.append(");\n");
            } else {
                saida.append("gets(");
                // saida.append(id.getText());
                visitIdentificador(id);
                saida.append(");\n");
            }
        }
        
        return null;
    }

    @Override
    public Void visitCmdEscreva(CmdEscrevaContext ctx) { // comando para escrever a variavel, verifica o tipo ou na tabela, ou no utils
        for(AlgumaParser.ExpressaoContext exp: ctx.expressao()) {
                Escopo escopo = new Escopo(tabela);
                System.out.println("Searching for " + exp.getText());
                System.out.println("Does it exists in table? " + tabela.exists(exp.getText()));
                String cType = SemanticoUtils.getCTypeSymbol(SemanticoUtils.verificarTipo(escopo, exp));
                if(tabela.exists(exp.getText())){
                    Table.Tipos tip = tabela.verify(exp.getText());
                    cType = SemanticoUtils.getCTypeSymbol(tip);
                }
                saida.append("printf(\"%");
                saida.append(cType);
                saida.append("\", ");
                saida.append(exp.getText());
                saida.append(");\n");
        }
        return null;
    }

    @Override
    public Void visitCmdAtribuicao(CmdAtribuicaoContext ctx) {//atribui valores para a variavel, e usa strcpy para strings
        if(ctx.getText().contains("^"))
            saida.append("*");
        try{
            Table.Tipos tip = tabela.verify(ctx.identificador().getText());

            if(tip != null && tip == Table.Tipos.CADEIA){
                // saida.append("strcpy(" + ctx.identificador().getText()+","+ctx.expressao().getText()+");\n");
                saida.append("strcpy(");
                visitIdentificador(ctx.identificador());
                saida.append(","+ctx.expressao().getText()+");\n");
            }
            else{
                // saida.append(ctx.identificador().getText());
                visitIdentificador(ctx.identificador());
                saida.append(" = ");
                saida.append(ctx.expressao().getText());
                saida.append(";\n");
            }
        }
        catch(Exception e){
            System.out.println(e.getMessage() +  " q ocorreu");
        }
        return null;
    }

    @Override
    public Void visitCmdSe(CmdSeContext ctx) {//transcrição do comando if else
        saida.append("if(");
        visitExpressao(ctx.expressao());
        saida.append(") {\n");
        for(CmdContext cmd : ctx.cmd()) {
            visitCmd(cmd);
        }
        saida.append("}\n");
        if(ctx.cmdSenao() != null){
            saida.append("else {\n");
            for(CmdContext cmd : ctx.cmdSenao().cmd()) {
                visitCmd(cmd);
            }
            saida.append("}\n");
        }
        
        return null;
    }

    @Override
    public Void visitExpressao(ExpressaoContext ctx) {//usado para visitar uma expressao, que e constituida de termos e operadores
        if(ctx.termo_logico() != null){
            visitTermo_logico(ctx.termo_logico(0));

            for(int i = 1; i < ctx.termo_logico().size(); i++){
                AlgumaParser.Termo_logicoContext termo = ctx.termo_logico(i);
                saida.append(" || ");
                visitTermo_logico(termo);
            }
        }

        return null;
    }

    @Override
    public Void visitTermo_logico(Termo_logicoContext ctx) {//usado para visitar termos logicos
        visitFator_logico(ctx.fator_logico(0));

        for(int i = 1; i < ctx.fator_logico().size(); i++){
            AlgumaParser.Fator_logicoContext fator = ctx.fator_logico(i);
            saida.append(" && ");
            visitFator_logico(fator);
        }
        
        return null;
    }

    @Override
    public Void visitFator_logico(Fator_logicoContext ctx) {// usado para visitar fatores logicos
        if(ctx.getText().startsWith("nao")){
            saida.append("!");
        }
        visitParcela_logica(ctx.parcela_logica());
        
        return null;
    }

    @Override
    public Void visitParcela_logica(Parcela_logicaContext ctx) {//usado para visitar parcelas logicas
        if(ctx.exp_relacional() != null){
            visitExp_relacional(ctx.exp_relacional());
        } else{
            if(ctx.getText() == "verdadeiro"){
                saida.append("true");
            } else {
                saida.append("false");
            }
        }
        
        return null;
    }

    // usado para expressoes relacionais, convertendo o simbolo de igualdade para o equivalente em c
    @Override
    public Void visitExp_relacional(Exp_relacionalContext ctx) {
         visitExp_aritmetica(ctx.exp_aritmetica(0));
        for(int i = 1; i < ctx.exp_aritmetica().size(); i++){
            AlgumaParser.Exp_aritmeticaContext termo = ctx.exp_aritmetica(i);
            if(ctx.op_relacional().getText().equals("=")){
                saida.append(" == ");
            } else{
                saida.append(ctx.op_relacional().getText());
            }
            visitExp_aritmetica(termo);
        }
        
        return null;
    }

    @Override
    public Void visitExp_aritmetica(Exp_aritmeticaContext ctx) {//visitar expressoes aritmeticas
        visitTermo(ctx.termo(0));

        for(int i = 1; i < ctx.termo().size(); i++){
            AlgumaParser.TermoContext termo = ctx.termo(i);
            saida.append(ctx.op1(i-1).getText());
            visitTermo(termo);
        }
        return null;
    }

    @Override
    public Void visitTermo(TermoContext ctx) {//visita o termo para verificar se tem fatores
       visitFator(ctx.fator(0));

        for(int i = 1; i < ctx.fator().size(); i++){
            AlgumaParser.FatorContext fator = ctx.fator(i);
            saida.append(ctx.op2(i-1).getText());
            visitFator(fator);
        }
        return null;
    }

    @Override
    public Void visitFator(FatorContext ctx) {//visita o fator para verificar se tem parcelas
        visitParcela(ctx.parcela(0));

        for(int i = 1; i < ctx.parcela().size(); i++){
            AlgumaParser.ParcelaContext parcela = ctx.parcela(i);
            saida.append(ctx.op3(i-1).getText());
            visitParcela(parcela);
        }
        return null;
    }

    @Override
    public Void visitParcela(ParcelaContext ctx) {//redireciona parcela para unaria ou nao unaria
        if(ctx.parcela_unario() != null){
            if(ctx.op_unario() != null){
                saida.append(ctx.op_unario().getText());
            }
            visitParcela_unario(ctx.parcela_unario());
        } else{
            visitParcela_nao_unario(ctx.parcela_nao_unario());
        }
        
        return null;
    }

    @Override
    public Void visitParcela_unario(Parcela_unarioContext ctx) {
        //visitar parcela unario imprimindo todos os identificadores, ou redireciona caso chegou aqui com uma expressao ent
        if(ctx.IDENT() != null){
            saida.append(ctx.IDENT().getText());
            saida.append("(");
            for(int i = 0; i < ctx.expressao().size(); i++){
                visitExpressao(ctx.expressao(i));
                if(i < ctx.expressao().size()-1){
                    saida.append(", ");
                }
            }
        } else if(ctx.parentesis_expressao() != null){
            saida.append("(");
            visitExpressao(ctx.parentesis_expressao().expressao());
            saida.append(")");
        }
        else {
            saida.append(ctx.getText());
        }
        
        return null;
    }

    @Override
    public Void visitParcela_nao_unario(Parcela_nao_unarioContext ctx) {//parcela nao unaria é só o valor do campo
        // TODO Auto-generated method stub
        saida.append(ctx.getText());
        return null;
    }

    @Override
    public Void visitCmdCaso(CmdCasoContext ctx) {//switch case, tratando intervalos, com visita a expressao aritmetica
        // TODO Auto-generated method stub
        saida.append("switch(");
        visit(ctx.exp_aritmetica());
        saida.append("){\n");
        visit(ctx.selecao());
        if(ctx.cmdSenao() != null){
            visit(ctx.cmdSenao());
        }
        saida.append("}\n");
        return null;
    }
    @Override
    public Void visitSelecao(SelecaoContext ctx) {//visita todas os itens da selecao
        // TODO Auto-generated method stub
        ctx.item_selecao().forEach(var -> visitItem_selecao(var));
        return null;
    }
    @Override
    public Void visitItem_selecao(Item_selecaoContext ctx) {// cadda item deve ser tratado para caso seja um intervalo imprima todos os cases do mesmo
        // TODO Auto-generated method stub
        ArrayList<String> intervalo = new ArrayList<>(Arrays.asList(ctx.constantes().getText().split("\\.\\.")));
        String first = intervalo.size() > 0 ? intervalo.get(0) : ctx.constantes().getText();
        String last = intervalo.size() > 1 ? intervalo.get(1) : intervalo.get(0);
        for(int i = Integer.parseInt(first); i <= Integer.parseInt(last); i++){
            saida.append("case " + i + ":\n");
            ctx.cmd().forEach(var -> visitCmd(var));
            saida.append("break;\n");
        }
        return null;
    }
    @Override
    public Void visitCmdSenao(CmdSenaoContext ctx) {//o senao é traduzido como default em c para o cmdcaso
        // TODO Auto-generated method stub
        saida.append("default:\n");
        ctx.cmd().forEach(var -> visitCmd(var));
        saida.append("break;\n");
        return null;
    }

    @Override
    public Void visitCmdPara(CmdParaContext ctx) {//criando loop for, ate o valor passado depois do literall ate
        // TODO Auto-generated method stub
        String id = ctx.IDENT().getText();
        saida.append("for(" + id + " = ");
        visitExp_aritmetica(ctx.exp_aritmetica(0));
        saida.append("; " + id + " <= ");
        visitExp_aritmetica(ctx.exp_aritmetica(1));
        saida.append("; " + id + "++){\n");
        ctx.cmd().forEach(var -> visitCmd(var));
        saida.append("}\n");
        return null;
    }

    @Override
    public Void visitCmdEnquanto(CmdEnquantoContext ctx) {//cmd enquando loop while em c
        // TODO Auto-generated method stub
        saida.append("while(");
        visitExpressao(ctx.expressao());
        saida.append("){\n");
        ctx.cmd().forEach(var -> visitCmd(var));
        saida.append("}\n");
        return null;
    }

    @Override
    public Void visitCmdFaca(CmdFacaContext ctx) {//comando faca loop do while em c
        // TODO Auto-generated method stub
        saida.append("do{\n");
        ctx.cmd().forEach(var -> visitCmd(var));
        saida.append("} while(");
        visitExpressao(ctx.expressao());
        saida.append(");\n");
        return null;
    }


}
