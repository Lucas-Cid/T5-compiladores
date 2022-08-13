package compiladores.t4;

import compiladores.t4.AlgumaParser.CmdAtribuicaoContext;
import compiladores.t4.AlgumaParser.CmdContext;
import compiladores.t4.AlgumaParser.CmdEscrevaContext;
import compiladores.t4.AlgumaParser.CmdLeiaContext;
import compiladores.t4.AlgumaParser.CmdSeContext;
import compiladores.t4.AlgumaParser.CorpoContext;
import compiladores.t4.AlgumaParser.Declaracao_localContext;
import compiladores.t4.AlgumaParser.Declaracao_variavelContext;
import compiladores.t4.AlgumaParser.Exp_aritmeticaContext;
import compiladores.t4.AlgumaParser.Exp_relacionalContext;
import compiladores.t4.AlgumaParser.ExpressaoContext;
import compiladores.t4.AlgumaParser.FatorContext;
import compiladores.t4.AlgumaParser.Fator_logicoContext;
import compiladores.t4.AlgumaParser.ParcelaContext;
import compiladores.t4.AlgumaParser.Parcela_logicaContext;
import compiladores.t4.AlgumaParser.Parcela_unarioContext;
import compiladores.t4.AlgumaParser.TermoContext;
import compiladores.t4.AlgumaParser.Termo_logicoContext;

public class AlgumaGeradorC extends AlgumaBaseVisitor<Void> {
    StringBuilder saida;
    Table tabela;

    public AlgumaGeradorC() {
        saida = new StringBuilder();
        this.tabela = new Table();
    }

    @Override
    public Void visitPrograma(AlgumaParser.ProgramaContext ctx) {
        saida.append("#include <stdio.h>\n");
        saida.append("#include <stdlib.h>\n");
        saida.append("\n");
        // ctx.declaracao().forEach(dec -> visitDeclaracao(dec));
        saida.append("\n");
        saida.append("int main() {\n");

        visitCorpo(ctx.corpo());
        saida.append("return 0;\n");
        saida.append("}\n");
        return null;
    }

    @Override
    public Void visitCorpo(CorpoContext ctx) {
        for(AlgumaParser.Declaracao_localContext dec : ctx.declaracao_local()) {
            visitDeclaracao_local(dec);
        }

        for(AlgumaParser.CmdContext com : ctx.cmd()) {
            visitCmd(com);
        }

        return null;
    }

    @Override
    public Void visitDeclaracao_local(Declaracao_localContext ctx) {
        if(ctx.declaracao_variavel() != null){
            visitDeclaracao_variavel(ctx.declaracao_variavel());
        }
        // else if(ctx.declaracao_constante() != null){
        //     visitDeclaracao_constante(ctx.declaracao_constante());
        // } else if(ctx.declaracao_tipo() != null){
        //     visitDeclaracao_tipo(ctx.declaracao_tipo());
        // }

        return null;
    }

    @Override
    public Void visitDeclaracao_variavel(Declaracao_variavelContext ctx) {
        AlgumaParser.VariavelContext var = ctx.variavel();
        String cTipo = SemanticoUtils.getCType(var.tipo().getText());
        Table.Tipos tipo = SemanticoUtils.getTipo(var.tipo().getText());

        for(AlgumaParser.IdentificadorContext id: var.identificador()) {
            tabela.insert(id.getText(), tipo, Table.Structure.VAR);
            saida.append(cTipo + " ");
            saida.append(id.getText());
            if(cTipo == "char"){
                saida.append("[80]");
            }
            saida.append(";\n");
        }

        return null;
    }

    @Override
    public Void visitCmd(CmdContext ctx) {
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
        return null;
    }

    @Override
    public Void visitCmdLeia(CmdLeiaContext ctx) {
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
                saida.append(id.getText());
                saida.append(");\n");
            }
        }
        
        return null;
    }

    @Override
    public Void visitCmdEscreva(CmdEscrevaContext ctx) {
        for(AlgumaParser.ExpressaoContext exp: ctx.expressao()) {
            if(exp.IDENT() != null){
                saida.append("printf(\"%");
                saida.append(SemanticoUtils.getCTypeSymbol(tabela.verify(exp.IDENT().getText())));
                saida.append("\", ");
                saida.append(exp.IDENT().getText());
                saida.append(");\n");
            } else if(exp.CADEIA() != null){
                saida.append("printf(");
                saida.append(exp.CADEIA().getText());
                saida.append(");\n");
            } else {
                Escopo escopo = new Escopo(tabela);
                String cType = SemanticoUtils.getCTypeSymbol(SemanticoUtils.verificarTipo(escopo, exp));
                saida.append("printf(\"%");
                saida.append(cType);
                saida.append("\", ");
                saida.append(exp.getText());
                saida.append(");\n");
            }
        }
        return null;
    }

    @Override
    public Void visitCmdAtribuicao(CmdAtribuicaoContext ctx) {
        saida.append(ctx.identificador().getText());
        saida.append(" = ");
        saida.append(ctx.expressao().getText());
        saida.append(";\n");
        return null;
    }

    @Override
    public Void visitCmdSe(CmdSeContext ctx) {
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
    public Void visitExpressao(ExpressaoContext ctx) {
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
    public Void visitTermo_logico(Termo_logicoContext ctx) {
        visitFator_logico(ctx.fator_logico(0));

        for(int i = 1; i < ctx.fator_logico().size(); i++){
            AlgumaParser.Fator_logicoContext fator = ctx.fator_logico(i);
            saida.append(" && ");
            visitFator_logico(fator);
        }
        
        return null;
    }

    @Override
    public Void visitFator_logico(Fator_logicoContext ctx) {
        if(ctx.getText().startsWith("nao")){
            saida.append("!");
        }
        visitParcela_logica(ctx.parcela_logica());
        
        return null;
    }

    @Override
    public Void visitParcela_logica(Parcela_logicaContext ctx) {
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
    public Void visitExp_aritmetica(Exp_aritmeticaContext ctx) {
        visitTermo(ctx.termo(0));

        for(int i = 1; i < ctx.termo().size(); i++){
            AlgumaParser.TermoContext termo = ctx.termo(i);
            saida.append(ctx.op1(i-1).getText());
            visitTermo(termo);
        }
        return null;
    }

    @Override
    public Void visitTermo(TermoContext ctx) {
       visitFator(ctx.fator(0));

        for(int i = 1; i < ctx.fator().size(); i++){
            AlgumaParser.FatorContext fator = ctx.fator(i);
            saida.append(ctx.op2(i-1).getText());
            visitFator(fator);
        }
        return null;
    }

    @Override
    public Void visitFator(FatorContext ctx) {
        visitParcela(ctx.parcela(0));

        for(int i = 1; i < ctx.parcela().size(); i++){
            AlgumaParser.ParcelaContext parcela = ctx.parcela(i);
            saida.append(ctx.op3(i-1).getText());
            visitParcela(parcela);
        }
        return null;
    }

    @Override
    public Void visitParcela(ParcelaContext ctx) {
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
            saida.append(ctx.parentesis_expressao().expressao().getText());
            saida.append(")");
        }
        else {
            saida.append(ctx.getText());
        }
        
        return null;
    }
}
